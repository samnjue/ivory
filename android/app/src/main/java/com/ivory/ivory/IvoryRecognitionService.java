package com.ivory.ivory;

import android.content.Intent;
import android.speech.RecognitionService;
import android.speech.SpeechRecognizer;

public class IvoryRecognitionService extends RecognitionService {
    @Override
    protected void onStartListening(Intent recognizerIntent, Callback callback) {
        callback.error(SpeechRecognizer.ERROR_NO_MATCH);
    }

    @Override
    protected void onCancel(Callback callback) {}

    @Override
    protected void onStopListening(Callback callback) {}
}