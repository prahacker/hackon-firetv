package com.example.firetv

import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide

class CardPresenter(
    private val isZoomedModeProvider: () -> Boolean
) : Presenter() {

    private val normalCardWidth = 320
    private val normalCardHeight = 400
    private val zoomedCardWidth = 384
    private val zoomedCardHeight = 480
    private val animationDuration = 150L

    constructor(isZoomedMode: Boolean) : this({ isZoomedMode })

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

        val isZoomed = isZoomedModeProvider()

        val baseWidth = if (isZoomed) zoomedCardWidth else normalCardWidth
        val baseHeight = if (isZoomed) zoomedCardHeight else normalCardHeight
        cardView.setMainImageDimensions(baseWidth, baseHeight)

        // --- THIS IS THE CHANGE ---
        // Conditionally set the text and info area visibility based on the zoom mode
        if (isZoomed) {
            // In zoomed mode, hide the text and the entire grey info area
            cardView.titleText = null
            cardView.contentText = null
            cardView.setInfoVisibility(View.GONE) // Hide the info area
        } else {
            // In normal mode, show the text and the info area
            cardView.titleText = movie.title
            cardView.contentText = movie.studio
            cardView.setInfoVisibility(View.VISIBLE) // Show the info area
        }
        // --- END OF CHANGE ---

        Glide.with(cardView.context)
            .load(movie.cardImageUrl)
            .centerCrop()
            .into(cardView.mainImageView)

        cardView.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            val unfocusedWidth = if (isZoomed) zoomedCardWidth else normalCardWidth
            val unfocusedHeight = if (isZoomed) zoomedCardHeight else normalCardHeight

            val focusedWidth = (unfocusedWidth * 1.1f).toInt()
            val focusedHeight = (unfocusedHeight * 1.1f).toInt()

            val startWidth = if (hasFocus) unfocusedWidth else focusedWidth
            val endWidth = if (hasFocus) focusedWidth else unfocusedWidth
            val startHeight = if (hasFocus) unfocusedHeight else focusedHeight
            val endHeight = if (hasFocus) focusedHeight else unfocusedHeight

            animateSizeChange(cardView, startWidth, endWidth, startHeight, endHeight)
        }
    }

    private fun animateSizeChange(cardView: ImageCardView, startWidth: Int, endWidth: Int, startHeight: Int, endHeight: Int) {
        val widthAnimator = ValueAnimator.ofInt(startWidth, endWidth)
        widthAnimator.duration = animationDuration
        widthAnimator.addUpdateListener {
            val width = it.animatedValue as Int
            val height = startHeight + ((it.animatedFraction * (endHeight - startHeight)).toInt())
            cardView.setMainImageDimensions(width, height)
        }
        widthAnimator.start()
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val cardView = viewHolder.view as ImageCardView
        cardView.onFocusChangeListener = null
        cardView.mainImage = null
    }
}