package com.example.firetv

import android.os.Bundle
import android.widget.Toast
import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.widget.*

class VideoDetailsFragment : DetailsSupportFragment() {

    private var selectedMovie: Movie? = null
    private lateinit var detailsBackgroundTask: DetailsBackgroundTask

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedMovie = requireActivity().intent.getSerializableExtra(DetailsActivity.MOVIE) as? Movie

        if (selectedMovie == null) {
            Toast.makeText(requireContext(), "Movie data not found.", Toast.LENGTH_LONG).show()
            activity?.finish()
            return
        }

        detailsBackgroundTask = DetailsBackgroundTask(this)
        setupUI()
    }

    private fun setupUI() {
        val movie = selectedMovie!!
        val detailsPresenter = FullWidthDetailsOverviewRowPresenter(CustomDetailsOverviewPresenter())

        // 👉 Set the ActionClickedListener here!
        detailsPresenter.onActionClickedListener = OnActionClickedListener { action ->
            Toast.makeText(requireContext(), "Action clicked: ${action.id}", Toast.LENGTH_SHORT).show()
        }

        val detailsAdapter = ArrayObjectAdapter(detailsPresenter)
        val detailsRow = DetailsOverviewRow(movie)
        detailsAdapter.add(detailsRow)
        adapter = detailsAdapter

        // Load the blurred background
        detailsBackgroundTask.loadBackground(requireContext(), movie.backdrop_image)
    }
}
