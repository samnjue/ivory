package com.ivory.ivory;

import android.content.Intent;
import android.os.RemoteException;
import android.speech.RecognitionService;
import android.speech.SpeechRecognizer;

public class IvoryRecognitionService extends RecognitionService {
    @Override
    protected void onStartListening(Intent recognizerIntent, Callback callback) {
        try {
            callback.error(SpeechRecognizer.ERROR_NO_MATCH);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCancel(Callback callback) {}

    @Override
    protected void onStopListening(Callback callback) {}
}