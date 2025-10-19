package com.ivory.ivory;

import android.content.Intent;
import android.os.Bundle;
import android.service.voice.VoiceInteractionService;

public class IvoryAssistantService extends VoiceInteractionService {

    @Override
    public void onReady() {
        super.onReady();
    }

    @Override
    public void onShutdown() {
        super.onShutdown();
    }

    @Override
    public void onLaunchVoiceAssistFromKeyguard() {
        super.onLaunchVoiceAssistFromKeyguard();
        launchAssist();
    }

    private void launchAssist() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP |
                        Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.putExtra("showAssistOverlay", true);
        intent.setAction(Intent.ACTION_ASSIST);
        startActivity(intent);
    }
}