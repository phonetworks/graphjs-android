package com.phonetwork.graphjs

import com.beust.klaxon.Json
import java.net.URI
import java.util.*

data class GraphJsUserProfile(val username: String,
                              val email: String,
                              val joinTime: Date? = null,
                              val avatar: URI? = null,
                              val birthday: Date? = null,
                              val about: String? = null,
                              val followerCount: Int = 0,
                              val followingCount: Int = 0,
                              val membershipCount: Int = 0)

data class GraphJsThread(val id: String,
                         val title: String,
                         @Json("author") val authorId: String,
                         @Json("timestamp") val created: Date,
                         @Json("contributor") val contributorIds: List<String>)

data class GraphJsThreadMessage(
        val id: String,
        @Json("author") val authorId: String,
        val content: String,
        @Json("timestamp") val created: Date)

data class GraphJsCallResult(
        val success: Boolean = false,
        val reason: String? = null)

data class GraphJsRegisterCallResult(
        val success: Boolean = false,
        val reason: String? = null,
        val userId: String? = null)

data class GraphJsLoginCallResult(
        val success: Boolean = false,
        val reason: String? = null,
        val userId: String? = null)

data class GraphJsProfileCallResult(
        val success: Boolean = false,
        val reason: String? = null,
        val profile: GraphJsUserProfile? = null)

data class GraphJsThreadMessageCreateResult(
        val success: Boolean = false,
        val reason: String? = null,
        val threadMessageId: String? = null)

data class GraphJsThreadCallResult(
        val success: Boolean = false,
        val reason: String? = null,
        val title: String? = null,
        val messages: List<GraphJsThreadMessage>? = null)

data class GraphJsThreadsCallResult(
        val success: Boolean = false,
        val reason: String? = null,
        val threads: List<GraphJsThread>? = null)

data class GraphJsCallCountResult(
        val success: Boolean = false,
        val reason: String? = null,
        val count: Int = 0)

data class GraphJsCallFeedTokenResult(
        val success: Boolean = false,
        val reason: String? = null,
        val token: String? = null)

enum class GraphJsFeedType(val value: String) {
    Wall("wall"),
    Timeline("timeline")
}