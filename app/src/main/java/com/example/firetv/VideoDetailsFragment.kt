package com.example.firetv

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.app.DetailsSupportFragmentBackgroundController
import androidx.leanback.widget.*
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

class VideoDetailsFragment : DetailsSupportFragment() {

    private var mSelectedMovie: Movie? = null
    private lateinit var mDetailsBackground: DetailsSupportFragmentBackgroundController
    private lateinit var mPresenterSelector: ClassPresenterSelector
    private lateinit var mAdapter: ArrayObjectAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate DetailsFragment")

        mDetailsBackground = DetailsSupportFragmentBackgroundController(this)

        mSelectedMovie = requireActivity().intent.getSerializableExtra(DetailsActivity.MOVIE) as? Movie

        if (mSelectedMovie != null) {
            mPresenterSelector = ClassPresenterSelector()
            mAdapter = ArrayObjectAdapter(mPresenterSelector)
            setupDetailsOverviewRow()
            setupDetailsOverviewRowPresenter()
            setupRelatedMovieListRow()
            adapter = mAdapter
            initializeBackground(mSelectedMovie)
            onItemViewClickedListener = ItemViewClickedListener()
        } else {
            startActivity(Intent(requireActivity(), MainActivity::class.java))
        }
    }

    private fun initializeBackground(movie: Movie?) {
        mDetailsBackground.enableParallax()
        Glide.with(requireActivity())
            .asBitmap()
            .centerCrop()
            .error(R.drawable.default_background)
            .load(movie?.backdrop_image)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    mDetailsBackground.coverBitmap = resource
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    private fun setupDetailsOverviewRow() {
        val row = DetailsOverviewRow(mSelectedMovie)
        row.imageDrawable = ContextCompat.getDrawable(requireActivity(), R.drawable.default_background)

        val width = convertDpToPixel(requireActivity(), DETAIL_THUMB_WIDTH)
        val height = convertDpToPixel(requireActivity(), DETAIL_THUMB_HEIGHT)

        Glide.with(requireActivity())
            .load(mSelectedMovie?.poster_image)
            .centerCrop()
            .error(R.drawable.default_background)
            .into(object : CustomTarget<Drawable>(width, height) {
                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable>?
                ) {
                    row.imageDrawable = resource
                    mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size())
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })

        val actionAdapter = ArrayObjectAdapter().apply {
            add(Action(ACTION_WATCH_TRAILER, getString(R.string.watch_trailer_1), getString(R.string.watch_trailer_2)))
            add(Action(ACTION_RENT, getString(R.string.rent_1), getString(R.string.rent_2)))
            add(Action(ACTION_BUY, getString(R.string.buy_1), getString(R.string.buy_2)))
        }

        row.actionsAdapter = actionAdapter
        mAdapter.add(row)
    }

    private fun setupDetailsOverviewRowPresenter() {
        val detailsPresenter = FullWidthDetailsOverviewRowPresenter(DetailsDescriptionPresenter()).apply {
            backgroundColor = ContextCompat.getColor(requireContext(), R.color.selected_background)

            val sharedElementHelper = FullWidthDetailsOverviewSharedElementHelper().apply {
                setSharedElementEnterTransition(requireActivity(), DetailsActivity.SHARED_ELEMENT_NAME)
            }
            setListener(sharedElementHelper)
            isParticipatingEntranceTransition = true

            onActionClickedListener = OnActionClickedListener { action ->
                when (action.id) {
                    ACTION_WATCH_TRAILER -> {
                        val platforms = mSelectedMovie?.available_platforms ?: listOf()

                        if (platforms.size > 1) {
                            val dialog = DeeplinkChoiceDialog(requireContext(), platforms) { selected ->
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse(selected)
                                }
                                startActivity(intent)
                            }
                            dialog.show()
                        } else {
                            val deeplink = mSelectedMovie?.deeplink ?: return@OnActionClickedListener
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse(deeplink)
                            }
                            startActivity(intent)
                        }
                    }
                    else -> Toast.makeText(requireActivity(), action.toString(), Toast.LENGTH_SHORT).show()
                }
            }

        }

        mPresenterSelector.addClassPresenter(DetailsOverviewRow::class.java, detailsPresenter)
    }

    private fun setupRelatedMovieListRow() {
        val relatedMovies = MovieListProvider.getMovieList(requireContext()).toMutableList()
        relatedMovies.shuffle()

        val listRowAdapter = ArrayObjectAdapter(CardPresenter()).apply {
            for (i in 0 until NUM_COLS) {
                add(relatedMovies[i % relatedMovies.size])
            }
        }

        val header = HeaderItem(0, getString(R.string.related_movies))
        mAdapter.add(ListRow(header, listRowAdapter))
        mPresenterSelector.addClassPresenter(ListRow::class.java, ListRowPresenter())
    }

    private fun convertDpToPixel(context: Context, dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }

    private inner class ItemViewClickedListener : OnItemViewClickedListener {
        override fun onItemClicked(
            itemViewHolder: Presenter.ViewHolder?,
            item: Any?,
            rowViewHolder: RowPresenter.ViewHolder,
            row: Row
        ) {
            if (item is Movie) {
                Log.d(TAG, "Clicked Movie: $item")
                val platforms = item.available_platforms
                if (platforms.size > 1) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Choose a platform")
                        .setItems(platforms.toTypedArray()) { _, which ->
                            DeepLinkHelper.launchUrl(requireContext(), platforms[which])
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                } else if (platforms.isNotEmpty()) {
                    DeepLinkHelper.launchUrl(requireContext(), platforms.first())
                } else {
                    Toast.makeText(requireContext(), "No available platform", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        private const val TAG = "VideoDetailsFragment"

        private const val ACTION_WATCH_TRAILER = 1L
        private const val ACTION_RENT = 2L
        private const val ACTION_BUY = 3L

        private const val DETAIL_THUMB_WIDTH = 274
        private const val DETAIL_THUMB_HEIGHT = 274

        private const val NUM_COLS = 10
    }
}
