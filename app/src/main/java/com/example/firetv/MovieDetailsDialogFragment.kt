package com.example.firetv

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide

class MovieDetailsDialogFragment(private val movie: Movie) : DialogFragment() {

    interface DeeplinkListener {
        fun onPlayClicked(movie: Movie)
    }

    private var deeplinkListener: DeeplinkListener? = null

    // Dismiss callback to reschedule trailer playback
    var onDialogDismiss: (() -> Unit)? = null

    fun setDeeplinkListener(listener: DeeplinkListener): MovieDetailsDialogFragment {
        this.deeplinkListener = listener
        return this
    }

    private fun platformToUrl(platform: String, homepage: String?): String? {
        return when {
            platform.contains("Netflix", ignoreCase = true) -> homepage ?: "https://www.netflix.com/"
            platform.contains("Amazon Prime", ignoreCase = true) -> homepage ?: "https://www.primevideo.com/"
            platform.contains("YouTube", ignoreCase = true) -> "https://www.youtube.com/"
            else -> homepage
        }
    }

    private fun launchPlatformIntent(url: String?, platform: String?) {
        if (!url.isNullOrBlank()) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                when {
                    platform?.contains("Netflix", ignoreCase = true) == true -> setPackage("com.netflix.mediaclient")
                    platform?.contains("Prime", ignoreCase = true) == true -> setPackage("com.amazon.cloud9")
                }
            }

            try {
                startActivity(intent)
            } catch (e: Exception) {
                Log.w("MovieDetailsDialog", "App not found, trying browser: ${e.message}")
                try {
                    val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(fallbackIntent)
                } catch (fallbackError: Exception) {
                    Log.e("MovieDetailsDialog", "Fallback failed: ${fallbackError.message}")
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.custom_details_overview, null)

        val banner = view.findViewById<ImageView>(R.id.details_banner)
        val title = view.findViewById<TextView>(R.id.details_title)
        val ratingBar = view.findViewById<RatingBar>(R.id.details_rating_bar)
        val subtitle = view.findViewById<TextView>(R.id.details_subtitle)
        val body = view.findViewById<TextView>(R.id.details_body)
        val playMovie = view.findViewById<Button>(R.id.play_movie_button)
        val createRoom = view.findViewById<Button>(R.id.create_room_button)

        Glide.with(requireContext())
            .load(movie.backdrop_image)
            .centerCrop()
            .into(banner)

        title.text = movie.title.ifBlank { "Untitled" }
        subtitle.text = movie.available_platforms.firstOrNull() ?: "Unknown Platform"
        ratingBar.rating = (movie.rating / 2.0).toFloat().coerceIn(0f, 5f)
        ratingBar.visibility = if (movie.rating > 0) View.VISIBLE else View.GONE
        body.text = movie.description.ifBlank { "No description available." }

        playMovie.setOnClickListener {
            // Log the click event immediately
            FirebaseLogger.logInteraction(
                actionType = "click",
                screenContext = "Detail_Page",
                focusedItem = movie,
                timeSinceLastAction = SequenceTracker.timeSinceLastAction(),
                consecutiveActionCount = SequenceTracker.increment("click"),
                clickType = "play"
            )

            try {
                val platforms = movie.deeplinks?.filterNotNull()?.takeIf { it.isNotEmpty() }

                if (platforms == null) {
                    Toast.makeText(requireContext(), "No deeplinks available", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                deeplinkListener?.onPlayClicked(movie)

                AlertDialog.Builder(requireContext())
                    .setTitle("Watch on")
                    .setItems(platforms.toTypedArray()) { _, idx ->
                        val platform = platforms.getOrNull(idx)
                        val url = platformToUrl(platform ?: "", movie.homepage)

                        launchPlatformIntent(url, platform)
                        dismiss()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()

            } catch (e: Exception) {
                Log.e("MovieDetailsDialog", "Error showing deeplink dialog: ${e.message}", e)
            }
        }

        createRoom.setOnClickListener {
            Toast.makeText(requireContext(), "Room creation coming soon!", Toast.LENGTH_SHORT).show()
        }

        return Dialog(requireContext()).apply {
            setContentView(view)
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (resources.displayMetrics.heightPixels * 0.75).toInt()
            )
            window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.BLACK))
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) {
                // This is the key change to log the back button press
                FirebaseLogger.logInteraction(
                    actionType = "back",
                    screenContext = "Detail_Page",
                    focusedItem = movie,
                    timeSinceLastAction = SequenceTracker.timeSinceLastAction(),
                    consecutiveActionCount = SequenceTracker.increment("back")
                )
                dismiss()
                requireActivity().onBackPressedDispatcher.onBackPressed()
                true
            } else false
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDialogDismiss?.invoke()
    }
}