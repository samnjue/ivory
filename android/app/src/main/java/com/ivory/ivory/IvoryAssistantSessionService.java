package com.ivory.ivory;

import android.content.Intent;
import android.os.Bundle;
import android.service.voice.VoiceInteractionService;

public class IvoryAssistantSessionService extends VoiceInteractionService {
    
    @Override
    public void onReady() {
        super.onReady();
    }

    @Override
    public VoiceInteractionSession onNewSession(Bundle args) {
        return new IvoryAssistantSession(this);
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
                       Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("showAssistOverlay", true);
        startActivity(intent);
    }
}