package com.phonetwork.graphjs

import com.beust.klaxon.Json

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
        val profile: UserProfile? = null)

data class GraphJsCreateCallResult(
        val success: Boolean = false,
        val reason: String? = null,
        val id: String? = null)

data class GraphJsThreadCallResult(
        val success: Boolean = false,
        val reason: String? = null,
        val title: String? = null,
        val messages: List<ThreadMessage> = listOf())

data class GraphJsThreadsCallResult(
        val success: Boolean = false,
        val reason: String? = null,
        val threads: List<ForumThread> = listOf())

data class GraphJsCallCountResult(
        val success: Boolean = false,
        val reason: String? = null,
        val count: Int = 0)

data class GraphJsCallFeedTokenResult(
        val success: Boolean = false,
        val reason: String? = null,
        val token: String? = null)

data class GraphJsMembersCallResult(
        val success: Boolean = false,
        val reason: String? = null,
        val members: Map<String, Member> = mapOf())

data class GraphJsFollowingCallResult(
        val success: Boolean = false,
        val reason: String? = null,
        val following: Map<String, Member> = mapOf())

data class GraphJsFollowersCallResult(
        val success: Boolean = false,
        val reason: String? = null,
        val followers: Map<String, Member> = mapOf())

data class GraphJsSendMessageResult(
        val success: Boolean = false,
        val reason: String? = null,
        val messageId: String? = null)

data class GraphJsDirectMessageResult(
        val success: Boolean = false,
        val reason: String? = null,
        val message: DirectMessage? = null)

data class GraphJsDirectMessagesResult(
        val success: Boolean = false,
        val reason: String? = null,
        val messages: Map<String, DirectMessage>? = mapOf())

data class GraphJsGroupsResult(
        val success: Boolean = false,
        val reason: String? = null,
        val groups: List<Group> = listOf())

data class GraphJsGroupResult(
        val success: Boolean = false,
        val reason: String? = null,
        val group: Group? = null)

data class GraphJsGroupMembersResult(
        val success: Boolean = false,
        val reason: String? = null,
        @Json("members")val memberIds: List<String> = listOf())

data class GraphJsIsStarredResult(
        val success: Boolean = false,
        val reason: String? = null,
        val count: Int = 0,
        @Json("starred") val starredByMe: Boolean = false)

data class GraphJsStarsStatResult(
        val success: Boolean = false,
        val reason: String? = null,
        val pages: Map<String, StarsStatEntry> = mapOf())

data class GraphJsCommentsResult(
        val success: Boolean = false,
        val reason: String? = null,
        val comments: Map<String, ContentComment> = mapOf())

data class GraphJsContentResult(
        val success: Boolean = false,
        val reason: String? = null,
        val content: String? = null)