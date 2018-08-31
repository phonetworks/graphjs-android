package com.phonetwork.graphjs;

import android.content.Context
import android.util.Log

import com.android.volley.*
import com.android.volley.toolbox.*

import com.beust.klaxon.*

import org.json.JSONArray
import org.json.JSONObject

import java.net.CookieHandler
import java.net.CookieManager

import java.net.URI
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class ApiManager
    @JvmOverloads constructor(
            context: Context,
            url: String = "https://phonetworks.com:1338/",
            private val publicId: String = "16D58CF2-FD88-4A49-972B-6F60054BF023",
            private val requestTimeoutMillis: Int = TimeUnit.SECONDS.toMillis(20).toInt(),
            private val debugLogs: Boolean = false) {

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

    private var currentUserId: String? = null

    init {
        if (debugLogs) {
            // https://stackoverflow.com/a/39774490/902217
            VolleyLog.DEBUG = true
            VolleyLog.setTag("Volley")

            Log.isLoggable("Volley", Log.VERBOSE)
        }

        CookieManager.setDefault(cookieManager)

        requestQueue.start()
    }

    //region User API

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

    //region Profile API

    @JvmOverloads
    fun profile(userId: String? = null, callback: (GraphJsProfileCallResult) -> Unit) {
        val params = mapOf("id" to (userId ?: currentUserId))

        addGetJsonRequest("getProfile", params) { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)
            val profile = parseSafe<UserProfile>(response.optJSONObject("profile"))

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

    @JvmOverloads
    fun generateFeedToken(type: FeedType, userId: String? = null, callback: (GraphJsCallFeedTokenResult) -> Unit) {
        val params = mapOf("type" to type.value, "id" to (userId ?: currentUserId))

        addGetJsonRequest("generateFeedToken", params) { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)
            val token: String? = response.optString("token", null)

            callback(GraphJsCallFeedTokenResult(success, reason, token))
        }
    }

    //region Thread API

    fun startThread(title: String, message: String, callback: (GraphJsCreateCallResult) -> Unit) {
        val params = mapOf("title" to title, "message" to message)

        addGetJsonRequest("startThread", params) { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)
            val id: String? = response.optString("id", null)

            callback(GraphJsCreateCallResult(success, reason, id))
        }
    }

    fun replyThread(threadId: String, message: String, callback: (GraphJsCreateCallResult) -> Unit) {
        val params = mapOf("id" to threadId, "message" to message)

        addGetJsonRequest("reply", params) { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)
            val id: String? = response.optString("id", null)

            callback(GraphJsCreateCallResult(success, reason, id))
        }
    }

    fun getThread(threadId: String, callback: (GraphJsThreadCallResult) -> Unit) {
        val params = mapOf("id" to threadId)

        addGetJsonRequest("getThread", params) { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)
            val title: String? = response.optString("title", null)
            val messages = parseArraySafe<ThreadMessage>(response.optJSONArray("messages"))

            callback(GraphJsThreadCallResult(success, reason, title, messages))
        }
    }

    fun threads(callback: (GraphJsThreadsCallResult) -> Unit) {
        addGetJsonRequest("getThreads") { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)
            val threads = parseArraySafe<ForumThread>(response.optJSONArray("threads"))

            callback(GraphJsThreadsCallResult(success, reason, threads))
        }
    }

    fun deleteForumPost(postId: String, callback: (GraphJsCallResult) -> Unit) {
        val params = mapOf("id" to postId)

        addGetJsonRequest("deleteForumPost", params) { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)

            callback(GraphJsCallResult(success, reason))
        }
    }

    /**
     * NOTE: you cannot edit replies posts only threads
     */
    fun editForumPost(postId: String, content: String, callback: (GraphJsCallResult) -> Unit) {
        val params = mapOf("id" to postId, "content" to content)

        addGetJsonRequest("editForumPost", params) { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)

            callback(GraphJsCallResult(success, reason))
        }
    }

    //region Members API

    fun members(callback: (GraphJsMembersCallResult) -> Unit) {
        addGetJsonRequest("getMembers") { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)
            val members = parseMapSafe<Member>(response.optJSONObject("members"))

            callback(GraphJsMembersCallResult(success, reason, members))
        }
    }

    @JvmOverloads
    fun followers(userId: String? = null, callback: (GraphJsFollowersCallResult) -> Unit) {
        val params = mapOf("id" to (userId ?: currentUserId))

        addGetJsonRequest("getFollowers", params) { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)
            val followers = parseMapSafe<Member>(response.optJSONObject("followers"))

            callback(GraphJsFollowersCallResult(success, reason, followers))
        }
    }

    @JvmOverloads
    fun following(userId: String? = null, callback: (GraphJsFollowingCallResult) -> Unit) {
        val params = mapOf("id" to (userId ?: currentUserId))

        addGetJsonRequest("getFollowing", params) { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)
            val following = parseMapSafe<Member>(response.optJSONObject("following"))

            callback(GraphJsFollowingCallResult(success, reason, following))
        }
    }

    fun follow(followeeId: String, callback: (GraphJsCallResult) -> Unit) {
        val params = mapOf("id" to followeeId)

        addGetJsonRequest("follow", params) { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)

            callback(GraphJsCallResult(success, reason))
        }
    }

    fun unfollow(followeeId: String, callback: (GraphJsCallResult) -> Unit) {
        val params = mapOf("id" to followeeId)

        addGetJsonRequest("unfollow", params) { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)

            callback(GraphJsCallResult(success, reason))
        }
    }

    //region Direct Messaging API

    fun sendDirectMessage(toUserId: String, message: String, callback: (GraphJsSendMessageResult) -> Unit) {
        val params = mapOf("to" to toUserId, "message" to message)

        addGetJsonRequest("sendMessage", params) { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)
            val messageId: String? = response.optString("id", null)

            callback(GraphJsSendMessageResult(success, reason, messageId))
        }
    }

    fun sendDirectAnonymousMessage(sender: String, toUserId: String, message: String,
                                   callback: (GraphJsSendMessageResult) -> Unit) {
        val params = mapOf("sender" to sender, "to" to toUserId, "message" to message)

        addGetJsonRequest("sendAnonymousMessage", params) { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)
            val messageId: String? = response.optString("id", null)

            callback(GraphJsSendMessageResult(success, reason, messageId))
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

    fun inbox(callback: (GraphJsDirectMessagesResult) -> Unit) {
        msgbox("getInbox", callback)
    }

    fun outbox(callback: (GraphJsDirectMessagesResult) -> Unit) {
        msgbox("getOutbox", callback)
    }

    private fun msgbox(method: String, callback: (GraphJsDirectMessagesResult) -> Unit) {
        addGetJsonRequest(method) { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)
            val messages = parseMapSafe<DirectMessage>(response.optJSONObject("messages"))

            callback(GraphJsDirectMessagesResult(success, reason, messages))
        }
    }

    fun getConversation(withUserId: String, callback: (GraphJsDirectMessagesResult) -> Unit) {
        val params = mapOf("with" to withUserId)

        addGetJsonRequest("getConversation", params) { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)
            val messages = parseMapSafe<DirectMessage>(response.optJSONObject("messages"))

            callback(GraphJsDirectMessagesResult(success, reason, messages))
        }
    }

    fun conversations(callback: (GraphJsDirectMessagesResult) -> Unit) {
        addGetJsonRequest("getConversations") { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)
            val messages = parseMapSafe<DirectMessage>(response.optJSONObject("messages"))

            callback(GraphJsDirectMessagesResult(success, reason, messages))
        }
    }

    fun getMessage(messageId: String, callback: (GraphJsDirectMessageResult) -> Unit) {
        val params = mapOf("msgid" to messageId)

        addGetJsonRequest("getMessage", params) { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)
            val message = parseSafe<DirectMessage>(response.optJSONObject("message"))

            callback(GraphJsDirectMessageResult(success, reason, message))
        }
    }

    //region Group managment API

    fun createGroup(title: String, description: String, callback: (GraphJsCreateCallResult) -> Unit) {
        val params = mapOf("title" to title, "description" to description)

        addGetJsonRequest("createGroup", params) { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)
            val id: String? = response.optString("id", null)

            callback(GraphJsCreateCallResult(success, reason, id))
        }
    }

    @JvmOverloads
    fun setGroup(groupId: String, title: String? = null, description: String? = null, cover: URI? = null,
                 callback: (GraphJsCallResult) -> Unit) {
        val params = mapOf("id" to groupId, "title" to title, "description" to description, "cover" to cover?.toString())

        addGetJsonRequest("setGroup", params) { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)

            callback(GraphJsCallResult(success, reason))
        }
    }

    fun joinGroup(groupId: String, callback: (GraphJsCallResult) -> Unit) {
        val params = mapOf("id" to groupId)

        addGetJsonRequest("join", params) { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)

            callback(GraphJsCallResult(success, reason))
        }
    }

    fun leaveGroup(groupId: String, callback: (GraphJsCallResult) -> Unit) {
        val params = mapOf("id" to groupId)

        addGetJsonRequest("leave", params) { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)

            callback(GraphJsCallResult(success, reason))
        }
    }

    fun listMemberships(userId: String? = currentUserId, callback: (GraphJsGroupsResult) -> Unit) {
        val params = mapOf("id" to userId)

        addGetJsonRequest("listMemberships", params) { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)
            val groups = parseArraySafe<Group>(response.optJSONArray("groups"))

            callback(GraphJsGroupsResult(success, reason, groups))
        }
    }

    fun groups(callback: (GraphJsGroupsResult) -> Unit) {
        addGetJsonRequest("listGroups") { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)
            val groups = parseArraySafe<Group>(response.optJSONArray("groups"))

            callback(GraphJsGroupsResult(success, reason, groups))
        }
    }

    fun getGroup(groupId: String, callback: (GraphJsGroupResult) -> Unit) {
        val params = mapOf("id" to groupId)

        addGetJsonRequest("getGroup", params) { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)
            val groups = parseSafe<Group>(response.optJSONObject("group"))

            callback(GraphJsGroupResult(success, reason, groups))
        }
    }

    fun listMembers(groupId: String, callback: (GraphJsGroupMembersResult) -> Unit) {
        val params = mapOf("id" to groupId)

        addGetJsonRequest("listMembers", params) { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)
            val members = parseArraySafe<String>(response.optJSONArray("members"))

            callback(GraphJsGroupMembersResult(success, reason, members))
        }
    }

    //region Content Management API

    fun star(contentUrl: URI, callback: (GraphJsCallCountResult) -> Unit) {
        val params = mapOf("url" to contentUrl.toString())

        addGetJsonRequest("star", params) { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)
            val count: Int = response.optInt("count", 0)

            callback(GraphJsCallCountResult(success, reason, count))
        }
    }

    fun unstar(contentUrl: URI, callback: (GraphJsCallResult) -> Unit) {
        val params = mapOf("url" to contentUrl.toString())

        addGetJsonRequest("unstar", params) { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)

            callback(GraphJsCallResult(success, reason))
        }
    }

    fun isStarred(contentUrl: URI, callback: (GraphJsIsStarredResult) -> Unit) {
        val params = mapOf("url" to contentUrl.toString())

        addGetJsonRequest("isStarred", params) { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)
            val count: Int = response.optInt("count", 0)
            val starred: Boolean = response.optBoolean("starred", false)

            callback(GraphJsIsStarredResult(success, reason, count, starred))
        }
    }

    fun starredContent(callback: (GraphJsStarsStatResult) -> Unit) {
        addGetJsonRequest("getStarredContent") { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)
            val pages = parseMapSafe<StarsStatEntry>(response.optJSONObject("pages"))

            callback(GraphJsStarsStatResult(success, reason, pages))
        }
    }

    fun myStars(callback: (GraphJsStarsStatResult) -> Unit) {
        addGetJsonRequest("getMyStarredContent") { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)
            val pages = parseMapSafe<StarsStatEntry>(response.optJSONObject("pages"))

            callback(GraphJsStarsStatResult(success, reason, pages))
        }
    }

    fun addComment(contentUrl: URI, content: String, callback: (GraphJsCreateCallResult) -> Unit) {
        val params = mapOf("url" to contentUrl.toString(), "content" to content)

        addGetJsonRequest("addComment", params) { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)
            val id: String? = response.optString("comment_id", null)

            callback(GraphJsCreateCallResult(success, reason, id))
        }
    }

    fun editComment(commentId: String, content: String, callback: (GraphJsCallResult) -> Unit) {
        val params = mapOf("id" to commentId, "content" to content)

        addGetJsonRequest("editComment", params) { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)

            callback(GraphJsCallResult(success, reason))
        }
    }

    fun comments(contentUrl: URI, callback: (GraphJsCommentsResult) -> Unit) {
        val params = mapOf("url" to contentUrl.toString())

        addGetJsonRequest("getComments", params) { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)
            val commentEntries = response.optJSONArray("comments")

            val comments = mutableMapOf<String, ContentComment>()
            for (i in 0 until commentEntries.length() - 1) {
                val commentEntry = commentEntries.getJSONObject(i)

                for (key in commentEntry.keys()) {
                    val comment = commentEntry.getJSONObject(key)

                    parseSafe<ContentComment>(comment)?.let {
                        comments[key] = it
                    }
                }
            }

            callback(GraphJsCommentsResult(success, reason, comments))
        }
    }

    fun removeComment(commentId: String, callback: (GraphJsCallResult) -> Unit) {
        val params = mapOf("comment_id" to commentId)

        addGetJsonRequest("removeComment", params) { response ->
            val success: Boolean = response.optBoolean("success")
            val reason: String? = response.optString("reason", null)

            callback(GraphJsCallResult(success, reason))
        }
    }

    /**
     * Helper method to fire GET request & do basic validation
     * @param apiMethod method name of Server GraphJS API
     * @param params associative list of GET params
     * @param callback callback to notify about results
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

        log(message = "-> HTTP GET $actualUri")

        // TODO check possibility to use StringRequest to avoid double JSON deserialization
        val request = JsonObjectRequest(Request.Method.GET, actualUri.toString(), null,
                Response.Listener<JSONObject> { response ->
                    log(message = "<- HTTP GET $response")

                    if (!response.has("success")) {
                        response.put("success", false)
                    }

                    callback(response)
                },
                Response.ErrorListener { error ->
                    var reason: String? = null

                    val response = error.networkResponse

                    response?.let {
                        val charset = HttpHeaderParser.parseCharset(response.headers, "UTF-8")
                        reason = response.data?.toString(Charset.forName(charset))
                    }

                    if (reason == null) {
                        reason = error.message
                    }

                    if (reason == null) {
                        reason = "VolleyError=$error"
                    }

                    log(message = "<- HTTP ${response?.statusCode} $reason")

                    callback(createErrorJson(reason))
                })

        request.retryPolicy = DefaultRetryPolicy(
                requestTimeoutMillis,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)

        // TODO check if make sense to return request as some generic instance of Cancellable/Disposable
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

    /**
     * Helper function to parse map of objects
     * @param jsonMap
     */
    private inline fun <reified T> parseMapSafe(jsonMap: JSONObject?): Map<String, T> {
        val result = mutableMapOf<String, T>()

        if (jsonMap == null) {
            return result
        }

        try {
            for (key in jsonMap.keys()) {
                val messageJson = jsonMap.get(key).toString()

                Klaxon().converter(dateConverter)
                        .converter(uriConverter)
                        .parse<T>(messageJson)?.let { value -> result[key] = value }
            }
        } catch (e: Exception) {
            log(Log.WARN, "Cannot parse $jsonMap to ${T::class.java} because ${e.message}")
        }

        return result
    }

    /**
     * Helper function to parse list of objects
     * @param jsonMap
     */
    private inline fun <reified T> parseArraySafe(jsonArray: JSONArray?): List<T> {
        val result = mutableListOf<T>()

        if (jsonArray == null) {
            return result
        }

        try {
            Klaxon().converter(dateConverter)
                    .converter(uriConverter)
                    .parseArray<T>(jsonArray.toString())?.let {
                        result.addAll(it);
                    }

        } catch (e: Exception) {
            log(Log.WARN, "Cannot parse $jsonArray to ${T::class.java} because ${e.message}")
        }

        return result
    }

    /**
     * Helper function to parse single object
     * @param jsonMap
     */
    private inline fun <reified T> parseSafe(json: JSONObject?): T? {
        if (json == null) {
            return null;
        }

        try {
            return Klaxon().converter(dateConverter)
                    .converter(uriConverter)
                    .parse<T>(json.toString())
        } catch (e: Exception) {
            log(Log.WARN, "Cannot parse $json to ${T::class.java} because ${e.message}")
        }

        return null
    }

    private fun log(priority: Int = Log.DEBUG, message: String) {
        if (debugLogs) {
            Log.println(priority, logcatTag, message)
        }
    }

    private val dateConverter = object: Converter {
        override fun canConvert(cls: Class<*>)
                = cls == Date::class.java

        override fun fromJson(jv: JsonValue) =
                if (jv.string != null) {
                    try {
                        Date(jv.string!!.toLong())
                    } catch (e: NumberFormatException) {
                        birthdayDateFormat.parse(jv.string)
                    }
                } else if (jv.int != null) {
                    Date(jv.int!!.toLong())
                } else if (jv.longValue != null) {
                    Date(jv.longValue!!)
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
                = """ { "uri" : $value } """
    }
}