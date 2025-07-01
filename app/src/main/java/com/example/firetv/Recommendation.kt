package com.example.firetv.model

data class Recommendation(
    val title: String,
    val original_title: String?,
    val platform: String?,
    val available_platforms: List<String>?,
    val tags: List<String>?,
    val genres: List<String>?,
    val description: String?,
    val trailer: String?,
    val poster_image: String?,
    val backdrop_image: String?,
    val imageUrl: String?,
    val rating: Float?,
    val vote_count: Int?,
    val popularity: Float?,
    val first_air_date: String?,
    val last_air_date: String?,
    val number_of_seasons: Int?,
    val number_of_episodes: Int?,
    val status: String?,
    val homepage: String?,
    val tmdb_id: Int?
)


class SkeletonMovie