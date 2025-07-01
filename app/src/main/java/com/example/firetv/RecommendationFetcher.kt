package com.example.firetv

import android.os.AsyncTask
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import com.example.firetv.model.Recommendation
import java.net.URLEncoder

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

    fun fetchReasoningForTitle(title: String, callback: (String?) -> Unit) {
        Thread {
            try {
                val encodedTitle = URLEncoder.encode(title, "UTF-8")
                val url = "http://3.111.57.57:8080/reasoning?title=$encodedTitle"
                val result = URL(url).readText()
                val json = JSONObject(result)
                val reasoning = json.optString("reasoning", null)
                callback(reasoning)
            } catch (e: Exception) {
                Log.e("RecommendationFetcher", "Reasoning fetch error: ${e.message}")
                callback(null)
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