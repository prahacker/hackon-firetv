package com.example.firetv

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FirebaseLogger {

    private const val USER_ID = "USER_ABC_123"

    private val db = FirebaseDatabase.getInstance(
        "https://firetv-project-ba2ad-default-rtdb.asia-southeast1.firebasedatabase.app"
    ).reference.child("interactions")

    private fun getFocusedItemMap(movie: Movie?): Map<String, Any>? {
        return if (movie != null) {
            mapOf(
                "item_id" to (movie.getUniqueId() ?: "unknown_id"),
                "genres" to if (movie.genres.isNotEmpty()) movie.genres else listOf(movie.getGenre() ?: "unknown"),
                "title" to (movie.title ?: "unknown_title")
            )
        } else {
            null
        }
    }

    fun logInteraction(
        actionType: String,
        screenContext: String,
        focusedItem: Any?, // you can keep this flexible
        timeSinceLastAction: Float,
        consecutiveActionCount: Int,
        hoverDuration: Float? = null,
        clickType: String? = null,
        scrollSpeed: Float? = null,
        scrollDepth: Float? = null,
        playbackPosition: Long? = null,
        sessionEndReason: String? = null,
        genre: Any? = null
    ) {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        val timestamp = formatter.format(Date())

        // ✅ Safely cast to Movie (or handle other types later if needed)
        val focusedItemMap = when (focusedItem) {
            is Movie -> getFocusedItemMap(focusedItem)
            else -> null
        }

        val logData = mutableMapOf<String, Any?>(
            "timestamp" to timestamp,
            "session_id" to SessionTracker.sessionId,
            "user_id" to USER_ID,
            "action_type" to actionType,
            "screen_context" to screenContext,
            "focused_item" to focusedItemMap,
            "sequence_context" to mapOf(
                "time_since_last_action" to timeSinceLastAction,
                "consecutive_action_count" to consecutiveActionCount
            )
        )

        // Genre only for non-navigation events
        if (genre != null && actionType !in setOf("dpad_up", "dpad_down", "dpad_left", "dpad_right", "back")) {
            logData["genre"] = genre
        }

        hoverDuration?.let { logData["hover_duration"] = it }
        clickType?.let { logData["click_type"] = it }
        scrollSpeed?.let { logData["scroll_speed"] = it }
        scrollDepth?.let { logData["scroll_depth"] = it }
        playbackPosition?.let { logData["playback_position"] = it }
        sessionEndReason?.let { logData["session_end_reason"] = it }

        db.push().setValue(logData).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("FirebaseLogger", "Successfully logged $actionType")
            } else {
                Log.e("FirebaseLogger", "Failed to log $actionType: ${task.exception?.message}")
            }
        }
    }

    fun logMovement(direction: String) {
        logInteraction(
            actionType = direction,
            screenContext = "Home",
            focusedItem = null,
            timeSinceLastAction = SequenceTracker.timeSinceLastAction(),
            consecutiveActionCount = SequenceTracker.increment(direction)
        )
    }
}
