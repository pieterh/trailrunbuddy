package com.trailrunbuddy.app.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trailrunbuddy.app.data.local.TrailRunBuddyDatabase
import com.trailrunbuddy.app.data.local.entity.ProfileEntity
import com.trailrunbuddy.app.data.local.entity.TimerEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimerDaoTest {

    private lateinit var db: TrailRunBuddyDatabase
    private lateinit var profileDao: ProfileDao
    private lateinit var timerDao: TimerDao
    private var profileId: Long = 0L

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TrailRunBuddyDatabase::class.java
        ).allowMainThreadQueries().build()
        profileDao = db.profileDao()
        timerDao = db.timerDao()
        profileId = profileDao.insert(
            ProfileEntity(name = "Trail", colorHex = "#43A047", createdAt = 1_000L)
        )
    }

    @After
    fun tearDown() = db.close()

    private fun timer(name: String, order: Int = 0) = TimerEntity(
        profileId = profileId, name = name,
        durationSeconds = 600, timerType = "REPEATING", sortOrder = order
    )

    @Test
    fun insertAndBulkInsertTimers() = runBlocking {
        val ids = timerDao.insertAll(listOf(timer("Drink", 0), timer("Eat", 1)))
        assertEquals(2, ids.size)
    }

    @Test
    fun deleteByIdRemovesSingleTimer() = runBlocking {
        val id = timerDao.insert(timer("Drink"))
        timerDao.deleteById(id)
        val profile = profileDao.getById(profileId)
        assertEquals(0, profile!!.timers.size)
    }

    @Test
    fun deleteByProfileIdRemovesAllTimersForProfile() = runBlocking {
        timerDao.insertAll(listOf(timer("Drink"), timer("Eat")))
        timerDao.deleteByProfileId(profileId)
        val profile = profileDao.getById(profileId)
        assertEquals(0, profile!!.timers.size)
    }

    @Test
    fun updateChangesTimerFields() = runBlocking {
        val id = timerDao.insert(timer("Old"))
        val profile = profileDao.getById(profileId)!!
        val existing = profile.timers.first { it.id == id }
        timerDao.update(existing.copy(name = "New", durationSeconds = 900))
        val updated = profileDao.getById(profileId)!!.timers.first { it.id == id }
        assertEquals("New", updated.name)
        assertEquals(900, updated.durationSeconds)
    }
}
