package com.example.firetv

import android.os.AsyncTask
import org.json.JSONObject
import java.net.URL

object RecommendationFetcher {
    fun fetchRecommendationsFromApi(apiUrl: String, callback: (List<Recommendation>) -> Unit) {
        AsyncTask.execute {
            try {
                val text = URL(apiUrl).readText()
                val data = JSONObject(text)
                val recs = mutableListOf<Recommendation>()
                // If your API returns { data: {...} }
                data.optJSONObject("data")?.let { obj ->
                    recs.add(parseRecommendation(obj))
                }
                callback(recs)
            } catch (e: Exception) {
                callback(emptyList())
            }
        }
    }


    private fun parseRecommendation(obj: JSONObject): Recommendation {
        val posterImage = obj.optString("poster_image")
        val backdropImage = obj.optString("backdrop_image")
        val imageUrl = when {
            !posterImage.isNullOrBlank() && posterImage != "N/A" -> posterImage
            !backdropImage.isNullOrBlank() && backdropImage != "N/A" -> backdropImage
            else -> null
        }
        return Recommendation(
            title = obj.optString("title", ""),
            original_title = obj.optString("original_title"),
            platform = obj.optString("platform"),
            available_platforms = obj.optJSONArray("available_platforms")?.let { arr -> List(arr.length()) { arr.getString(it) } },
            tags = obj.optJSONArray("tags")?.let { arr -> List(arr.length()) { arr.getString(it) } },
            genres = obj.optJSONArray("genres")?.let { arr -> List(arr.length()) { arr.getString(it) } },
            description = obj.optString("description"),
            trailer = obj.optString("trailer"),
            poster_image = posterImage,
            backdrop_image = backdropImage,
            imageUrl = imageUrl,   // <-- This sets the correct image for your card
            rating = if (obj.has("rating")) obj.optDouble("rating").toFloat() else null,
            vote_count = if (obj.has("vote_count")) obj.optInt("vote_count") else null,
            popularity = if (obj.has("popularity")) obj.optDouble("popularity").toFloat() else null,
            first_air_date = obj.optString("first_air_date"),
            last_air_date = obj.optString("last_air_date"),
            number_of_seasons = if (obj.has("number_of_seasons")) obj.optInt("number_of_seasons") else null,
            number_of_episodes = if (obj.has("number_of_episodes")) obj.optInt("number_of_episodes") else null,
            status = obj.optString("status"),
            homepage = obj.optString("homepage"),
            tmdb_id = if (obj.has("tmdb_id")) obj.optInt("tmdb_id") else null
        )
    }

}
