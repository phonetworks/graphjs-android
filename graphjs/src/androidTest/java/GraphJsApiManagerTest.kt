package com.phonetwork.graphjs

import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import org.junit.After

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URI
import java.util.*

import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
class GraphJsApiManagerTest {
    // might be changing after server change
    companion object {
        const val testUserName = "johndoe"
        const val testUserPassword = "qwerty"
        const val testUserEmail = "jdoe@example.org"
        const val testUserId = "47bbd923bf6a523c8859d09426f90425"
    }
    
    private lateinit var context: Context

    private val subject: GraphJsApiManager by lazy { GraphJsApiManager(context,
            "http://10.0.3.2:1338/",
            "79982844-6a27-4b3b-b77f-419a79be0e10") }

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

    @Test
    fun getProfile() {
        val lock = CountDownLatch(1)
        var result = GraphJsProfileCallResult()

        subject.getProfile(testUserId) { r ->
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
    fun getProfileCurrentUser() {
        val lock = CountDownLatch(1)
        var result = GraphJsProfileCallResult()

        subject.getProfile { r ->
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
    fun resetGetProfileBadUserId() {
        val lock = CountDownLatch(1)
        var result = GraphJsProfileCallResult()

        subject.getProfile("00000000000000000000000000000") { r ->
            result = r
            lock.countDown()
        }

        lock.await()

        assert(!result.success)
        assertEquals("Invalid user ID", result.reason)
    }

    @Test
    fun changeProfileBio() {
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
    fun changeProfileBirthday() {
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
    fun changeProfileUsername() {
        val lock = CountDownLatch(1)
        var result = GraphJsCallResult()

        subject.setProfile(username = testUserName) { r ->
            result = r
            lock.countDown()
        }

        lock.await()

        assert(result.success)
        // It's ok that we get error here because we send the same testUserName as user already have
        // We not changing it to keep test as simple as possible
        assertEquals("Given field (Username) is not unique with the value ($testUserName)", result.reason)
    }

    @Test
    fun changeProfileEmail() {
        val lock = CountDownLatch(1)
        var result = GraphJsCallResult()

        subject.setProfile(email = testUserEmail) { r ->
            result = r
            lock.countDown()
        }

        lock.await()

        assert(result.success)
        assert(result.success)
        // It's ok that we get error here because we send the same testUserName as user already have
        // We not changing it to keep test as simple as possible
        assertEquals("Given field (Email) is not unique with the value ($testUserEmail)", result.reason)
    }

    @Test
    fun changeProfilePassword() {
        val lock = CountDownLatch(1)
        var result = GraphJsCallResult()

        subject.setProfile(password = testUserPassword) { r ->
            result = r
            lock.countDown()
        }

        lock.await()

        assert(result.success)
        assertNull(result.reason)
    }

    @Test
    fun changeProfileAvatar() {
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
    fun countUnreadMessages() {
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
    fun generateFeedTokenShallow() {
        val lock = CountDownLatch(1)
        var result = GraphJsCallFeedTokenResult()

        subject.generateFeedToken(GraphJsFeedType.Wall) { r ->
            result = r
            lock.countDown()
        }

        lock.await()

        assertNull(result)
    }

    @Test
    fun getThreads() {
        val lock = CountDownLatch(1)
        var result = GraphJsThreadsCallResult()

        subject.getThreads { r ->
            result = r
            lock.countDown()
        }

        lock.await()

        assertTrue(result.success)
        assertNotNull(result.threads)
    }

    fun getThreadCRUD() {
        var lock = CountDownLatch(1)
        var createResult = GraphJsThreadMessageCreateResult()

        subject.startThread("Unit Test Thread", "First!") { r ->
            createResult = r
            lock.countDown()
        }

        lock.await()

        assertTrue(createResult.success)
        assertNotNull(createResult.threadMessageId)

        lock = CountDownLatch(1)
        var replyResult = GraphJsThreadMessageCreateResult()

        subject.replyThread(createResult.threadMessageId!!, "Second!") { r ->
            replyResult = r
            lock.countDown()
        }

        assertTrue(replyResult.success)
        assertNotNull(replyResult.threadMessageId)

        lock = CountDownLatch(1)
        var editResult = GraphJsCallResult()
        subject.editForumPost(replyResult.threadMessageId!!, "Second!!") { r->
            editResult = r
            lock.countDown()
        }

        assertTrue(editResult.success)

        lock = CountDownLatch(1)
        var getResult = GraphJsThreadCallResult()
        subject.getThread(createResult.threadMessageId!!) { r ->
            getResult = r
            lock.countDown()
        }

        assertTrue(getResult.success)

        lock = CountDownLatch(1)
        var deleteResult = GraphJsCallResult()
        subject.deleteForumPost(replyResult.threadMessageId!!) { r ->
            deleteResult = r
            lock.countDown()
        }

        assertTrue(deleteResult.success)

        lock = CountDownLatch(1)
        subject.deleteForumPost(createResult.threadMessageId!!) { r ->
            deleteResult = r
            lock.countDown()
        }

        assertTrue(deleteResult.success)
    }
}