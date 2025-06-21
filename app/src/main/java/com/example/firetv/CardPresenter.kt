package com.example.firetv

import android.graphics.Color
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
        val cardView = viewHolder.view as ImageCardView

        // --- SKELETON PLACEHOLDER ---
        if (item is SkeletonMovie) {
            cardView.titleText = ""
            cardView.contentText = ""
            cardView.mainImage = null
            cardView.setBackgroundColor(Color.parseColor("#e0e0e0")) // Grey
            // Remove rating bar if present
            cardView.findViewWithTag<LinearLayout>("rating_container")?.let {
                cardView.removeView(it)
            }
            return
        } else {
            cardView.setBackgroundColor(Color.TRANSPARENT)
        }

        // --- MOVIE TYPE (Popular row) ---
        if (item is Movie) {
            cardView.titleText = item.title
            cardView.contentText = item.studio ?: ""
            Glide.with(cardView.context)
                .load(item.cardImageUrl)
                .centerCrop()
                .into(cardView.mainImageView)
            setRating(cardView, item.rating)
            return
        }

        // --- RECOMMENDATION TYPE (from API) ---
        if (item is Recommendation) {
            cardView.titleText = item.title
            cardView.contentText = item.description ?: ""
            // Prefer imageUrl, fallback to poster_image if missing
            val mainImg = item.imageUrl ?: item.poster_image ?: item.imageUrl

            Glide.with(cardView.context)
                .load(mainImg)
                .centerCrop()
                .into(cardView.mainImageView)
            // If you want to show rating, use item.rating; else, skip or use 0.0
            setRating(cardView, item.rating?.toDouble() ?: 0.0)
            return
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

    // Helper: Adds a RatingBar to the card view
    private fun setRating(cardView: ImageCardView, rating: Double) {
        // Remove old rating bar if present
        cardView.findViewWithTag<LinearLayout>("rating_container")?.let {
            cardView.removeView(it)
        }
        // Add new RatingBar
        val ratingBar = RatingBar(cardView.context, null, android.R.attr.ratingBarStyleSmall).apply {
            numStars = 5
            stepSize = 0.5f
            this.rating = ((rating / 2).coerceIn(0.0, 5.0)).toFloat()
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
