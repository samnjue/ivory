package com.ivory.ivory;

import android.content.Intent;
import android.os.Bundle;
import android.service.voice.VoiceInteractionService;
import android.util.Log;

public class IvoryAssistantService extends VoiceInteractionService {
    private static final String TAG = "IvoryAssistantService";

    @Override
    public void onReady() {
        super.onReady();
        Log.d(TAG, "Service ready");
    }

    @Override
    public void onShutdown() {
        super.onShutdown();
        Log.d(TAG, "Service shutdown");
    }

    @Override
    public void onLaunchVoiceAssistFromKeyguard() {
        super.onLaunchVoiceAssistFromKeyguard();
        Log.d(TAG, "Launching from keyguard");
        launchAssist();
    }

    private void launchAssist() {
        Log.d(TAG, "Launching assist");
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP |
                        Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.putExtra("showAssistOverlay", true);
        intent.setAction(Intent.ACTION_ASSIST);
        startActivity(intent);
        Log.d(TAG, "Started MainActivity");
    }
}