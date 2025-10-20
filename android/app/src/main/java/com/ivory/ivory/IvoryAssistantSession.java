package com.ivory.ivory;

import android.app.assist.AssistContent;
import android.app.assist.AssistStructure;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.service.voice.VoiceInteractionSession;
import android.util.Log;
import android.view.WindowManager;

public class IvoryAssistantSession extends VoiceInteractionSession {
    private static final String TAG = "IvoryAssistantSession";

    public IvoryAssistantSession(Context context) {
        super(context);
        
        // Disable the default VoiceInteractionSession UI
        setUiEnabled(false);
        Log.d(TAG, "IvoryAssistantSession initialized and default UI disabled.");
    }

    @Override
    public void onShow(Bundle args, int showFlags) {
        super.onShow(args, showFlags);
        Log.d(TAG, "onShow called with flags: " + showFlags);
        
        // Launch MainActivity as an overlay
        launchAssistOverlay();
    }

    @Override
    public void onHandleAssist(Bundle data, AssistStructure structure, AssistContent content) {
        super.onHandleAssist(data, structure, content);
        Log.d(TAG, "Handling assist");
        
        // Launch MainActivity as an overlay
        launchAssistOverlay();
    }
    
    private void launchAssistOverlay() {
        Intent intent = new Intent(getContext(), AssistOverlayActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                         Intent.FLAG_ACTIVITY_NO_ANIMATION |
                         Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        intent.setAction(Intent.ACTION_ASSIST);
        getContext().startActivity(intent);
        Log.d(TAG, "Started AssistOverlayActivity");

        // Finish the session immediately as the overlay activity takes over
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Log.d(TAG, "Finishing session");
            finish();
        }, 50);
    }
    
    @Override
    public void onHide() {
        super.onHide();
        Log.d(TAG, "Session hidden");
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Session destroyed");
    }
}