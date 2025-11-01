package com.ivory.ivory

import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService

class IvoryAssistantSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?) = IvoryAssistantSession(this)
}

class IvoryAssistantSession(context: android.content.Context) : VoiceInteractionSession(context) {
    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        SystemOverlayService.show(context)
        finish()
    }
}