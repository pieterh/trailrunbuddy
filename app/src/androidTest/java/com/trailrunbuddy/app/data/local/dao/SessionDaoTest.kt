package com.trailrunbuddy.app.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trailrunbuddy.app.data.local.TrailRunBuddyDatabase
import com.trailrunbuddy.app.data.local.entity.SessionEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionDaoTest {

    private lateinit var db: TrailRunBuddyDatabase
    private lateinit var sessionDao: SessionDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TrailRunBuddyDatabase::class.java
        ).allowMainThreadQueries().build()
        sessionDao = db.sessionDao()
    }

    @After
    fun tearDown() = db.close()

    private fun session(profileId: Long = 1L) = SessionEntity(
        id = 1,
        profileId = profileId,
        state = "RUNNING",
        startedAt = 1_000L,
        pausedAt = null,
        totalPausedMs = 0L,
        timerStatesJson = "1:0:false"
    )

    @Test
    fun getReturnsNullWhenEmpty() = runBlocking {
        assertNull(sessionDao.get())
    }

    @Test
    fun saveAndRetrieveSession() = runBlocking {
        sessionDao.save(session())
        val result = sessionDao.get()
        assertNotNull(result)
        assertEquals(1L, result!!.profileId)
        assertEquals("RUNNING", result.state)
    }

    @Test
    fun saveReplacesPreviousSession() = runBlocking {
        sessionDao.save(session(profileId = 1L))
        sessionDao.save(session(profileId = 2L).copy(state = "PAUSED"))
        val result = sessionDao.get()
        assertEquals(2L, result!!.profileId)
        assertEquals("PAUSED", result.state)
    }

    @Test
    fun deleteRemovesSession() = runBlocking {
        sessionDao.save(session())
        sessionDao.delete()
        assertNull(sessionDao.get())
    }

    @Test
    fun savePreservesTimerStatesJson() = runBlocking {
        val json = "1:3:false;2:0:true"
        sessionDao.save(session().copy(timerStatesJson = json))
        assertEquals(json, sessionDao.get()!!.timerStatesJson)
    }

    @Test
    fun savePreservesPausedAtAndTotalPausedMs() = runBlocking {
        val paused = session().copy(pausedAt = 5_000L, totalPausedMs = 2_000L)
        sessionDao.save(paused)
        val result = sessionDao.get()!!
        assertEquals(5_000L, result.pausedAt)
        assertEquals(2_000L, result.totalPausedMs)
    }
}
