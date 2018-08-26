package com.phonetwork.graphjs;

import android.content.Context
import android.util.Log
import com.android.volley.*
import com.android.volley.toolbox.HttpHeaderParser

import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.beust.klaxon.Converter
import com.beust.klaxon.JsonValue
import com.beust.klaxon.Klaxon
import com.beust.klaxon.KlaxonException

import org.json.JSONObject
import java.net.CookieHandler
import java.net.CookieManager

import java.net.URI
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class GraphJsApiManager
    @JvmOverloads constructor(
            context: Context,
            url: String = "http://phonetworks.com:1338/",
            id: String = "16D58CF2-FD88-4A49-972B-6F60054BF023",
            timeoutMillis: Int = TimeUnit.SECONDS.toMillis(20).toInt()) {

    private val logcatTag = "GJSAPI"
    private val expiresDateFormat = SimpleDateFormat("EEEE, dd-MMM-yy HH:mm:ss zzz", Locale.US)
    private val birthdayDateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
    private val credentialsExpiredTimeout : Int = 10

    private val requestQueue: RequestQueue by lazy {
        Volley.newRequestQueue(context.applicationContext)
    }

    private val cookieManager: CookieHandler by lazy {
        CookieHandler.getDefault() ?: CookieManager()
    }

    private val baseUri: URI by lazy {
        // make sure that URI ends with '/'
        if (url.endsWith('/')) URI.create(url) else URI.create("$url/")
    }

    private val context: Context = context
    private val requestTimeoutMillis: Int = timeoutMillis
    private val publicId: String = id

    private var currentUserId: String? = null

    init {
        VolleyLog.DEBUG = true // https://stackoverflow.com/a/39774490/902217
        VolleyLog.setTag("Volley");
        Log.isLoggable("Volley", Log.VERBOSE);

        CookieManager.setDefault(cookieManager)

        requestQueue.start()
    }

    fun signup(username: String, email: String, password: String,
               callback: (GraphJsRegisterCallResult) -> Unit) {
        val params = mapOf("username" to username, "email" to email, "password" to password)

        addGetJsonRequest("signup", params) { response ->
            val success: Boolean = response.getBoolean("success")
            val reason: String? = response.optString("reason", null)
            val id: String? = response.optString("id", null)

            callback(GraphJsRegisterCallResult(success, reason, id))
        }
    }

    //region User API

    fun login(username: String, password: String, callback: (GraphJsLoginCallResult) -> Unit) {
        val params = mapOf("username" to username, "password" to password)

        addGetJsonRequest("login", params) { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)
            val id: String? = response.optString("id", null)

            if (success) {
                val key = publicId.replace("-", "")

                val cal = Calendar.getInstance()
                cal.time = Date()
                cal.add(Calendar.MINUTE, credentialsExpiredTimeout)
                val expires = expiresDateFormat.format(cal.time)

                cookieManager.put(baseUri, mapOf("Set-Cookie" to listOf(
                        "graphjs_${key}_id=$id; path=/; expires=$expires;", //
                        "graphjs_${key}_session_off=; expires=Thu, 01 Jan 1970 00:00:01 GMT;"
                )))

                currentUserId = id
            }

            callback(GraphJsLoginCallResult(success, reason, id))
        }
    }

    fun whoami(callback: (GraphJsLoginCallResult) -> Unit) {
        addGetJsonRequest("whoami") { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)
            val id: String? = response.optString("id", null)

            callback(GraphJsLoginCallResult(success, reason, id))
        }
    }

    fun logout(callback: (GraphJsCallResult) -> Unit) {
        addGetJsonRequest("logout") { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)

            if (success) {
                val key = publicId.replace("-", "")
                cookieManager.put(baseUri, mapOf("Set-Cookie" to listOf(
                        "graphjs_${key}_id=; expires=Thu, 01 Jan 1970 00:00:01 GMT;",
                        "graphjs_${key}_session_off=true;"
                )))
                currentUserId = null
            }

            callback(GraphJsCallResult(success, reason))
        }
    }

    fun resetPassword(email: String, callback: (GraphJsCallResult) -> Unit) {
        val params = mapOf("email" to email)

        addGetJsonRequest("resetPassword", params) { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)

            callback(GraphJsCallResult(success, reason))
        }
    }

    fun verifyPasswordReset(email: String, code: String, callback: (GraphJsCallResult) -> Unit) {
        val params = mapOf("email" to email, "code" to code)

        addGetJsonRequest("resetPassword", params) { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)

            callback(GraphJsCallResult(success, reason))
        }
    }

    fun getProfile(userId: String? = null, callback: (GraphJsProfileCallResult) -> Unit) {
        val params = mapOf("id" to (userId ?: currentUserId))

        addGetJsonRequest("getProfile", params) { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)
            val profileJson: String? = response.optJSONObject("profile")?.toString()

            val profile : GraphJsUserProfile? = profileJson?.let {
                try {
                    return@let Klaxon()
                            .converter(birthdayDateConverter)
                            .converter(uriConverter)
                            .parse<GraphJsUserProfile>(it)
                } catch (e: KlaxonException) {
                    Log.d(logcatTag, "Cannot parse profile", e)
                }

                return@let null
            }

            callback(GraphJsProfileCallResult(success, reason, profile))
        }
    }

    @JvmOverloads
    fun setProfile(email: String? = null,
                   about: String? = null,
                   avatar: URI? = null,
                   birthday: Date? = null,
                   username: String? = null,
                   password: String? = null, callback: (GraphJsCallResult) -> Unit) {

        val params = mutableMapOf("email" to email, "about" to about, "avatar" to avatar?.toString(),
                "username" to username, "password" to password)

        birthday?.let {
            params["birthday"] = birthdayDateFormat.format(birthday)
        }

        addGetJsonRequest("setProfile", params) { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)

            callback(GraphJsCallResult(success, reason))
        }
    }

    fun countUnreadMessages(callback: (GraphJsCallCountResult) -> Unit) {
        addGetJsonRequest("countUnreadMessages") { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)
            val count: Int = response.optInt("count", 0)

            callback(GraphJsCallCountResult(success, reason, count))
        }
    }

    fun generateFeedToken(type: GraphJsFeedType, userId: String? = null, callback: (GraphJsCallFeedTokenResult) -> Unit) {
        val params = mapOf("type" to type.value, "id" to (userId ?: currentUserId))

        addGetJsonRequest("generateFeedToken", params) { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)
            val token: String? = response.optString("token", null)

            callback(GraphJsCallFeedTokenResult(success, reason, token))
        }
    }

    //region Thread API

    fun startThread(title: String, message: String, callback: (GraphJsThreadMessageCreateResult) -> Unit) {
        val params = mapOf("title" to title, "message" to message)

        addGetJsonRequest("startThread", params) { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)
            val id: String? = response.optString("id", null)

            callback(GraphJsThreadMessageCreateResult(success, reason, id))
        }
    }

    fun replyThread(threadId: String, message: String, callback: (GraphJsThreadMessageCreateResult) -> Unit) {
        val params = mapOf("id" to threadId, "message" to message)

        addGetJsonRequest("reply", params) { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)
            val id: String? = response.optString("id", null)

            callback(GraphJsThreadMessageCreateResult(success, reason, id))
        }
    }

    fun getThread(threadId: String, callback: (GraphJsThreadCallResult) -> Unit) {
        val params = mapOf("id" to threadId)

        addGetJsonRequest("getThread", params) { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)
            val title: String? = response.optString("title", null)
            val messagesJson: String? = response.optJSONArray("messages")?.toString()

            val messages = messagesJson?.let {
                try {
                    return@let Klaxon()
                            .converter(birthdayDateConverter)
                            .converter(uriConverter)
                            .parse<List<GraphJsThreadMessage>>(it)
                } catch (e: KlaxonException) {
                    Log.d(logcatTag, "Cannot parse thread messages", e)
                }

                return@let null
            }

            callback(GraphJsThreadCallResult(success, reason, title, messages))
        }
    }

    fun getThreads(callback: (GraphJsThreadsCallResult) -> Unit) {
        addGetJsonRequest("getThreads") { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)
            val threadsJson: String? = response.optJSONArray("threads")?.toString()

            val threads = threadsJson?.let {
                try {
                    return@let Klaxon().parse<List<GraphJsThread>>(it)
                } catch (e: KlaxonException) {
                    Log.d(logcatTag, "Cannot parse thread", e)
                }

                return@let null
            }

            callback(GraphJsThreadsCallResult(success, reason, threads))
        }
    }

    fun deleteForumPost(threadId: String, callback: (GraphJsCallResult) -> Unit) {
        val params = mapOf("id" to threadId)

        addGetJsonRequest("deleteForumPost", params) { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)

            TODO("not implemented")
        }
    }

    fun editForumPost(threadId: String, content: String, callback: (GraphJsCallResult) -> Unit) {
        val params = mapOf("id" to threadId, "content" to content)

        addGetJsonRequest("editForumPost", params) { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)

            TODO("not implemented")
        }
    }

    //region Members API

    fun members(callback: (GraphJsCallResult) -> Unit) {

        addGetJsonRequest("editForumPost") { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)

            TODO("not implemented")
        }
    }

    //region Direct Messaging API

    /**
     * Helper method to fire GET request & do basic validation
     */
    private fun addGetJsonRequest(apiMethod: String,
                                  params: Map<String, String?> = emptyMap(),
                                  callback: (JSONObject) -> Unit) {

        params["email"]?.let { email ->
            if (!shallowEmailValidation(email)) {
                callback(createErrorJson("Valid email required."))
                return
            }
        }

        val actualParams = params.toMutableMap()
        actualParams["public_id"] = publicId

        val actualUri = updateUri(baseUri, actualParams, apiMethod)

        Log.d(logcatTag, "-> HTTP GET $actualUri")

        val request = JsonObjectRequest(Request.Method.GET, actualUri.toString(), null,
                Response.Listener<JSONObject> { response ->
                    Log.d(logcatTag, "<- HTTP GET $response")

                    if (!response.has("success")) {
                        response.put("success", false)
                    }

                    callback(response)
                },
                Response.ErrorListener { error ->
                    var reason: String? = null

                    val response = error.networkResponse

                    response.let {
                        val charset = HttpHeaderParser.parseCharset(response.headers, "UTF-8")
                        reason = response.data?.toString(Charset.forName(charset))
                    }

                    if (reason == null) {
                        reason = error.message
                    }

                    if (reason == null) {
                        reason = "VolleyError=$error"
                    }

                    Log.d(logcatTag, "<- HTTP ${response.statusCode} $reason")

                    callback(createErrorJson(reason))
                })

        request.retryPolicy = DefaultRetryPolicy(
                requestTimeoutMillis,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)

        requestQueue.add(request)
    }

    private fun createErrorJson(reason: String?): JSONObject {
        return JSONObject(mapOf("success" to false, "reason" to reason))
    }

    /**
     * Create updated URI which will have:
     * 1. new query based on existing baseUri.query + queryParams
     * 2. resolve path on existing url
     *
     * @param baseUri which will be used as base to create a new one
     * @param queryParams to be added to result URI
     * @param appendPath to append to baseUri.path
     *
     * @return created URI
     */
    private fun updateUri(baseUri: URI, queryParams: Map<String, String?>, appendPath: String): URI {
        val queryBuilder = StringBuilder(baseUri.query ?: "")

        for ((name, value) in queryParams) {
            value?.let {
                // No need URL-encode because volley will do this for us
                queryBuilder.append('&')
                        .append(name)
                        .append('=')
                        .append(value)
            }
        }

        val actualQuery = queryBuilder.removePrefix("&").toString()

        return URI(baseUri.scheme, baseUri.userInfo, baseUri.host, baseUri.port,
                baseUri.path + appendPath,
                actualQuery, baseUri.fragment)

    }

    /**
     * Actual email validation regex is too complex, proofs:
     *  - https://tools.ietf.org/html/rfc2822#section-3.4.1
     *  - https://stackoverflow.com/a/1819170/902217
     */
    private fun shallowEmailValidation(email: String): Boolean {
        return email.indexOf('@') != -1
    }

    private val birthdayDateConverter = object: Converter {
        override fun canConvert(cls: Class<*>)
                = cls == Date::class.java

        override fun fromJson(jv: JsonValue) =
                if (jv.string != null) {
                    birthdayDateFormat.parse(jv.string)
                } else {
                    throw KlaxonException("Couldn't parse birthday: ${jv.string}")
                }

        override fun toJson(value: Any)
                = """ { "date" : $value } """
    }

    private val uriConverter = object: Converter {
        override fun canConvert(cls: Class<*>)
                = cls == URI::class.java

        override fun fromJson(jv: JsonValue) =
                if (jv.string != null) {
                    URI.create(jv.string)
                } else {
                    throw KlaxonException("Couldn't parse uri: ${jv.string}")
                }

        override fun toJson(value: Any)
                = """ { "date" : $value } """
    }
}