package com.example.firetv

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.fragment.app.FragmentActivity

class MainActivity : FragmentActivity() {

    private lateinit var voiceHelper: VoiceCommandHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        voiceHelper = VoiceCommandHelper(this)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.browse_fragment, MovieFragment())
                .commitNow()
        }
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    SequenceTracker.lastDpadKeyCode = event.keyCode
                }

                KeyEvent.KEYCODE_BACK -> {
                    FirebaseLogger.logInteraction(
                        actionType = "back",
                        screenContext = "Home",
                        focusedItem = FocusTracker.getLastFocusedItem(),
                        timeSinceLastAction = SequenceTracker.timeSinceLastAction(),
                        consecutiveActionCount = SequenceTracker.increment("back")
                    )
                }

                KeyEvent.KEYCODE_VOICE_ASSIST -> {
                    voiceHelper.startVoiceRecognition()
                }

            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        voiceHelper.handleResult(requestCode, resultCode, data) { spokenText ->
            if (spokenText.contains("surprise me", ignoreCase = true)) {
                val fragment = supportFragmentManager.findFragmentById(R.id.browse_fragment)
                if (fragment is MovieFragment) {
                    fragment.surpriseMe()
                }
            }
        }
    }
}
