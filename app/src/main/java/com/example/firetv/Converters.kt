package com.example.firetv.model
import com.example.firetv.Movie

fun Recommendation.toMovie(): Movie {
    return Movie(
        id = (this.tmdb_id?.toString() ?: ""),  // Use TMDB ID as fallback ID
        title = this.title,
        description = this.description ?: "",
        studio = this.platform ?: "Unknown",
        videoUrl = "",                          // No video URL in Recommendation
        tmdb_id = this.tmdb_id ?: 0,
        deeplink = "",                          // Default empty, handled by dialog maybe
        homepage = this.homepage ?: "",
        poster_image = this.poster_image ?: "",
        cardImageUrl = this.poster_image ?: "", // Map poster to card
        backdrop_image = this.backdrop_image ?: "",
        deeplinks = this.available_platforms ?: emptyList(),
        rating = (this.rating ?: 0f).toDouble(),
        available_platforms = this.available_platforms ?: emptyList(),
        trailer = this.trailer,
        genres = this.genres ?: emptyList()
    )
}
