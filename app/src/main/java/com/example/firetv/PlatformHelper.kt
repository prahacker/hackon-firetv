package com.example.firetv

import android.content.Context
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import androidx.core.content.ContextCompat

object PlatformHelper {

    fun simplifyPlatformName(platform: String): String {
        return when {
            platform.contains("Netflix", ignoreCase = true) -> "Netflix"
            platform.contains("Prime", ignoreCase = true) || platform.contains("Amazon", ignoreCase = true) -> "Prime Video"
            platform.contains("Disney", ignoreCase = true) -> "Disney+"
            else -> platform
        }
    }

    fun getSpannableStringForBanner(context: Context, platform: String): SpannableStringBuilder {
        val simplifiedName = simplifyPlatformName(platform)
        val builder = SpannableStringBuilder()

        val drawableId = when {
            simplifiedName.equals("Netflix", ignoreCase = true) -> R.drawable.ic_netflix_logo
            simplifiedName.equals("Prime Video", ignoreCase = true) -> R.drawable.ic_prime_video_logo
            simplifiedName.equals("Disney+", ignoreCase = true) -> R.drawable.ic_disney_plus_logo
            else -> null
        }

        if (drawableId != null) {
            val drawable = ContextCompat.getDrawable(context, drawableId)?.apply {
                val targetHeight = (20 * context.resources.displayMetrics.density).toInt()
                val aspectRatio = this.intrinsicWidth.toFloat() / this.intrinsicHeight.toFloat()
                val targetWidth = (targetHeight * aspectRatio).toInt()
                setBounds(0, 0, targetWidth, targetHeight)
            }

            if (drawable != null) {
                builder.append(" ")
                builder.setSpan(ImageSpan(drawable, ImageSpan.ALIGN_BASELINE), builder.length - 1, builder.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                builder.append("  ")
            }
        }

        builder.append(simplifiedName)
        return builder
    }

    fun getSpannableStringForPlatform(context: Context, platform: String): SpannableStringBuilder {
        val simplifiedName = simplifyPlatformName(platform)
        val builder = SpannableStringBuilder()

        val drawableId = when {
            simplifiedName.equals("Netflix", ignoreCase = true) -> R.drawable.ic_netflix_logo
            simplifiedName.equals("Prime Video", ignoreCase = true) -> R.drawable.ic_prime_video_logo
            simplifiedName.equals("Disney+", ignoreCase = true) -> R.drawable.ic_disney_plus_logo
            else -> R.drawable.ic_blue_tick
        }

        val drawable = ContextCompat.getDrawable(context, drawableId)?.apply {
            val targetHeight = (20 * context.resources.displayMetrics.density).toInt()
            val aspectRatio = this.intrinsicWidth.toFloat() / this.intrinsicHeight.toFloat()
            val targetWidth = (targetHeight * aspectRatio).toInt()
            setBounds(0, 0, targetWidth, targetHeight)
        }

        if (drawable != null) {
            builder.append(" ")
            builder.setSpan(ImageSpan(drawable, ImageSpan.ALIGN_BASELINE), builder.length - 1, builder.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
            builder.append("  ")
        }

        builder.append(simplifiedName)
        return builder
    }
}