package com.ivory.ivory

import android.content.Intent
import android.os.RemoteException
import android.speech.RecognitionService
import android.speech.SpeechRecognizer

class IvoryRecognitionService : RecognitionService() {
    override fun onStartListening(recognizerIntent: Intent, callback: Callback) {
        try {
            // Immediately return no match
            callback.error(SpeechRecognizer.ERROR_NO_MATCH)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    override fun onCancel(callback: Callback) {}
    override fun onStopListening(callback: Callback) {}
}