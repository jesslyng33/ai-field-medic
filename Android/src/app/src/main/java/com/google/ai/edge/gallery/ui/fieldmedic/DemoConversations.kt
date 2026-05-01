package com.google.ai.edge.gallery.ui.fieldmedic

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.IOException

private const val TAG = "DemoConversations"
private const val ASSET_PATH = "demo_conversations.json"

object DemoConversations {

    data class Turn(
        val keyPhrases: List<String>,
        val minMatches: Int,
        val response: String,
    )

    data class Demo(
        val id: String,
        val turns: List<Turn>,
    )

    @Volatile private var demos: List<Demo> = emptyList()
    @Volatile private var loaded = false

    private var activeDemoIdx: Int? = null
    private var nextTurnIdx: Int = 0

    fun loadIfNeeded(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            try {
                val json = context.assets.open(ASSET_PATH)
                    .bufferedReader().use { it.readText() }
                demos = parse(json)
                Log.i(TAG, "Loaded ${demos.size} demo(s) from assets/$ASSET_PATH")
            } catch (e: IOException) {
                Log.w(TAG, "No demo conversations file at assets/$ASSET_PATH: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse $ASSET_PATH", e)
            }
            loaded = true
        }
    }

    private fun parse(json: String): List<Demo> {
        val root = JSONObject(json)
        val arr = root.getJSONArray("demos")
        val out = mutableListOf<Demo>()
        for (i in 0 until arr.length()) {
            val d = arr.getJSONObject(i)
            val id = d.optString("id", "demo_$i")
            val turnsArr = d.getJSONArray("turns")
            val turns = mutableListOf<Turn>()
            for (j in 0 until turnsArr.length()) {
                val t = turnsArr.getJSONObject(j)
                val keysArr = t.getJSONArray("keyPhrases")
                val keys = mutableListOf<String>()
                for (k in 0 until keysArr.length()) {
                    keys.add(normalize(keysArr.getString(k)))
                }
                val min = t.optInt("minMatches", 2).coerceAtLeast(1)
                val resp = t.getString("response")
                turns.add(Turn(keys, min, resp))
            }
            out.add(Demo(id, turns))
        }
        return out
    }

    @Synchronized
    fun reset() {
        activeDemoIdx = null
        nextTurnIdx = 0
    }

    @Synchronized
    fun tryMatch(userText: String): String? {
        if (demos.isEmpty()) return null
        val normalized = normalize(userText)
        if (normalized.isBlank()) return null

        val activeIdx = activeDemoIdx
        if (activeIdx != null) {
            val demo = demos[activeIdx]
            if (nextTurnIdx >= demo.turns.size) return null
            val turn = demo.turns[nextTurnIdx]
            if (matches(normalized, turn)) {
                Log.i(TAG, "demo[${demo.id}] matched turn ${nextTurnIdx + 1}/${demo.turns.size}")
                nextTurnIdx += 1
                return turn.response
            }
            return null
        }

        for ((i, demo) in demos.withIndex()) {
            val first = demo.turns.firstOrNull() ?: continue
            if (matches(normalized, first)) {
                activeDemoIdx = i
                nextTurnIdx = 1
                Log.i(TAG, "Activated demo[${demo.id}] on opening turn")
                return first.response
            }
        }
        return null
    }

    private fun matches(normalizedText: String, turn: Turn): Boolean {
        if (turn.keyPhrases.isEmpty()) return false
        var hits = 0
        for (kp in turn.keyPhrases) {
            if (kp.isNotEmpty() && normalizedText.contains(kp)) hits += 1
            if (hits >= turn.minMatches) return true
        }
        return false
    }

    private fun normalize(s: String): String = s
        .lowercase()
        .replace(Regex("[^a-z0-9 ']"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}
