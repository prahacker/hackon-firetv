package com.example.firetv

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.*
import com.bumptech.glide.Glide

class MovieFragment : BrowseSupportFragment() {

    private var hoverStartTime = 0L
    private var lastHoveredMovieTitle: String? = null

    private var bannerImageView: ImageView? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        title = "Welcome to FireTV"
        bannerImageView = requireActivity().findViewById(R.id.banner)
        setupRows()
        setupEventListeners()
    }

    private fun setupRows() {
        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        val cardPresenter = CardPresenter()
        val movieList = MovieList.getMovies(requireContext())

        val rowAdapter = ArrayObjectAdapter(cardPresenter).apply {
            movieList.forEach { add(it) }
        }

        rowsAdapter.add(ListRow(HeaderItem(0, "Popular"), rowAdapter))
        adapter = rowsAdapter
    }

    private fun setupEventListeners() {
        setOnItemViewSelectedListener { _, item, _, _ ->
            val movie = item as? Movie ?: return@setOnItemViewSelectedListener

            // Log hover time for last movie
            lastHoveredMovieTitle?.let {
                FirebaseLogger.logHoverDuration(it, hoverStartTime, System.currentTimeMillis())
            }

            hoverStartTime = System.currentTimeMillis()
            lastHoveredMovieTitle = movie.title

            // Log Firebase hover
            FirebaseLogger.logHover(movie)

            // Show banner
            bannerImageView?.let {
                it.visibility = View.VISIBLE
                Glide.with(requireContext())
                    .load(movie.backdrop_image)
                    .centerCrop()
                    .into(it)
            }
        }

        setOnItemViewClickedListener { _, item, _, _ ->
            val movie = item as? Movie ?: return@setOnItemViewClickedListener
            FirebaseLogger.logClick(movie)

            if (movie.deeplinks.size > 1) {
                // Show dialog with deeplink options
                AlertDialog.Builder(requireContext())
                    .setTitle("Watch on")
                    .setItems(movie.deeplinks.toTypedArray()) { _, which ->
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse(movie.homepage)
                        }
                        startActivity(intent)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                // Open homepage directly
                DeepLinkHelper.launchUrl(requireContext(), movie.homepage)
            }
        }
    }
}
