package com.example.firetv

import java.io.Serializable

data class Movie(
    val id: String = "",                                // Firebase logs & card
    val title: String = "",                             // Shown on card & banner
    val description: String = "",                       // Used in details view
    val studio: String = "",                            // Used in details view
    val videoUrl: String = "",                          // Playback intent
    val deeplink: String = "",                          // For opening apps
    val homepage: String = "",                          // Optional external link

    val poster_image: String = "",                      // Detailed screen & full
    val cardImageUrl: String = "",                      // CardPresenter
    val backdrop_image: String = "",                    // VideoDetailsFragment
    val deeplinks: List<String> = emptyList(),
    val rating: Double = 0.0,                           // Star rating
    val available_platforms: List<String> = emptyList() // For FirebaseLogger
) : Serializable
