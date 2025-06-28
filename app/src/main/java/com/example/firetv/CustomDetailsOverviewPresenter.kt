package com.example.firetv

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.leanback.widget.Presenter

class CustomDetailsOverviewPresenter : Presenter() {
    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.custom_details_overview, parent, false)
        return CustomDetailsViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val movie = item as? Movie ?: return
        val holder = viewHolder as CustomDetailsViewHolder

        holder.title.text = movie.title.ifBlank { "Untitled" }

        holder.ratingBar.apply {
            rating = (movie.rating / 2.0).toFloat().coerceIn(0f, 5f)
            visibility = if (movie.rating > 0) View.VISIBLE else View.GONE
        }

        holder.subtitle.text = movie.available_platforms.firstOrNull() ?: "Unknown Platform"

        val desc = StringBuilder(movie.description.ifBlank { "No description available." })
        holder.body.text = desc.toString()
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val holder = viewHolder as CustomDetailsViewHolder
        holder.title.text = null
        holder.ratingBar.rating = 0f
        holder.subtitle.text = null
        holder.body.text = null
    }

    class CustomDetailsViewHolder(view: View) : ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.details_title)
        val ratingBar: RatingBar = view.findViewById(R.id.details_rating_bar)
        val subtitle: TextView = view.findViewById(R.id.details_subtitle)
        val body: TextView = view.findViewById(R.id.details_body)
    }
}
