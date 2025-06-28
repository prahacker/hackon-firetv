package com.example.firetv

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONArray

object MovieList {
    private val allowedPlatforms = setOf(
        "Netflix",
        "Netflix India",
        "Amazon Prime Video",
        "Prime Video",
        "Amazon Prime Video with Ads",
        "Netflix Standard with Ads"
    )

    fun getMovies(context: Context, callback: (List<Movie>) -> Unit) {
        Thread {
            val movies = mutableListOf<Movie>()
            try {
                val jsonStr = context.assets.open("netflix_prime_content_deduped.json")
                    .bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(jsonStr)

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val movie = Movie.fromJson(obj)
                    movies.add(movie)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            // Post the result back to the Main UI Thread
            Handler(Looper.getMainLooper()).post {
                callback(movies)
            }
        }.start()
    }
}
