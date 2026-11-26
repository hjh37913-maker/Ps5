package com.kaizer.ps5simulator.data.model

/** Demo profile — local-only data, no PSN account is involved. */
data class UserProfile(
    val displayName: String = "Kaizer",
    val onlineName: String = "Kaizer_UA",
    val trophyLevel: Int = 47,
    val levelPercent: Int = 30,
    val platinum: Int = 9,
    val gold: Int = 63,
    val silver: Int = 141,
    val bronze: Int = 502,
    val avatarSeed: Int = 3,
    val plusMember: Boolean = true,
) {
    val totalTrophies: Int get() = platinum + gold + silver + bronze
}

data class Friend(
    val name: String,
    val status: FriendStatus,
    val detail: String,
    val avatarSeed: Int,
)

enum class FriendStatus(val label: String) {
    ONLINE("Online"),
    PLAYING("Playing now"),
    AWAY("Away"),
    OFFLINE("Offline"),
}

data class TrophyGroup(
    val gameId: String,
    val gameTitle: String,
    val platform: String,
    val progressPercent: Int,
    val platinum: Int,
    val gold: Int,
    val silver: Int,
    val bronze: Int,
    val lastPlayed: String,
)

data class ActivityEntry(
    val gameId: String,
    val title: String,
    val detail: String,
    val minutes: Int,
)

data class GameStat(
    val gameId: String,
    val playtimeMinutes: Int,
    val progressPercent: Int,
)

/** One "activity card" inside the Control Center / game hub. */
data class ActivityCard(
    val id: String,
    val gameId: String,
    val category: String,
    val title: String,
    val subtitle: String,
    val progress: Int?,
    val progressLabel: String?,
    val primaryAction: String,
    val minutesLeft: Int? = null,
)

/** A notification row in the Control Center notifications tab. */
data class Notif(
    val id: String,
    val gameId: String?,
    val title: String,
    val body: String,
    val time: String,
    val unread: Boolean = true,
)
