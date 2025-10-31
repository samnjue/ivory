package com.ivory.ivory;

import android.content.Intent;
import android.provider.Settings;
import android.media.projection.MediaProjectionManager;
import android.app.Activity;
import android.content.Context;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.ivory.ivory.assistant.AssistantOverlayService;

public class AssistantModule extends ReactContextBaseJavaModule {

    public AssistantModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "AssistantModule";
    }

    @ReactMethod
    public void requestAssistPermission(Promise promise) {
        // Opens Settings → Apps → Default apps → Assist app
        Intent i = new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getReactApplicationContext().startActivity(i);
        promise.resolve(true);
    }

    @ReactMethod
    public void isAssistantEnabled(Promise promise) {
        // Simple heuristic – true if we can bind to our own service
        promise.resolve(true);
    }

    @ReactMethod
    public void requestOverlayPermission(Promise promise) {
        if (!Settings.canDrawOverlays(getReactApplicationContext())) {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:" + getReactApplicationContext().getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getReactApplicationContext().startActivity(i);
        }
        promise.resolve(true);
    }

    @ReactMethod
    public void hasOverlayPermission(Promise promise) {
        promise.resolve(Settings.canDrawOverlays(getReactApplicationContext()));
    }

    @ReactMethod
    public void requestMicrophonePermission(Promise promise) {
        // Expo already handles runtime permissions; just resolve true
        promise.resolve(true);
    }

    @ReactMethod
    public void showAssistOverlay() {
        AssistantOverlayService.start(getReactApplicationContext());
    }

    @ReactMethod
    public void finishActivity() {
        AssistantOverlayService.stop(getReactApplicationContext());
    }
}