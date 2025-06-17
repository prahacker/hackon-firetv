package com.example.firetv

import android.app.AlertDialog
import android.content.Context

class DeeplinkChoiceDialog(
    context: Context,
    private val links: List<String>,
    private val onLinkSelected: (String) -> Unit
) : AlertDialog(context) {

    override fun show() {
        val items = links.toTypedArray()
        AlertDialog.Builder(context)
            .setTitle("Choose a platform")
            .setItems(items) { _, which ->
                onLinkSelected(items[which])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}