package com.ivory.ivory;

import android.content.Intent;

public class AssistantService extends Service {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Setup notification for foreground service if needed

        // Listen for Assist triggers (via VoiceInteractionService?)
        // Or custom broadcasts for debugging

        return START_STICKY;
    }
    // Helper to show overlay
    public void showOverlay() {
        // ...see PillOverlayView
    }
    public void hideOverlay() {
        // ...remove overlay, animate down
    }
}
