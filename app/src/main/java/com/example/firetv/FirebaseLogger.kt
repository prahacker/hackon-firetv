package com.example.firetv

import com.google.firebase.database.FirebaseDatabase

object FirebaseLogger {
    private val db = FirebaseDatabase.getInstance(
        "https://firetv-project-ba2ad-default-rtdb.asia-southeast1.firebasedatabase.app"
    ).reference.child("logs")

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
    }

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
    }


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
    }
    fun logError(message: String) {
        db.child("errors").push().setValue(
            mapOf(
                "event" to "error",
                "message" to message,
                "timestamp" to System.currentTimeMillis(),
                "session_id" to SessionTracker.sessionId
            )
        )
    }

}
