package com.example.firetv

import android.util.Log
import com.google.firebase.database.FirebaseDatabase

object FirebaseLogger {

    // Root logs reference (shared bucket)
    private val db = FirebaseDatabase.getInstance(
        "https://firetv-project-ba2ad-default-rtdb.asia-southeast1.firebasedatabase.app"
    ).reference.child("logs")

    /**
     * Logs a hover event
     */
    fun logHover(movie: Movie) {
        db.child("hovers").push().setValue(
            mapOf(
                "event" to "hover",
                "movie_id" to movie.id,
                "title" to movie.title,
                "timestamp" to System.currentTimeMillis(),
                "session_id" to SessionTracker.sessionId
            )
        )
        Log.d("FirebaseLogger", "Hover: ${movie.title}")
    }

    /**
     * Logs how long a user hovered over a movie
     */
    fun logHoverDuration(movieId: String, start: Long, end: Long) {
        db.child("hover_durations").push().setValue(
            mapOf(
                "event" to "hover_duration",
                "movie_id" to movieId,
                "duration" to (end - start),
                "start_time" to start,
                "end_time" to end,
                "session_id" to SessionTracker.sessionId
            )
        )
        Log.d("FirebaseLogger", "Hover Duration for $movieId: ${end - start}ms")
    }

    /**
     * Logs click on a movie, including the platform chosen
     */
    fun logClick(movie: Movie) {
        db.child("clicks").push().setValue(
            mapOf(
                "event" to "click",
                "movie_id" to movie.id,
                "platform" to (movie.available_platforms.firstOrNull() ?: "Unknown"),
                "title" to movie.title,
                "clicked_at" to System.currentTimeMillis(),
                "session_id" to SessionTracker.sessionId
            )
        )
        Log.d("FirebaseLogger", "Click: ${movie.title}")
    }

    /**
     * Logs trailer playback duration in separate folder
     */
    fun logTrailerDuration(title: String, durationMs: Long) {
        FirebaseDatabase.getInstance(
            "https://firetv-project-ba2ad-default-rtdb.asia-southeast1.firebasedatabase.app"
        ).reference.child("trailer_logs").push().setValue(
            mapOf(
                "event" to "trailer_play",
                "title" to title,
                "duration_seconds" to durationMs / 1000.0,
                "timestamp" to System.currentTimeMillis(),
                "session_id" to SessionTracker.sessionId
            )
        )
        Log.d("FirebaseLogger", "Trailer for '$title' played for ${durationMs / 1000.0} sec")
    }

    /**
     * Logs app-side errors
     */
    fun logError(message: String) {
        db.child("errors").push().setValue(
            mapOf(
                "event" to "error",
                "message" to message,
                "timestamp" to System.currentTimeMillis(),
                "session_id" to SessionTracker.sessionId
            )
        )
        Log.e("FirebaseLogger", "Error: $message")
    }
}
