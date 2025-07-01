package com.example.firetv
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.DialogFragment

class PlatformPickerDialogFragment(
    private val platforms: List<String>,
    private val homepage: String?,
    private val onPlatformSelected: (String) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_platform_picker, null)

        val container = view.findViewById<LinearLayout>(R.id.platform_container)
        val cancelBtn = view.findViewById<Button>(R.id.cancel_button)

        platforms.forEach { platform ->
            val imageView = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    120
                ).apply {
                    setMargins(0, 12, 0, 12)
                }

                scaleType = ImageView.ScaleType.FIT_CENTER
                setBackgroundColor(Color.parseColor("#222"))

                // Set logos dynamically
                val normalized = platform.lowercase()

                when {
                    normalized.contains("netflix") -> setImageResource(R.drawable.ic_netflix_logo)
                    normalized.contains("prime") || normalized.contains("amazon") -> setImageResource(R.drawable.ic_prime_video_logo)
                    else -> setImageResource(R.drawable.generic_platform_logo)
                }

                setOnClickListener {
                    onPlatformSelected(platform)
                    dismiss()
                }
            }

            container.addView(imageView)
        }

        cancelBtn.setOnClickListener { dismiss() }

        return Dialog(requireContext()).apply {
            setContentView(view)
            window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.8).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            window?.setBackgroundDrawable(ColorDrawable(Color.DKGRAY))
        }
    }
}
