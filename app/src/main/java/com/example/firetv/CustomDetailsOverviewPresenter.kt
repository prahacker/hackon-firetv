package com.example.firetv

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide

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
        holder.subtitle.text = movie.available_platforms.firstOrNull() ?: "Unknown Platform"
        holder.body.text = movie.description.ifBlank { "No description available." }

        Glide.with(viewHolder.view.context)
            .load(movie.cardImageUrl)
            .into(holder.poster)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val holder = viewHolder as CustomDetailsViewHolder
        // Clear views
        holder.poster.setImageDrawable(null)
        holder.title.text = null
        holder.subtitle.text = null
        holder.body.text = null
    }

    class CustomDetailsViewHolder(view: View) : ViewHolder(view) {
        val poster: ImageView = view.findViewById(R.id.details_poster)
        val title: TextView = view.findViewById(R.id.details_title)
        val subtitle: TextView = view.findViewById(R.id.details_subtitle)
        val body: TextView = view.findViewById(R.id.details_body)
    }
}