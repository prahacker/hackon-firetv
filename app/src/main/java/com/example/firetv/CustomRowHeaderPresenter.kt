package com.example.firetv

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.RowHeaderPresenter

class CustomRowHeaderPresenter : RowHeaderPresenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.custom_header_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: Presenter.ViewHolder?, item: Any?) {
        // The 'item' is the entire ListRow. We get the header from it.
        val headerItem = (item as? ListRow)?.headerItem
        val titleView = viewHolder?.view?.findViewById<TextView>(R.id.header_label)

        if (titleView != null) {
            // If the headerItem is valid, set its name. Otherwise, set the text to empty
            // to prevent old text from showing in the recycled view.
            titleView.text = headerItem?.name ?: ""
        }
    }

    override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder?) {
        // No special cleanup is needed here, but it's good practice to have the method.
    }
}