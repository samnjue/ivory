package com.ivory.ivory;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import androidx.annotation.NonNull;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.modules.core.DeviceEventManagerModule;

public class AssistantModule extends ReactContextBaseJavaModule {

    private static final String MODULE_NAME = "AssistantModule";
    private static final int REQUEST_CODE_ENABLE_ASSIST = 1001;
    private final ReactApplicationContext reactContext;

    public AssistantModule(ReactApplicationContext context) {
        super(context);
        this.reactContext = context;
    }

    @NonNull
    @Override
    public String getName() {
        return MODULE_NAME;
    }

    @ReactMethod
    public void requestAssistPermission(Promise promise) {
        try {
            Activity currentActivity = getCurrentActivity();
            if (currentActivity == null) {
                promise.reject("NO_ACTIVITY", "Activity doesn't exist");
                return;
            }

            Intent intent;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                intent = new Intent(Settings.ACTION_ASSISTANT_SETTINGS);
            } else {
                intent = new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS);
            }

            currentActivity.startActivityForResult(intent, REQUEST_CODE_ENABLE_ASSIST);
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void isAssistantEnabled(Promise promise) {
        try {
            String assistComponent = Settings.Secure.getString(
                reactContext.getContentResolver(),
                "voice_interaction_service"
            );

            boolean isEnabled = assistComponent != null &&
                    assistComponent.contains(reactContext.getPackageName());
            promise.resolve(isEnabled);
        } catch (Exception e) {
            promise.reject("ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void showAssistOverlay() {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit("onAssistRequested", null);
    }
}