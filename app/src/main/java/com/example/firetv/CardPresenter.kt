package com.example.firetv

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide

class CardPresenter(
    private val isZoomedModeProvider: () -> Boolean
) : Presenter() {
    constructor(isZoomedMode: Boolean) : this({ isZoomedMode }) // <--- legacy support

    private val normalCardWidth = 320
    private val normalCardHeight = 400
    private val zoomedCardWidth = 520
    private val zoomedCardHeight = 580

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val cardView = ImageCardView(parent.context).apply {
            isFocusable = true
            isFocusableInTouchMode = true
        }
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val movie = item as Movie
        val cardView = viewHolder.view as ImageCardView

        val isZoomedMode = isZoomedModeProvider()

        if (isZoomedMode) {
            cardView.setMainImageDimensions(zoomedCardWidth, zoomedCardHeight)
            cardView.titleText = ""
            cardView.contentText = ""
            cardView.findViewById<TextView>(androidx.leanback.R.id.title_text)?.apply {
                text = ""
                setTextColor(Color.TRANSPARENT)
                visibility = View.VISIBLE
                layoutParams?.height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
            cardView.findViewById<TextView>(androidx.leanback.R.id.content_text)?.apply {
                text = ""
                setTextColor(Color.TRANSPARENT)
                visibility = View.VISIBLE
                layoutParams?.height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
        } else {
            cardView.setMainImageDimensions(normalCardWidth, normalCardHeight)
            cardView.titleText = movie.title
            cardView.contentText = movie.studio
            cardView.findViewById<TextView>(androidx.leanback.R.id.title_text)
                ?.setTextColor(Color.WHITE)
            cardView.findViewById<TextView>(androidx.leanback.R.id.content_text)
                ?.setTextColor(Color.LTGRAY)
        }

        Glide.with(cardView.context)
            .load(movie.cardImageUrl)
            .centerCrop()
            .into(cardView.mainImageView)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val cardView = viewHolder.view as ImageCardView
        cardView.mainImage = null
    }
}
