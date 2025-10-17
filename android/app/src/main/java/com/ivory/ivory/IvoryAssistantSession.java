package com.ivory.ivory;

import android.app.assist.AssistContent;
import android.app.assist.AssistStructure;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.voice.VoiceInteractionSession;

public class IvoryAssistantSession extends VoiceInteractionSession {

    public IvoryAssistantSession(Context context) {
        super(context);
    }

    @Override
    public void onHandleAssist(Bundle data, AssistStructure structure, AssistContent content) {
        super.onHandleAssist(data, structure, content);

        // Launch the assist overlay
        Intent intent = new Intent(getContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("showAssistOverlay", true);
        getContext().startActivity(intent);

        // Finish the session
        finish();
    }
}