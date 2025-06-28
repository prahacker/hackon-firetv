package com.example.firetv

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.util.Log

class VoiceCommandHelper(private val activity: Activity) {

    companion object {
        const val VOICE_REQUEST_CODE = 1001
    }

    fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something...")
        try {
            activity.startActivityForResult(intent, VOICE_REQUEST_CODE)
        } catch (e: Exception) {
            Log.e("VoiceCommandHelper", "Voice recognition not available", e)
        }
    }

    fun handleResult(requestCode: Int, resultCode: Int, data: Intent?, onCommandDetected: (String) -> Unit) {
        if (requestCode == VOICE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.get(0) ?: return
            Log.d("VoiceCommandHelper", "Heard: $spokenText")
            onCommandDetected(spokenText)
        }
    }
}
