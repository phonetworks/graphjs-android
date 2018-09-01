package com.phonetwork.graphjs

import com.beust.klaxon.Json

// TODO review to remove success from some Result classes to avoid double check success and data property

data class GraphJsCallResult(
        val success: Boolean = false,
        val reason: String? = null)

data class GraphJsRegisterResult(
        val success: Boolean = false,
        val reason: String? = null,
        val userId: String? = null)

data class GraphJsLoginResult(
        val success: Boolean = false,
        val reason: String? = null,
        val userId: String? = null)

data class GraphJsProfileResult(
        val success: Boolean = false,
        val reason: String? = null,
        val profile: UserProfile? = null)

data class GraphJsCreateResult(
        val success: Boolean = false,
        val reason: String? = null,
        val id: String? = null)

data class GraphJsThreadResult(
        val success: Boolean = false,
        val reason: String? = null,
        val title: String? = null,
        val messages: List<ThreadMessage> = listOf())

data class GraphJsThreadsResult(
        val success: Boolean = false,
        val reason: String? = null,
        val threads: List<ForumThread> = listOf())

data class GraphJsCountResult(
        val success: Boolean = false,
        val reason: String? = null,
        val count: Int = 0)

data class GraphJsFeedTokenResult(
        val success: Boolean = false,
        val reason: String? = null,
        val token: String? = null)

data class GraphJsMembersResult(
        val success: Boolean = false,
        val reason: String? = null,
        val members: Map<String, Member> = mapOf())

data class GraphJsDirectMessageResult(
        val success: Boolean = false,
        val reason: String? = null,
        val message: DirectMessage? = null)

data class GraphJsDirectMessagesResult(
        val success: Boolean = false,
        val reason: String? = null,
        val messages: Map<String, DirectMessage> = mapOf())

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
        @Json("members") val memberIds: List<String> = listOf())

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