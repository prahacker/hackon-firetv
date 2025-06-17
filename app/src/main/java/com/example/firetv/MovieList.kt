package com.example.firetv

import android.content.Context
import org.json.JSONArray

object MovieList {
    fun getMovies(context: Context): List<Movie> {
        val movies = mutableListOf<Movie>()
        try {
            val inputStream = context.assets.open("tmdb_complete_shows_20250615_135431.json")
            val jsonStr = inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonStr)

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)

                val movie = Movie(
                    title = obj.optString("title"),
                    description = obj.optString("description"),
                    studio = obj.optString("platform", "Unknown"),
                    homepage = obj.optString("homepage"),
                    poster_image = obj.optString("poster_image"),
                    cardImageUrl = obj.optString("poster_image"),
                    backdrop_image = obj.optString("backdrop_image"),
                    deeplinks = obj.optJSONArray("available_platforms")?.let { array ->
                        List(array.length()) { array.getString(it) }
                    } ?: emptyList(),
                    rating = obj.optDouble("rating", 0.0)
                )

                movies.add(movie)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return movies
    }
}
