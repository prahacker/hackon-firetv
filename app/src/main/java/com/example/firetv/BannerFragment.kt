package com.example.firetv

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter

class BannerFragment : RowsSupportFragment() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var rowAdapter: ArrayObjectAdapter
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val banners = MovieListProvider.getMovieList(requireContext())
            .sortedByDescending { it.rating }
            .take(5)

        rowAdapter = ArrayObjectAdapter(BannerPresenter())
        rowAdapter.addAll(0, banners)
        adapter = rowAdapter

        startAutoSlide()
    }

    private fun startAutoSlide() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (rowAdapter.size() > 0) {
                    val pos = currentIndex % rowAdapter.size()
                    selectedPosition = pos
                    currentIndex++
                }
                handler.postDelayed(this, 4000)
            }
        }, 2000)
    }
}
