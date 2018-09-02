package org.phonetworks.graphjs

import com.beust.klaxon.Json

import java.net.URI
import java.util.*

data class UserProfile(val username: String,
                       val email: String,
                       @Json("jointime") val joinTime: Date? = null,
                       val avatar: URI? = null,
                       val birthday: Date? = null,
                       val about: String? = null,
                       @Json("follower_count") val followerCount: Int = 0,
                       @Json("following_count") val followingCount: Int = 0,
                       @Json("membership_count") val membershipCount: Int = 0)

data class ForumThread(val id: String,
                       val title: String,
                       @Json("author") val authorId: String,
                       @Json("timestamp") val created: Date,
                       val contributors: Map<String, UserProfile> = mapOf())

data class Member(val username: String,
                  val avatar: URI? = null)

data class ThreadMessage(
        val id: String,
        @Json("author") val authorId: String,
        val content: String,
        @Json("timestamp") val created: Date)

data class DirectMessage(
        val id: String,
        @Json("from") val fromUserId: String? = null,   // may be empty for outgoing
        @Json("to") val toUserId: String? = null,       // may be empty for incoming
        @Json("message") val content: String? = null,
        @Json("is_read") val isRead: Boolean = false,
        @Json("timestamp") val sentTime: Date? = null)

data class Group(
        val id: String,
        val title: String,
        val description: String,
        @Json("creator") val creatorId: String,
        val cover: URI? = null,
        @Json("count") val membersCounter: String,
        @Json("members") val memberIds: List<String> = listOf())

data class StarsStatEntry(
        val title: String,
        @Json("star_count") val stars: Int = 0)

data class ContentComment(
        val content: String,
        val createTime: Date,
        @Json("author") val authorId: String)

enum class FeedType(val value: String) {
    Wall("wall"),
    Timeline("timeline")
}