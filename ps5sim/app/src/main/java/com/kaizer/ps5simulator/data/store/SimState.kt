package com.kaizer.ps5simulator.data.store

import android.content.Context
import android.content.SharedPreferences

/**
 * Simulated console state: volumes, radios, power mode, and library bookkeeping
 * (favourites, installs, launch history). This *simulates* a console; it never
 * touches real device volume, Bluetooth or network APIs.
 */
class SimState(context: Context) {

    private val sp: SharedPreferences =
        context.getSharedPreferences("ps5sim_state", Context.MODE_PRIVATE)

    var speakerVolume: Int
        get() = sp.getInt("speaker", 70)
        set(v) = sp.edit().putInt("speaker", v.coerceIn(0, 100)).apply()

    var musicVolume: Int
        get() = sp.getInt("music", 35)
        set(v) = sp.edit().putInt("music", v.coerceIn(0, 100)).apply()

    var microphoneMuted: Boolean
        get() = sp.getBoolean("mic_muted", false)
        set(v) = sp.edit().putBoolean("mic_muted", v).apply()

    var wifiOn: Boolean
        get() = sp.getBoolean("wifi", true)
        set(v) = sp.edit().putBoolean("wifi", v).apply()

    var bluetoothOn: Boolean
        get() = sp.getBoolean("bt", true)
        set(v) = sp.edit().putBoolean("bt", v).apply()

    var notificationsMuted: Boolean
        get() = sp.getBoolean("notif_muted", false)
        set(v) = sp.edit().putBoolean("notif_muted", v).apply()

    var brightness: Int
        get() = sp.getInt("brightness", 80)
        set(v) = sp.edit().putInt("brightness", v.coerceIn(20, 100)).apply()

    var doNotDisturb: Boolean
        get() = sp.getBoolean("dnd", false)
        set(v) = sp.edit().putBoolean("dnd", v).apply()

    var nowPlaying: String
        get() = sp.getString("now_playing", "Simulator Soundtrack") ?: "Simulator Soundtrack"
        set(v) = sp.edit().putString("now_playing", v).apply()

    var musicPlaying: Boolean
        get() = sp.getBoolean("music_playing", false)
        set(v) = sp.edit().putBoolean("music_playing", v).apply()

    var selectedGameId: String?
        get() = sp.getString("selected_game", null)
        set(v) = sp.edit().putString("selected_game", v).apply()

    var lastLaunchId: String?
        get() = sp.getString("last_launch", null)
        set(v) = sp.edit().putString("last_launch", v).apply()

    var homeTab: Int
        get() = sp.getInt("home_tab", 0)
        set(v) = sp.edit().putInt("home_tab", v.coerceIn(0, 1)).apply()

    val installedIds: Set<String>
        get() = sp.getStringSet("installed", defaultInstalled()) ?: defaultInstalled()

    fun toggleInstalled(id: String) {
        val cur = installedIds.toMutableSet()
        if (!cur.add(id)) cur.remove(id)
        sp.edit().putStringSet("installed", cur).apply()
    }

    val favoriteIds: Set<String>
        get() = sp.getStringSet("favorites", setOf("gtavi", "spiderman2")) ?: emptySet()

    fun toggleFavorite(id: String): Boolean {
        val cur = favoriteIds.toMutableSet()
        val added = cur.add(id)
        if (!added) cur.remove(id)
        sp.edit().putStringSet("favorites", cur).apply()
        return added
    }

    fun recordLaunch(id: String) {
        lastLaunchId = id
        val cur = order().toMutableList()
        cur.remove(id)
        cur.add(0, id)
        sp.edit().putString("recents", cur.take(12).joinToString(",")).apply()
        val count = sp.getInt("launch_count_$id", 0) + 1
        sp.edit().putInt("launch_count_$id", count).apply()
    }

    fun launchCount(id: String): Int = sp.getInt("launch_count_$id", 0)

    fun order(): List<String> =
        (sp.getString("recents", "") ?: "").split(",").filter { it.isNotBlank() }

    fun recentIds(): List<String> = order()

    fun playMinutes(id: String): Int = 45 + (launchCount(id) * 37) % 900

    private fun defaultInstalled() = setOf(
        "gtavi", "gtav", "gowragnarok", "spiderman2", "tlou2", "ghostyotei",
        "astrobot", "minecraft", "fortnite", "re4", "horizonfw", "eldenring",
    )

    fun reset() {
        sp.edit().clear().apply()
    }
}
