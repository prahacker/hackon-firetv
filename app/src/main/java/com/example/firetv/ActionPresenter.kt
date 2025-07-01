package com.example.firetv

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.leanback.widget.Presenter
import com.example.firetv.MovieFragment.CreateSocialRoomAction
import com.example.firetv.MovieFragment.JoinSocialRoomAction

class ActionPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_action_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val titleView = viewHolder.view.findViewById<TextView>(R.id.action_title)
        val title = when (item) {
            is CreateSocialRoomAction -> "Create Social Room"
            is JoinSocialRoomAction -> "Join Social"
            else -> ""
        }
        titleView.text = title
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        // No special cleanup needed
    }
}