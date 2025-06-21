package com.example.firetv

import android.graphics.Color
import android.view.ViewGroup
import android.widget.TextView
import androidx.leanback.widget.BaseCardView
import androidx.leanback.widget.Presenter
import com.example.firetv.MovieFragment.CreateSocialRoomAction
import com.example.firetv.MovieFragment.JoinSocialRoomAction

class ActionPresenter : Presenter() {

    class SocialRoomCardView(context: android.content.Context) : BaseCardView(context) {
        private val titleText: TextView

        init {
            isFocusable = true
            isFocusableInTouchMode = true
            setBackgroundColor(Color.parseColor("#24272C"))

            titleText = TextView(context).apply {
                textSize = 14f
                setTextColor(Color.WHITE)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setPadding(16, 20, 16, 20)
                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                // Allow text wrapping
                maxLines = 2
                setSingleLine(false)
            }

            addView(titleText)
            // Increase height to accommodate wrapped text
            layoutParams = ViewGroup.LayoutParams(313, 200)
        }

        fun setTitle(title: String) {
            titleText.text = title
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val cardView = SocialRoomCardView(parent.context)
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val cardView = viewHolder.view as SocialRoomCardView

        when (item) {
            is CreateSocialRoomAction -> {
                cardView.setTitle("Create Social Room")
            }
            is JoinSocialRoomAction -> {
                cardView.setTitle("Join Social Room")
            }
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {}
}
