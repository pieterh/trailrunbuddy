package com.trailrunbuddy.app.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trailrunbuddy.app.data.local.TrailRunBuddyDatabase
import com.trailrunbuddy.app.data.local.entity.ProfileEntity
import com.trailrunbuddy.app.data.local.entity.TimerEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileDaoTest {

    private lateinit var db: TrailRunBuddyDatabase
    private lateinit var profileDao: ProfileDao
    private lateinit var timerDao: TimerDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TrailRunBuddyDatabase::class.java
        ).allowMainThreadQueries().build()
        profileDao = db.profileDao()
        timerDao = db.timerDao()
    }

    @After
    fun tearDown() = db.close()

    private fun profile(name: String = "Trail") =
        ProfileEntity(name = name, colorHex = "#43A047", createdAt = 1_000L)

    @Test
    fun insertAndRetrieveById() = runBlocking {
        val id = profileDao.insert(profile("Marathon"))
        val result = profileDao.getById(id)
        assertNotNull(result)
        assertEquals("Marathon", result!!.profile.name)
    }

    @Test
    fun observeAllEmitsInsertedProfiles() = runBlocking {
        profileDao.insert(profile("A"))
        profileDao.insert(profile("B"))
        val list = profileDao.observeAll().first()
        assertEquals(2, list.size)
    }

    @Test
    fun deleteRemovesProfile() = runBlocking {
        val id = profileDao.insert(profile())
        profileDao.deleteById(id)
        assertNull(profileDao.getById(id))
    }

    @Test
    fun deleteProfileCascadesToTimers() = runBlocking {
        val profileId = profileDao.insert(profile())
        val timer = TimerEntity(
            profileId = profileId, name = "Drink",
            durationSeconds = 600, timerType = "REPEATING", sortOrder = 0
        )
        timerDao.insert(timer)

        profileDao.deleteById(profileId)

        // After cascade delete, no timers should remain for this profile
        val remainingProfiles = profileDao.observeAll().first()
        assertTrue(remainingProfiles.isEmpty())
    }

    @Test
    fun updateChangesProfileName() = runBlocking {
        val id = profileDao.insert(profile("Old"))
        val updated = profileDao.getById(id)!!.profile.copy(name = "New")
        profileDao.update(updated)
        assertEquals("New", profileDao.getById(id)!!.profile.name)
    }

    @Test
    fun profileWithTimersIncludesRelatedTimers() = runBlocking {
        val profileId = profileDao.insert(profile())
        timerDao.insertAll(listOf(
            TimerEntity(profileId = profileId, name = "Drink", durationSeconds = 600, timerType = "REPEATING", sortOrder = 0),
            TimerEntity(profileId = profileId, name = "Eat", durationSeconds = 1800, timerType = "ONCE", sortOrder = 1)
        ))
        val result = profileDao.getById(profileId)
        assertNotNull(result)
        assertEquals(2, result!!.timers.size)
    }
}
