package com.example.firetv

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object MovieListProvider {
    fun getMovieList(context: Context): List<Movie> {
        val json = context.assets.open("netflix_prime_content_deduped.json")
            .bufferedReader().use { it.readText() }

        val listType = object : TypeToken<List<Movie>>() {}.type
        return Gson().fromJson(json, listType)
    }
}
