package com.example.firetv

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.*
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class MovieDetailsDialogFragment(
    private val movie: Movie,
    private val allMovies: List<Movie>,
    private val recommendedMovies: List<Movie>, // Pass in recommended movies
    private val reasoning: String?
) : DialogFragment() {

    private val reasoningHandler = Handler(Looper.getMainLooper())
    private var reasoningRunnable: Runnable? = null

    interface DeeplinkListener {
        fun onPlayClicked(movie: Movie)
    }

    private var deeplinkListener: DeeplinkListener? = null
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
        val subtitle = view.findViewById<TextView>(R.id.details_subtitle)
        val body = view.findViewById<TextView>(R.id.details_body)
        val playMovie = view.findViewById<Button>(R.id.play_movie_button)
        val createRoom = view.findViewById<Button>(R.id.create_room_button)
        val reasoningTextView = view.findViewById<TextView>(R.id.reasoning_popup_text)

        Glide.with(requireContext())
            .load(movie.backdrop_image)
            .centerCrop()
            .into(banner)

        title.text = movie.title.ifBlank { "Untitled" }

        val uniqueSimplifiedPlatforms = movie.available_platforms
            .map { platform -> PlatformHelper.simplifyPlatformName(platform) }
            .distinct()
            .take(3)

        val platformsBuilder = SpannableStringBuilder()
        uniqueSimplifiedPlatforms.forEachIndexed { index, simplifiedName ->
            val originalPlatform = movie.available_platforms.first { PlatformHelper.simplifyPlatformName(it) == simplifiedName }
            platformsBuilder.append(PlatformHelper.getSpannableStringForPlatform(requireContext(), originalPlatform))
            if (index < uniqueSimplifiedPlatforms.size - 1) {
                platformsBuilder.append("  •  ")
            }
        }
        subtitle.text = if (platformsBuilder.isNotEmpty()) platformsBuilder else "Unknown Platform"

        body.text = movie.description.ifBlank { "No description available." }

        if (!reasoning.isNullOrBlank()) {
            reasoningTextView.text = reasoning
            reasoningTextView.visibility = View.VISIBLE
            reasoningRunnable = Runnable {
                val fadeOut = AlphaAnimation(1f, 0f)
                fadeOut.duration = 500
                fadeOut.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation) {}
                    override fun onAnimationEnd(animation: Animation) {
                        reasoningTextView.visibility = View.GONE
                    }
                    override fun onAnimationRepeat(animation: Animation) {}
                })
                reasoningTextView.startAnimation(fadeOut)
            }
            reasoningHandler.postDelayed(reasoningRunnable!!, 7000)
        }


        playMovie.setOnClickListener {
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

        val similarList = view.findViewById<RecyclerView>(R.id.similar_content_list)
        val similarMovies = allMovies.filter {
            it != movie && it.genres.any { g -> g in movie.genres }
        }.take(12)
        Log.d("MovieDetailsDialog", "Similar movies: ${similarMovies.size}")

        val similarAdapter = SimilarMovieAdapter(similarMovies) { selected ->
            dismiss()
            view?.postDelayed({
                MovieDetailsDialogFragment(selected, allMovies, emptyList(), null) // Pass empty list and null
                    .show(requireActivity().supportFragmentManager, "MovieDetailsDialog")
            }, 250)
        }

        similarList.adapter = similarAdapter
        similarList.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        val recommendationsList = view.findViewById<RecyclerView>(R.id.recommendations_list)
        val recommendationsAdapter = SimilarMovieAdapter(recommendedMovies) { selected ->
            dismiss()
            view?.postDelayed({
                MovieDetailsDialogFragment(selected, allMovies, emptyList(), null) // Pass empty list and null
                    .show(requireActivity().supportFragmentManager, "MovieDetailsDialog")
            }, 250)
        }
        recommendationsList.adapter = recommendationsAdapter
        recommendationsList.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

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
        reasoningRunnable?.let { reasoningHandler.removeCallbacks(it) }
        onDialogDismiss?.invoke()
    }
}