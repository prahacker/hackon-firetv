package com.example.firetv

import android.os.AsyncTask
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import com.example.firetv.model.Recommendation
object RecommendationFetcher {

    fun fetchRecommendationsFromApi(url: String, callback: (List<Movie>) -> Unit) {
        Thread {
            try {
                val result = URL(url).readText()
                val json = JSONObject(result)
                val movies = parseRecommendations(json)
                callback(movies)
            } catch (e: Exception) {
                Log.e("RecommendationFetcher", "API fetch error: ${e.message}")

                callback(emptyList())
            }
        }.start()
    }

    fun fetchBlocking(url: String): List<Movie> {
        return try {
            val result = URL(url).readText()
            val json = JSONObject(result)
            parseRecommendations(json)
        } catch (e: Exception) {
            Log.e("RecommendationFetcher", "Blocking fetch error: ${e.message}")
            emptyList()
        }
    }

    private fun parseRecommendations(json: JSONObject): List<Movie> {
        val list = mutableListOf<Movie>()
        val recArray = json.optJSONArray("recommendation") ?: return list
        for (i in 0 until recArray.length()) {
            val obj = recArray.getJSONObject(i)
            val movie = Movie.fromJson(obj)
            list.add(movie)
        }


        return list
    }
}
