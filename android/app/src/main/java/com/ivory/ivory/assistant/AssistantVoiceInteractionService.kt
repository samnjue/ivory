package com.ivory.ivory.assistant

import android.service.voice.VoiceInteractionService
import android.content.Intent

class AssistantVoiceInteractionService : VoiceInteractionService() {
    override fun onReady() {
        // nothing special – the overlay service will be launched from the Assist gesture
    }

    override fun onLaunchVoiceAssistFromTrigger() {
        // The system called us because the user long-pressed Home or swiped.
        AssistantOverlayService.start(this)
    }
}