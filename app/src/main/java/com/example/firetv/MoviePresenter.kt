package com.example.firetv.presenter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.leanback.widget.BaseCardView
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.firetv.Movie

class MoviePresenter : Presenter() {

    companion object {
        private const val TAG = "MoviePresenter"
    }

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val cardView = ImageCardView(parent.context).apply {
            isFocusable = true
            isFocusableInTouchMode = true

            try {
                cardType = BaseCardView.CARD_TYPE_INFO_UNDER
                setMainImageDimensions(313, 176)
                setInfoAreaBackgroundColor(Color.parseColor("#CC000000"))
                setInfoVisibility(View.VISIBLE)

                // Safe background removal
                background = null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    foreground = null
                }

                // Safe focus change listener with proper error handling
                setOnFocusChangeListener { view, hasFocus ->
                    try {
                        handleFocusChange(view, hasFocus)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in focus change listener", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up card view", e)
            }
        }

        return ViewHolder(cardView)
    }

    private fun handleFocusChange(view: View, hasFocus: Boolean) {
        // Ensure view is ImageCardView
        if (view !is ImageCardView) {
            Log.w(TAG, "View is not ImageCardView")
            return
        }

        try {
            if (hasFocus) {
                // Focus effects
                view.setInfoAreaBackgroundColor(Color.parseColor("#E6000000"))

                // Safe animation
                view.animate()
                    .scaleX(1.05f)
                    .scaleY(1.05f)
                    .setDuration(200)
                    .start()

                // Safe glow effect
                try {
                    val glowDrawable = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = 8f
                        setStroke(3, Color.WHITE)
                        setColor(Color.TRANSPARENT)
                    }
                    view.background = glowDrawable
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to set glow effect", e)
                }

                // Safe elevation
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    try {
                        view.elevation = 12f
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to set elevation", e)
                    }
                }

            } else {
                // Unfocus effects
                view.setInfoAreaBackgroundColor(Color.parseColor("#80000000"))

                // Safe animation back
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(200)
                    .start()

                // Remove effects safely
                try {
                    view.background = null
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        view.elevation = 0f
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to remove effects", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling focus change", e)
        }
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        try {
            val movie = item as? Movie
            if (movie == null) {
                Log.e(TAG, "Item is not a Movie object")
                return
            }

            val cardView = viewHolder.view as? ImageCardView
            if (cardView == null) {
                Log.e(TAG, "ViewHolder view is not ImageCardView")
                return
            }

            // Safe text setting
            cardView.titleText = movie.title ?: "Unknown Title"
            cardView.contentText = movie.available_platforms.firstOrNull() ?: "Unknown Platform"



            // Safe image loading
            try {
                Glide.with(cardView.context)
                    .load(movie.poster_image)
                    .apply(RequestOptions().centerCrop())
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(cardView.mainImageView)
            } catch (e: Exception) {
                Log.e("CardPresenter", "Error loading image", e)
            }


        } catch (e: Exception) {
            Log.e(TAG, "Error in onBindViewHolder", e)
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        try {
            val cardView = viewHolder.view as? ImageCardView
            cardView?.let {
                it.clearAnimation()
                it.scaleX = 1.0f
                it.scaleY = 1.0f
                it.background = null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    it.elevation = 0f
                }
                it.badgeImage = null
                it.mainImage = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onUnbindViewHolder", e)
        }
    }
}
