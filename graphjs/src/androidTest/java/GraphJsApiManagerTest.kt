package com.phonetwork.graphjs

import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import org.junit.After

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.CookieManager
import java.net.URI
import java.util.*

import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
class GraphJsApiManagerTest {
    // data can be generated with git@github.com:phonetworks/graphjs.git/scripts/data-gen
    companion object {
        const val testUserName = "johndoe"
        const val testUserPassword = "qwerty"
        const val testUserEmail = "jdoe@example.org"
        const val testUserId = "4276f9f759d87d7f91b5895a4ef5d6c1"

        const val alice = "alice"
        const val bob = "bob"
    }
    
    private lateinit var context: Context

    private val subject: GraphJsApiManager by lazy { GraphJsApiManager(context,
            "http://10.0.3.2:1338/",    // genymotion emulator host gateway
            "79982844-6a27-4b3b-b77f-419a79be0e10",
            debugLogs = true)
    }

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getTargetContext()

        val lock = CountDownLatch(1)
        var result = GraphJsLoginCallResult()

        subject.login(testUserName, testUserPassword) { r ->
            result = r
            lock.countDown()
        }

        lock.await()

        assert(result.success)
        assertEquals(testUserId, result.userId)
    }

    @After
    fun tearDown() {
        val lock = CountDownLatch(1)
        var result = GraphJsCallResult()

        subject.logout { r ->
            result = r
            lock.countDown()
        }

        lock.await()

        assert(result.success)
        assertNull(result.reason)
    }

    //region User API

    @Test
    fun signupInvalidEmail() {
        val lock = CountDownLatch(1)
        var result = GraphJsRegisterCallResult()

        subject.signup(testUserName, testUserEmail.replace('@', 'a'), testUserPassword) { r ->
            result = r
            lock.countDown()
        }

        lock.await()

        assert(!result.success)
        assertEquals("Valid email required.", result.reason)
    }

    @Test
    fun signupExistingUser() {
        val lock = CountDownLatch(1)
        var result = GraphJsRegisterCallResult()

        subject.signup(testUserName, testUserEmail, testUserPassword) { r ->
            result = r
            lock.countDown()
        }

        lock.await()

        assert(!result.success)
        assertEquals("Given field (Username) is not unique with the value (johndoe)", result.reason)
    }

    @Test
    fun whoami() {
        val lock = CountDownLatch(1)
        var result = GraphJsLoginCallResult()

        subject.whoami { r ->
            result = r
            lock.countDown()
        }

        lock.await()

        assert(result.success)
        assertEquals(testUserId, result.userId)
    }

    @Test
    fun loginUnregisteredUser() {
        val lock = CountDownLatch(1)
        var result = GraphJsLoginCallResult()

        subject.login("xxxxxx", "xxxxxx@example.org") { r ->
            result = r
            lock.countDown()
        }

        lock.await()

        assert(!result.success)
        assertEquals("Information don't match records", result.reason)
        assertNull("`id` must not be there", result.userId)
    }

    @Test
    fun resetPasswordSuccess() {
        val lock = CountDownLatch(1)
        var result = GraphJsCallResult()

        subject.resetPassword(testUserEmail) { r ->
            result = r
            lock.countDown()
        }

        lock.await()

        assert(result.success)
        assertNull(result.reason)
    }

    @Test
    fun verifyPasswordResetWrongCode() {
        val lock = CountDownLatch(1)
        var result = GraphJsCallResult()

        subject.verifyPasswordReset(testUserEmail, "------") { r ->
            result = r
            lock.countDown()
        }

        lock.await()

        assert(!result.success)
        assertNull(result.reason)
    }

    //region Profile API

    @Test
    fun profileHappy() {
        val lock = CountDownLatch(1)
        var result = GraphJsProfileCallResult()

        subject.profile(testUserId) { r ->
            result = r
            lock.countDown()
        }

        lock.await()

        assert(result.success)
        assertNull(result.reason)
        assertNotNull(result.profile)
        assertEquals(testUserEmail, result.profile?.email)
        assertEquals(testUserName, result.profile?.username)
        assertNotNull(result.profile?.avatar)
    }

    @Test
    fun profileCurrentUserHappy() {
        val lock = CountDownLatch(1)
        var result = GraphJsProfileCallResult()

        subject.profile { r ->
            result = r
            lock.countDown()
        }

        lock.await()

        assert(result.success)
        assertNull(result.reason)
        assertNotNull(result.profile)
        assertEquals(testUserEmail, result.profile?.email)
        assertEquals(testUserName, result.profile?.username)
        assertNotNull(result.profile?.avatar)
    }

    @Test
    fun profileWithBadUserId() {
        val lock = CountDownLatch(1)
        var result = GraphJsProfileCallResult()

        subject.profile("00000000000000000000000000000") { r ->
            result = r
            lock.countDown()
        }

        lock.await()

        assert(!result.success)
        assertEquals("Invalid user ID", result.reason)
    }

    @Test
    fun changeProfileBioHappy() {
        val lock = CountDownLatch(1)
        var result = GraphJsCallResult()

        subject.setProfile(about = "123145678901235567890") { r ->
            result = r
            lock.countDown()
        }

        lock.await()

        assert(result.success)
        assertNull(result.reason)
    }

    @Test
    fun changeProfileBirthdayHappy() {
        val lock = CountDownLatch(1)
        var result = GraphJsCallResult()

        val cal = Calendar.getInstance()
        cal.time = Date()
        cal.add(Calendar.YEAR, -14)

        subject.setProfile(birthday = cal.time) { r ->
            result = r
            lock.countDown()
        }

        lock.await()

        assert(result.success)
        assertNull(result.reason)
    }

    @Test
    fun changeProfileUsernameToSame() {
        val lock = CountDownLatch(1)
        var result = GraphJsCallResult()

        subject.setProfile(username = testUserName) { r ->
            result = r
            lock.countDown()
        }

        lock.await()

        assertFalse(result.success)
        // It's ok that we get error here because we send the same testUserName as user already have
        // We not changing it to keep test as simple as possible
        assertEquals("Given field (Username) is not unique with the value ($testUserName)", result.reason)
    }

    @Test
    fun changeProfileEmailToSame() {
        val lock = CountDownLatch(1)
        var result = GraphJsCallResult()

        subject.setProfile(email = testUserEmail) { r ->
            result = r
            lock.countDown()
        }

        lock.await()

        assertFalse(result.success)
        // It's ok that we get error here because we send the same testUserName as user already have
        // We not changing it to keep test as simple as possible
        assertEquals("Given field (Email) is not unique with the value ($testUserEmail)", result.reason)
    }

    @Test
    fun changeProfilePasswordHappy() {
        val lock = CountDownLatch(1)
        var result = GraphJsCallResult()

        subject.setProfile(password = testUserPassword) { r ->
            result = r
            lock.countDown()
        }

        lock.await()

        assertTrue(result.success)
        assertNull(result.reason)
    }

    @Test
    fun changeProfileAvatarHappy() {
        val lock = CountDownLatch(1)
        var result = GraphJsCallResult()

        subject.setProfile(avatar = URI.create("https://www.fnordware.com/superpng/pnggrad16rgb.png")) { r ->
            result = r
            lock.countDown()
        }

        lock.await()

        assert(result.success)
        assertNull(result.reason)
    }

    @Test
    fun changeProfileNoData() {
        val lock = CountDownLatch(1)
        var result = GraphJsCallResult()

        subject.setProfile { r ->
            result = r
            lock.countDown()
        }

        lock.await()

        assert(result.success)
        assertEquals("No field to set", result.reason)
    }

    @Test
    fun generateFeedTokenShallow() {
        val lock = CountDownLatch(1)
        var result = GraphJsCallFeedTokenResult()

        subject.generateFeedToken(FeedType.Wall) { r ->
            result = r
            lock.countDown()
        }

        lock.await()

        assertTrue(result.success)
        assertNotNull(result.token)
    }

    //region Thread API

    @Test
    fun threadsShallow() {
        val lock = CountDownLatch(1)
        var result = GraphJsThreadsCallResult()

        subject.threads { r ->
            result = r
            lock.countDown()
        }

        lock.await()

        assertTrue(result.success)
        assertNotNull(result.threads)
    }

    @Test
    fun threadCRUD() {
        var lock = CountDownLatch(1)
        var createResult = GraphJsCreateCallResult()

        subject.startThread("Unit Test Thread", "First!") { r ->
            createResult = r
            lock.countDown()
        }

        lock.await()

        assertTrue(createResult.success)
        assertNotNull(createResult.id)

        lock = CountDownLatch(1)
        var replyResult = GraphJsCreateCallResult()

        subject.replyThread(createResult.id!!, "Second!") { r ->
            replyResult = r
            lock.countDown()
        }

        lock.await()

        assertTrue(replyResult.success)
        assertNotNull(replyResult.id)

        lock = CountDownLatch(1)
        var editResult = GraphJsCallResult()
        subject.editForumPost(createResult.id!!, "First!!") { r->
            editResult = r
            lock.countDown()
        }

        lock.await()

        assertTrue(editResult.success)

        lock = CountDownLatch(1)
        var getResult = GraphJsThreadCallResult()
        subject.getThread(createResult.id!!) { r ->
            getResult = r
            lock.countDown()
        }

        lock.await()

        assertTrue(getResult.success)
        assertNotNull(getResult.messages)
        assertNotNull(getResult.messages.find { it.id == createResult.id })
        assertNotNull(getResult.messages.find { it.id == replyResult.id })

        lock = CountDownLatch(1)
        var deleteResult = GraphJsCallResult()
        subject.deleteForumPost(replyResult.id!!) { r ->
            deleteResult = r
            lock.countDown()
        }

        lock.await()

        assertTrue(deleteResult.success)

        lock = CountDownLatch(1)
        subject.deleteForumPost(createResult.id!!) { r ->
            deleteResult = r
            lock.countDown()
        }

        lock.await()

        assertTrue(deleteResult.success)
    }

    //region Members API

    @Test
    fun members() {
        val lock = CountDownLatch(1)
        var result = GraphJsMembersCallResult()

        subject.members { r ->
            result = r
            lock.countDown()
        }

        lock.await()

        assertTrue(result.success)
        assertNotNull(result.members)
    }

    @Test
    fun followers() {
        val lock = CountDownLatch(1)
        var result = GraphJsFollowersCallResult()

        subject.followers { r ->
            result = r
            lock.countDown()
        }

        lock.await()

        assertTrue(result.success)
        assertNotNull(result.followers)
    }

    @Test
    fun following() {
        val lock = CountDownLatch(1)
        var result = GraphJsFollowingCallResult()

        subject.following { r ->
            result = r
            lock.countDown()
        }

        lock.await()

        assertTrue(result.success)
        assertNotNull(result.following)
    }

    @Test
    fun cannotFollowYourself() {
        val lock = CountDownLatch(1)
        var result = GraphJsCallResult()

        subject.follow(testUserId) { r ->
            result = r
            lock.countDown()
        }

        lock.await()

        assertFalse(result.success)
        assertEquals("Follower and followee can't be the same", result.reason)
    }

    @Test
    fun cannotUnfollowYourself() {
        val lock = CountDownLatch(1)
        var result = GraphJsCallResult()

        subject.unfollow(testUserId) { r ->
            result = r
            lock.countDown()
        }

        lock.await()

        assertFalse(result.success)
        assertEquals("No follow edge found", result.reason)
    }

    //region Messaging API

    @Test
    fun talkingToMyself() {
        val lock = CountDownLatch(1)
        var result = GraphJsSendMessageResult()

        subject.sendDirectMessage(testUserId, "Hello to myself") { r ->
            result = r
            lock.countDown()
        }

        lock.await()

        assertFalse(result.success)
        assertEquals("Can't send a message to self", result.reason)
    }

    @Test
    fun anonymousMessageHappy() {
        val lock = CountDownLatch(1)
        var result = GraphJsSendMessageResult()

        val cookieManager = CookieManager.getDefault()
        CookieManager.setDefault(null)

        subject.sendDirectAnonymousMessage(alice, testUserId, "Hello from $alice") { r ->
            result = r
            lock.countDown()
        }

        lock.await()

        CookieManager.setDefault(cookieManager)

        assertTrue(result.success)
        assertNull(result.reason)
    }

    @Test
    fun anonymousMessageFail() {
        val lock = CountDownLatch(1)
        var result = GraphJsSendMessageResult()

        val cookieManager = CookieManager.getDefault()
        CookieManager.setDefault(null)

        subject.sendDirectAnonymousMessage(bob, "--------", "Hello from $bob") { r ->
            result = r
            lock.countDown()
        }

        lock.await()

        CookieManager.setDefault(cookieManager)

        assertFalse(result.success)
        assertEquals("Invalid recipient", result.reason)
    }

    @Test
    fun countUnreadMessagesHappy() {
        val lock = CountDownLatch(1)
        var result = GraphJsCallCountResult()

        subject.countUnreadMessages { r ->
            result = r
            lock.countDown()
        }

        lock.await()

        assert(result.success)
        assertNull(result.reason)
    }

    @Test
    fun inboxHappy() {
        val lock = CountDownLatch(1)
        var result = GraphJsDirectMessagesResult()

        subject.inbox { r ->
            result = r
            lock.countDown()
        }

        lock.await()

        assertTrue(result.success)
    }

    @Test
    fun outboxHappyAndGetMessage() {
        val lock = CountDownLatch(1)
        var result = GraphJsDirectMessagesResult()

        subject.outbox { r ->
            result = r
            lock.countDown()
        }

        lock.await()

        assertTrue(result.success)

        for ((messageId, message) in result.messages!!) {
            val msgLock = CountDownLatch(1)
            var msgResult = GraphJsDirectMessageResult()

            subject.getMessage(messageId) { r ->
                msgResult = r
                msgLock.countDown()
            }

            msgLock.await()

            assertTrue(msgResult.success)
            assertNotNull(msgResult.message)
            assertEquals(message.toUserId, msgResult.message!!.toUserId)
        }
    }

    @Test
    fun conversationsHappy() {
        val lock = CountDownLatch(1)
        var result = GraphJsDirectMessagesResult()

        subject.conversations { r ->
            result = r
            lock.countDown()
        }

        lock.await()

        assertTrue(result.success)
        assertNotNull(result.messages)

        for ((messageId, message) in result.messages!!) {
            val msgLock = CountDownLatch(1)
            var msgResult = GraphJsDirectMessagesResult()

            subject.getConversation(messageId) { r ->
                msgResult = r
                msgLock.countDown()
            }

            msgLock.await()

            assertTrue(msgResult.success)
            assertNotNull(msgResult.messages)
        }
    }

    //region Group managment API

    @Test
    fun groupCRUD() {
        var lock = CountDownLatch(1)
        var createResult = GraphJsCreateCallResult()

        subject.createGroup("MyGroup", "MyGroupDescription") { r ->
            createResult = r
            lock.countDown()
        }

        lock.await()

        assertTrue(createResult.success)
        assertNotNull(createResult.id)

        lock = CountDownLatch(1)
        var readResult = GraphJsGroupResult()

        subject.getGroup(createResult.id!!) { r ->
            readResult = r
            lock.countDown()
        }

        lock.await()

        assertTrue(readResult.success)
        assertEquals("MyGroup", readResult.group?.title)
        assertEquals("MyGroupDescription", readResult.group?.description)
        assertTrue(readResult.group?.memberIds?.contains(testUserId)!!)

        lock = CountDownLatch(1)
        var setResult = GraphJsCallResult()

        subject.setGroup(createResult.id!!, "MyGroupNew",
                "MyGroupDescriptionNew",
                URI.create("https://www.fnordware.com/superpng/pnggrad16rgb.png")) { r ->
            setResult = r
            lock.countDown()
        }

        lock.await()

        assertTrue(setResult.success)

        lock = CountDownLatch(1)
        var read2Result = GraphJsGroupsResult()

        subject.groups { r ->
            read2Result = r
            lock.countDown()
        }

        lock.await()

        assertTrue(read2Result.success)
        assertNotNull(read2Result.groups)
        assertNotNull(read2Result.groups.filter { it.id == createResult.id})

        lock = CountDownLatch(1)
        var read3Result = GraphJsGroupsResult()

        subject.listMemberships { r ->
            read3Result = r
            lock.countDown()
        }

        lock.await()

        assertTrue(read3Result.success)
        assertNotNull(read2Result.groups)

        val group = read2Result.groups.filter { it.id == createResult.id}.first()
        assertNotNull(group)
        assertEquals("MyGroupNew", group.title)
        assertEquals("MyGroupDescriptionNew", group.description)

        lock = CountDownLatch(1)
        var leaveResult = GraphJsCallResult()

        subject.leaveGroup(createResult.id!!) { r ->
            leaveResult = r
            lock.countDown()
        }

        lock.await()

        assertTrue(leaveResult.success)

        lock = CountDownLatch(1)
        var joinResult = GraphJsCallResult()

        subject.joinGroup(createResult.id!!) { r ->
            joinResult = r
            lock.countDown()
        }

        lock.await()

        assertTrue(joinResult.success)

        lock = CountDownLatch(1)
        var membersResult = GraphJsGroupMembersResult()

        subject.listMembers(createResult.id!!) { r ->
            membersResult = r
            lock.countDown()
        }

        lock.await()

        assertTrue(membersResult.success)
        assertTrue(membersResult.memberIds.contains(testUserId))
    }

    //region Content Management API

    @Test
    fun whoCaresIfOneMoreLightGoesOut() {
        val contentUrl = URI.create("https://www.youtube.com/watch?v=Tm8LGxTLtQk")
        var lock = CountDownLatch(1)

        var result = GraphJsCallCountResult()
        subject.star(contentUrl) { r ->
            result = r
            lock.countDown()
        }

        lock.await()

        assertTrue(result.success)
        assertTrue(result.count > 0)

        lock = CountDownLatch(1)
        var isStarredResult = GraphJsIsStarredResult()
        subject.isStarred(contentUrl) { r ->
            isStarredResult = r
            lock.countDown()
        }

        lock.await()

        assertTrue(isStarredResult.success)
        assertTrue(isStarredResult.starredByMe)
        assertEquals(result.count, isStarredResult.count)

        lock = CountDownLatch(1)
        var unstarResult = GraphJsCallResult()
        subject.unstar(contentUrl) { r ->
            unstarResult = r
            lock.countDown()
        }

        lock.await()

        assertTrue(unstarResult.success)
        lock = CountDownLatch(1)
        subject.isStarred(contentUrl) { r ->
            isStarredResult = r
            lock.countDown()
        }

        lock.await()

        assertTrue(isStarredResult.success)
        assertFalse(isStarredResult.starredByMe)
        assertEquals(result.count - 1, isStarredResult.count)
    }

    @Test
    fun allStars() {
        val contentUrl1 = URI.create("https://www.youtube.com/watch?v=vjF9GgrY9c0")
        val contentUrl2 = URI.create("https://www.youtube.com/watch?v=nKOPF6XtEZw")
        val contentUrl3 = URI.create("https://www.youtube.com/watch?v=3hJOCCXPwT8")

        var lock = CountDownLatch(1)
        subject.star(contentUrl1) { r ->
            lock.countDown()
        }
        lock.await()

        lock = CountDownLatch(1)
        subject.star(contentUrl2) { r ->
            lock.countDown()
        }
        lock.await()

        lock = CountDownLatch(1)
        subject.star(contentUrl3) { r ->
            lock.countDown()
        }
        lock.await()

        lock = CountDownLatch(1)
        var result = GraphJsStarsStatResult()
        subject.myStars { r ->
            result = r
            lock.countDown()
        }
        lock.await()

        assertTrue(result.success)
        assertTrue(result.pages.size >= 3)

        lock = CountDownLatch(1)
        subject.starredContent { r ->
            result = r
            lock.countDown()
        }
        lock.await()

        assertTrue(result.success)
        assertTrue(result.pages.size >= 3)
    }

    @Test
    fun commentCRUD() {
        val contentUrl = URI.create("https://www.youtube.com/watch?v=vjF9GgrY9c0")

        var lock = CountDownLatch(1)
        var createResult1 = GraphJsCreateCallResult()
        subject.addComment(contentUrl, "First!") { r ->
            createResult1 = r
            lock.countDown()
        }

        lock.await()

        assertTrue(createResult1.success)
        assertNotNull(createResult1.id)

        lock = CountDownLatch(1)
        var createResult2 = GraphJsCreateCallResult()
        subject.addComment(contentUrl, "Second!") { r ->
            createResult2 = r
            lock.countDown()
        }

        lock.await()

        assertTrue(createResult2.success)
        assertNotNull(createResult2.id)

        lock = CountDownLatch(1)
        var readResult = GraphJsCommentsResult()
        subject.comments(contentUrl) { r ->
            readResult = r
            lock.countDown()
        }

        lock.await()

        assertTrue(readResult.success)
        assertNotNull(readResult.comments)
        assertNotNull(readResult.comments[createResult1.id!!])
        assertNotNull(readResult.comments[createResult2.id!!])

        lock = CountDownLatch(1)
        var editResult = GraphJsCallResult()
        subject.editComment(createResult1.id!!, "First!!!") { r ->
            editResult = r
            lock.countDown()
        }

        lock.await()

        assertTrue(editResult.success)

        lock = CountDownLatch(1)
        var deleteResult1 = GraphJsCallResult()
        subject.removeComment(createResult1.id!!) { r ->
            deleteResult1 = r
            lock.countDown()
        }

        lock.await()

        assertTrue(deleteResult1.success)

        lock = CountDownLatch(1)
        var deleteResult2 = GraphJsCallResult()
        subject.removeComment(createResult2.id!!) { r ->
            deleteResult2 = r
            lock.countDown()
        }

        lock.await()

        assertTrue(deleteResult2.success)
    }
}