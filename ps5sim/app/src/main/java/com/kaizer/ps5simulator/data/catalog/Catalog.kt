package com.kaizer.ps5simulator.data.catalog

import android.content.Context
import com.kaizer.ps5simulator.data.model.ActivityCard
import com.kaizer.ps5simulator.data.model.Friend
import com.kaizer.ps5simulator.data.model.FriendStatus
import com.kaizer.ps5simulator.data.model.Game
import com.kaizer.ps5simulator.data.model.Notif
import com.kaizer.ps5simulator.data.model.TrophyGroup
import com.kaizer.ps5simulator.data.model.UserProfile
import org.json.JSONArray
import java.util.Locale

/**
 * Static demo content: the game library, friends, trophy roll-ups and activity cards.
 *
 * The library metadata is generated into assets/catalog.json by tools/gen_assets.py.
 * Everything here is local demo data for the simulation.
 */
object Catalog {

    private var games: List<Game> = emptyList()
    private var loadedFrom: String = "bundle"

    fun load(context: Context): List<Game> {
        if (games.isNotEmpty()) return games
        val list = try {
            val json = context.assets.open("catalog.json").bufferedReader().use { it.readText() }
            parse(json)
        } catch (t: Throwable) {
            emptyList()
        }
        games = if (list.isNotEmpty()) list else fallback
        loadedFrom = if (list.isNotEmpty()) "assets/catalog.json" else "embedded fallback"
        return games
    }

    fun source(): String = loadedFrom

    fun byId(id: String): Game? = games.firstOrNull { it.id == id }

    fun ordered(): List<Game> = games

    private fun parse(json: String): List<Game> {
        val arr = JSONArray(json)
        val out = ArrayList<Game>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out += Game(
                id = o.getString("id"),
                title = o.getString("title"),
                genre = o.getString("genre"),
                publisher = o.getString("publisher"),
                year = o.optInt("year", 2020),
                sizeGb = o.optDouble("sizeGb", 40.0),
                description = o.optString("description", ""),
                tagline = o.optString("tagline", ""),
                accent = parseColor(o.optString("accent", "#4879F5")),
                deep = parseColor(o.optString("deep", "#101018")),
                index = o.optInt("index", i),
            )
        }
        return out.sortedBy { it.index }
    }

    fun parseColor(hex: String): Int = try {
        val v = hex.removePrefix("#")
        val rgb = v.toLong(16).toInt()
        if (v.length <= 6) 0xFF000000.toInt() or rgb else rgb
    } catch (t: Throwable) {
        -0x1000000
    }

    // ---------------------------------------------------------------- demo profile
    val profile = UserProfile()

    val friends = listOf(
        Friend("Aurelia_W", FriendStatus.PLAYING, "God of War Ragnarök", 1),
        Friend("d1mitri", FriendStatus.ONLINE, "Console — online", 2),
        Friend("Nora.exe", FriendStatus.PLAYING, "Minecraft", 4),
        Friend("Kyiv_Storm", FriendStatus.AWAY, "Away — 12 min", 5),
        Friend("vynnyk", FriendStatus.ONLINE, "Console — online", 6),
        Friend("Sol_", FriendStatus.OFFLINE, "Last seen yesterday", 7),
    )

    val trophies: List<TrophyGroup> get() = games.take(8).mapIndexed { i, g ->
        val p = listOf(100, 87, 64, 55, 43, 31, 22, 9)[i]
        TrophyGroup(
            gameId = g.id,
            gameTitle = g.title,
            platform = "PS5",
            progressPercent = p,
            platinum = if (p == 100) 1 else 0,
            gold = (p / 12),
            silver = (p / 7),
            bronze = (p / 4),
            lastPlayed = listOf("2 hours ago", "Yesterday", "3 days ago", "1 week ago",
                "2 weeks ago", "3 weeks ago", "1 month ago", "2 months ago")[i],
        )
    }

    fun activitiesFor(game: Game): List<ActivityCard> {
        val base = game.title.lowercase(Locale.ROOT).hashCode()
        fun n(offset: Int, mod: Int) = (base + offset).mod(mod)
        return listOf(
            ActivityCard(
                id = "${game.id}_story", gameId = game.id, category = "Story",
                title = "Story progress", subtitle = "Continue your journey",
                progress = 20 + n(1, 60), progressLabel = "In progress",
                primaryAction = "Resume", minutesLeft = 25 + n(2, 90),
            ),
            ActivityCard(
                id = "${game.id}_activity", gameId = game.id, category = "Activity",
                title = "Explore the map", subtitle = "Collectibles and side stories",
                progress = 10 + n(3, 70), progressLabel = "In progress",
                primaryAction = "Jump in",
            ),
            ActivityCard(
                id = "${game.id}_trophy", gameId = game.id, category = "Trophy",
                title = "Trophy hunt", subtitle = "Remaining trophies in this chapter",
                progress = n(4, 90), progressLabel = "Completion",
                primaryAction = "Resume",
            ),
            ActivityCard(
                id = "${game.id}_friends", gameId = game.id, category = "Social",
                title = "Friends playing", subtitle = "${n(5, 4) + 1} friends in this game",
                progress = null, progressLabel = null, primaryAction = "View",
            ),
            ActivityCard(
                id = "${game.id}_news", gameId = game.id, category = "Official news",
                title = "What's new", subtitle = "Patch notes and events",
                progress = null, progressLabel = null, primaryAction = "Read",
            ),
        )
    }

    fun notifications(): List<Notif> = games.take(5).mapIndexed { i, g ->
        Notif(
            id = "notif_$i", gameId = g.id,
            title = g.title,
            body = when (i) {
                0 -> "The latest update is ready to install."
                1 -> "A friend started playing this game."
                2 -> "You earned a trophy in this game."
                3 -> "New activity available in your hub."
                else -> "Added to your collection."
            },
            time = listOf("Just now", "12 min ago", "1 h ago", "3 h ago", "Yesterday")[i],
            unread = i < 3,
        )
    }

    /** Games surfaced in the "Must see" carousel on the home screen. */
    fun mustSee(all: List<Game>): List<Game> {
        val pinned = listOf("gtavi", "ghostyotei", "spiderman2", "astrobot", "minecraft")
        val byId = all.associateBy { it.id }
        val out = ArrayList<Game>()
        pinned.forEach { id -> byId[id]?.let { out += it } }
        all.forEach { if (out.size < 9 && it !in out) out += it }
        return out
    }

    // ------------------------------------------------------------------ fallback
    private val fallback = listOf(
        Game("gtavi", "Grand Theft Auto VI", "Action-Adventure", "Rockstar Games", 2026, 112.0,
            "Vice City, Leonida.", "VICE CITY", 0xFFFF6CAA.toInt(), 0xFF280A28.toInt(), 0),
        Game("gowragnarok", "God of War Ragnarok", "Action-Adventure", "Santa Monica Studio", 2022, 84.0,
            "Kratos and Atreus face the end of the Norse realms.", "NINE REALMS",
            0xFF96C8EB.toInt(), 0xFF121C2C.toInt(), 1),
        Game("astrobot", "Astro Bot", "Platformer", "Team Asobi", 2024, 66.0,
            "A galaxy-sized rescue mission.", "GALAXY", 0xFF78D2FF.toInt(), 0xFF10284A.toInt(), 2),
    )
}
