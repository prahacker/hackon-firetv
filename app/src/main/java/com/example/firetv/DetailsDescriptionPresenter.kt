package com.example.firetv

import androidx.leanback.widget.AbstractDetailsDescriptionPresenter

class DetailsDescriptionPresenter : AbstractDetailsDescriptionPresenter() {

    override fun onBindDescription(
        viewHolder: ViewHolder,
        item: Any
    ) {
        val movie = item as Movie
        viewHolder.title.text = movie.title
        viewHolder.subtitle.text = movie.available_platforms.firstOrNull() ?: "Unknown Platform"
        viewHolder.body.text = movie.description
    }
}
