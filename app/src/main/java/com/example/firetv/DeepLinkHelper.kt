package com.example.firetv

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object DeepLinkHelper {

    fun launchUrl(context: Context, url: String?) {
        if (url.isNullOrBlank()) {
            Toast.makeText(context, "No link available", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to open link", Toast.LENGTH_SHORT).show()
        }
    }
}
