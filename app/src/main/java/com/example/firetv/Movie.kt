package com.example.firetv

import org.json.JSONObject
import java.io.Serializable

data class Movie(
    val id: String = "",
    val title: String = "",
    var description: String = "",
    val studio: String = "",
    val videoUrl: String = "",
    val tmdb_id: Int = 0,
    val deeplink: String = "",
    val homepage: String = "",
    val poster_image: String = "",
    val cardImageUrl: String = "",
    val backdrop_image: String = "",
    val deeplinks: List<String> = emptyList(),
    val rating: Double = 0.0,
    val available_platforms: List<String> = emptyList(),
    val trailer: String? = null,
    val genres: List<String> = emptyList()
) : Serializable {
    fun getGenre(): String {
        return genres.firstOrNull() ?: "Unknown"
    }

    fun getUniqueId(): String? {
        return when {
            tmdb_id != 0 -> tmdb_id.toString()
            id.isNotBlank() -> id
            title.isNotBlank() -> title
            else -> null
        }
    }

    companion object {
        fun fromJson(obj: JSONObject): Movie {
            val deeplinksObj = obj.optJSONObject("deeplinks")
            val deeplink = deeplinksObj?.optString("IN") ?: ""

            return Movie(
                title = obj.optString("title", ""),
                description = obj.optString("description", ""),
                poster_image = obj.optString("poster_image", ""),
                cardImageUrl = obj.optString("poster_image", ""),
                backdrop_image = obj.optString("backdrop_image", ""),
                trailer = obj.optString("trailer", null),
                deeplink = deeplink,
                tmdb_id = obj.optInt("tmdb_id", 0),
                homepage = obj.optString("homepage", ""),
                // Add the following line to parse the rating
                rating = obj.optDouble("rating", 0.0),
                available_platforms = obj.optJSONArray("available_platforms")?.let { arr ->
                    List(arr.length()) { i -> arr.getString(i) }
                } ?: emptyList(),
                genres = obj.optJSONArray("genres")?.let { arr ->
                    List(arr.length()) { i -> arr.getString(i) }
                } ?: emptyList())

        }
    }
}