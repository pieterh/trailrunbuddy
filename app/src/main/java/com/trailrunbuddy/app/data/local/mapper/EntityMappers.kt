package com.trailrunbuddy.app.data.local.mapper

import com.trailrunbuddy.app.data.local.entity.ProfileEntity
import com.trailrunbuddy.app.data.local.entity.SessionEntity
import com.trailrunbuddy.app.data.local.entity.TimerEntity
import com.trailrunbuddy.app.data.local.entity.TimerGroupEntity
import com.trailrunbuddy.app.data.local.relation.ProfileWithTimers
import com.trailrunbuddy.app.domain.model.Profile
import com.trailrunbuddy.app.domain.model.ProfileItem
import com.trailrunbuddy.app.domain.model.Session
import com.trailrunbuddy.app.domain.model.SessionState
import com.trailrunbuddy.app.domain.model.Timer
import com.trailrunbuddy.app.domain.model.TimerGroup
import com.trailrunbuddy.app.domain.model.TimerState
import com.trailrunbuddy.app.domain.model.TimerType

// ──────────────────────────────────────────
// Profile
// ──────────────────────────────────────────

fun ProfileWithTimers.toDomain(): Profile {
    val groupItem: ProfileItem.Group? = groups.firstOrNull()?.let { g ->
        val groupTimers = g.timers.sortedBy { it.sortOrder }.map { it.toDomain() }
        ProfileItem.Group(
            TimerGroup(
                id = g.group.id,
                profileId = g.group.profileId,
                sortOrder = g.group.sortOrder,
                timerType = TimerType.valueOf(g.group.timerType),
                timers = groupTimers
            )
        )
    }

    val standaloneItems: List<ProfileItem.StandaloneTimer> = timers
        .filter { it.groupId == null }
        .sortedBy { it.sortOrder }
        .map { ProfileItem.StandaloneTimer(it.toDomain()) }

    val allItems = (standaloneItems + listOfNotNull(groupItem)).sortedBy { it.sortOrder }

    return Profile(
        id = profile.id,
        name = profile.name,
        colorHex = profile.colorHex,
        createdAt = profile.createdAt,
        items = allItems,
        sortOrder = profile.sortOrder
    )
}

fun Profile.toEntity(): ProfileEntity = ProfileEntity(
    id = id,
    name = name,
    colorHex = colorHex,
    createdAt = createdAt,
    sortOrder = sortOrder
)

// ──────────────────────────────────────────
// Timer
// ──────────────────────────────────────────

fun TimerEntity.toDomain(): Timer = Timer(
    id = id,
    profileId = profileId,
    name = name,
    durationSeconds = durationSeconds,
    timerType = TimerType.valueOf(timerType),
    sortOrder = sortOrder
)

fun Timer.toEntity(
    profileId: Long = this.profileId,
    sortOrder: Int = this.sortOrder,
    groupId: Long? = null
): TimerEntity = TimerEntity(
    id = id,
    profileId = profileId,
    name = name,
    durationSeconds = durationSeconds,
    timerType = timerType.name,
    sortOrder = sortOrder,
    groupId = groupId
)

// ──────────────────────────────────────────
// TimerGroup
// ──────────────────────────────────────────

fun TimerGroup.toEntity(profileId: Long = this.profileId, sortOrder: Int = this.sortOrder): TimerGroupEntity =
    TimerGroupEntity(id = id, profileId = profileId, sortOrder = sortOrder, timerType = timerType.name)

// ──────────────────────────────────────────
// Session
// ──────────────────────────────────────────

fun SessionEntity.toDomain(): Session = Session(
    id = id,
    profileId = profileId,
    state = SessionState.valueOf(state),
    startedAt = startedAt,
    pausedAt = pausedAt,
    totalPausedMs = totalPausedMs,
    timerStates = decodeTimerStates(timerStatesJson)
)

fun Session.toEntity(): SessionEntity = SessionEntity(
    id = id,
    profileId = profileId,
    state = state.name,
    startedAt = startedAt,
    pausedAt = pausedAt,
    totalPausedMs = totalPausedMs,
    timerStatesJson = encodeTimerStates(timerStates)
)

// Format: "timerId:cycleCount:firedOnce;timerId:cycleCount:firedOnce"
private fun encodeTimerStates(states: List<TimerState>): String =
    states.joinToString(";") { "${it.timerId}:${it.cycleCount}:${it.firedOnce}" }

private fun decodeTimerStates(json: String): List<TimerState> {
    if (json.isBlank()) return emptyList()
    return json.split(";").mapNotNull { entry ->
        val parts = entry.split(":")
        if (parts.size == 3) {
            TimerState(
                timerId = parts[0].toLongOrNull() ?: return@mapNotNull null,
                cycleCount = parts[1].toIntOrNull() ?: 0,
                firedOnce = parts[2].toBooleanStrictOrNull() ?: false
            )
        } else null
    }
}
