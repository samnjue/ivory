package com.ivory.ivory;

import android.app.assist.AssistContent;
import android.app.assist.AssistStructure;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.voice.VoiceInteractionSession;
import android.util.Log;

public class IvoryAssistantSession extends VoiceInteractionSession {
    private static final String TAG = "IvoryAssistantSession";

    public IvoryAssistantSession(Context context) {
        super(context);
        
        // ⚡️ CRITICAL FIX: Disable the default VoiceInteractionSession window/UI.
        // We are launching our own MainActivity as the overlay, so we don't need the session's default UI.
        setUiEnabled(false); 
        Log.d(TAG, "IvoryAssistantSession initialized and default UI disabled.");
    }

    @Override
    public void onHandleAssist(Bundle data, AssistStructure structure, AssistContent content) {
        super.onHandleAssist(data, structure, content);
        Log.d(TAG, "Handling assist");

        // Launch the assist overlay (this intent will trigger the theme change in MainActivity.kt)
        Intent intent = new Intent(getContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                         Intent.FLAG_ACTIVITY_CLEAR_TOP |
                         Intent.FLAG_ACTIVITY_SINGLE_TOP |
                         Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.putExtra("showAssistOverlay", true);
        intent.setAction(Intent.ACTION_ASSIST);
        getContext().startActivity(intent);
        Log.d(TAG, "Started MainActivity for overlay");

        // Finish the session immediately, as the MainActivity is now responsible for the UI
        finish();
    }
}