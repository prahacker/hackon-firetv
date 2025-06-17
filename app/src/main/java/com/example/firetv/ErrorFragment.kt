package com.example.firetv

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.leanback.app.ErrorSupportFragment

/**
 * A simple Error Fragment to show a styled error message with dismiss option.
 */
class ErrorFragment : ErrorSupportFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.app_name)
    }

    internal fun setErrorContent() {
        imageDrawable = ContextCompat.getDrawable(requireActivity(), androidx.leanback.R.drawable.lb_ic_sad_cloud)
        message = getString(R.string.error_fragment_message)
        setDefaultBackground(true)

        buttonText = getString(R.string.dismiss_error)
        buttonClickListener = View.OnClickListener {
            parentFragmentManager.beginTransaction().remove(this@ErrorFragment).commit()
        }
    }
}
