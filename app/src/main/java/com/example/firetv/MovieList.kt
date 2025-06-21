package com.example.firetv

import android.content.Context
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

    fun getMovies(context: Context): List<Movie> {
        val movies = mutableListOf<Movie>()
        try {
            val inputStream = context.assets.open("netflix_prime_content_deduped.json")
            val jsonStr = inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonStr)

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)

                // Only include movies with at least one allowed platform
                val availablePlatformsArray = obj.optJSONArray("available_platforms")
                val matchingPlatforms = mutableListOf<String>()
                if (availablePlatformsArray != null) {
                    for (j in 0 until availablePlatformsArray.length()) {
                        val platform = availablePlatformsArray.getString(j)
                        if (platform in allowedPlatforms) {
                            matchingPlatforms.add(platform)
                        }
                    }
                }

                if (matchingPlatforms.isEmpty()) {
                    continue // Skip this movie
                }

                val movie = Movie(
                    title = obj.optString("title"),
                    description = obj.optString("description"),
                    studio = obj.optString("platform", "Unknown"),
                    homepage = obj.optString("homepage"),
                    poster_image = obj.optString("poster_image"),
                    cardImageUrl = obj.optString("poster_image"),
                    backdrop_image = obj.optString("backdrop_image"),
                    deeplinks = matchingPlatforms, // Only Netflix/Prime names, for dialog
                    rating = obj.optDouble("rating", 0.0),
                    trailer = obj.optString("trailer", null)
                )

                movies.add(movie)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return movies
    }
}
