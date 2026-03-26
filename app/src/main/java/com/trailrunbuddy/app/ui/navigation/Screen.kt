package com.trailrunbuddy.app.ui.navigation

sealed class Screen(val route: String) {

    data object ProfileList : Screen("profile_list")

    data class ProfileDetail(val profileId: Long) : Screen("profile_detail/{profileId}") {
        fun createRoute(): String = "profile_detail/$profileId"

        companion object {
            const val ROUTE = "profile_detail/{profileId}"
            const val ARG_PROFILE_ID = "profileId"
            val newProfile: String = "profile_detail/-1"
        }
    }

    data class ActiveSession(val profileId: Long) : Screen("active_session/{profileId}") {
        fun createRoute(): String = "active_session/$profileId"

        companion object {
            const val ROUTE = "active_session/{profileId}"
            const val ARG_PROFILE_ID = "profileId"
        }
    }

    data object Settings : Screen("settings")
}
