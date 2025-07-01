package com.example.firetv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.app.DetailsSupportFragmentBackgroundController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import jp.wasabeef.glide.transformations.BlurTransformation

class DetailsBackgroundTask(private val fragment: DetailsSupportFragment) {

    private val backgroundController = DetailsSupportFragmentBackgroundController(fragment)

    fun loadBackground(context: Context, url: String) {
        backgroundController.enableParallax()

        Glide.with(context)
            .asBitmap()
            .load(url)
            .transform(BlurTransformation(25, 3)) // Adjust blur radius and sampling as needed
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    backgroundController.coverBitmap = resource
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // Not used
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    Log.e("DetailsBackgroundTask", "Failed to load background image.")
                    val errorBitmap = errorDrawable?.let { drawable ->
                        if (drawable is BitmapDrawable) {
                            drawable.bitmap
                        } else {
                            val bitmap = Bitmap.createBitmap(
                                drawable.intrinsicWidth.coerceAtLeast(1),
                                drawable.intrinsicHeight.coerceAtLeast(1),
                                Bitmap.Config.ARGB_8888
                            )
                            val canvas = Canvas(bitmap)
                            drawable.setBounds(0, 0, canvas.width, canvas.height)
                            drawable.draw(canvas)
                            bitmap
                        }
                    }
                    backgroundController.coverBitmap = errorBitmap
                }



            })
    }
}