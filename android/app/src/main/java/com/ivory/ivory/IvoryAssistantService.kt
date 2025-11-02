package com.ivory.ivory

import android.service.voice.VoiceInteractionService

class IvoryAssistantService : VoiceInteractionService() {
    override fun onReady() {
        super.onReady()
    }

    override fun onLaunchVoiceAssistFromKeyguard() {
        launchOverlay()
    }

    private fun launchOverlay() {
        SystemOverlayService.show(this)
    }
}