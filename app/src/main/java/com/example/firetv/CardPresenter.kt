package com.example.firetv

import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RatingBar
import androidx.leanback.widget.BaseCardView
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide

class CardPresenter : Presenter() {

    companion object {
        private const val CARD_WIDTH = 313
        private const val CARD_HEIGHT = 176
    }

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val cardView = ImageCardView(parent.context).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
            cardType = BaseCardView.CARD_TYPE_INFO_UNDER
        }
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val movie = item as Movie
        val cardView = viewHolder.view as ImageCardView

        cardView.titleText = movie.title
        cardView.contentText = movie.studio

        // Set main image
        Glide.with(cardView.context)
            .load(movie.cardImageUrl)
            .centerCrop()
            .into(cardView.mainImageView)

        // Ensure we don't duplicate rating bars
        if (cardView.findViewWithTag<LinearLayout>("rating_container") == null) {
            val ratingBar = RatingBar(cardView.context, null, android.R.attr.ratingBarStyleSmall).apply {
                numStars = 5
                stepSize = 0.5f
                rating = movie.rating.toFloat().div(2).coerceIn(0f, 5f)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setIsIndicator(true)
            }

            val ratingContainer = LinearLayout(cardView.context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 0, 0, 16)
                tag = "rating_container"
                addView(ratingBar)
            }

            cardView.addView(ratingContainer)
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val cardView = viewHolder.view as ImageCardView
        cardView.mainImage = null

        // Remove rating container only
        cardView.findViewWithTag<LinearLayout>("rating_container")?.let {
            cardView.removeView(it)
        }
    }
}
