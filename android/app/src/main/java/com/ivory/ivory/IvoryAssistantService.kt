package com.ivory.ivory

import android.content.Intent
import android.service.voice.VoiceInteractionService

class IvoryAssistantService : VoiceInteractionService() {
    override fun onReady() {
        super.onReady()
        // Ready to receive ASSIST intent
    }

    override fun onLaunchVoiceAssistFromKeyguard() {
        launchOverlay()
    }

    private fun launchOverlay() {
        SystemOverlayService.show(this)
    }
}