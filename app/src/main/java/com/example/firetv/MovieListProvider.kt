package com.example.firetv

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object MovieListProvider {
    fun getMovieList(context: Context): List<Movie> {
        val json = context.assets.open("tmdb_complete_shows_20250615_135431.json")
            .bufferedReader().use { it.readText() }

        val listType = object : TypeToken<List<Movie>>() {}.type
        return Gson().fromJson(json, listType)
    }
}
