package com.example.firetv

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.leanback.widget.Presenter
import com.facebook.shimmer.ShimmerFrameLayout

class ShimmerPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val shimmerView = LayoutInflater.from(parent.context)
            .inflate(R.layout.shimmer_card_layout, parent, false) as ShimmerFrameLayout

        shimmerView.startShimmer()
        return ViewHolder(shimmerView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val shimmerView = viewHolder.view as ShimmerFrameLayout
        shimmerView.startShimmer()
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val shimmerView = viewHolder.view as ShimmerFrameLayout
        shimmerView.stopShimmer()
    }
}
