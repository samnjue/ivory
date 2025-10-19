package com.ivory.ivory;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import androidx.annotation.NonNull;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.modules.core.DeviceEventManagerModule;

public class AssistantModule extends ReactContextBaseJavaModule {

    private static final String MODULE_NAME = "AssistantModule";
    private static final String TAG = "AssistantModule";
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
    public void addListener(String eventName) {}

    @ReactMethod
    public void removeListeners(Integer count) {}

    @ReactMethod
    public void requestAssistPermission(Promise promise) {
        try {
            Activity currentActivity = getCurrentActivity();
            if (currentActivity == null) {
                Log.e(TAG, "No current activity available");
                promise.reject("NO_ACTIVITY", "Activity doesn't exist");
                return;
            }

            Intent intent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                RoleManager roleManager = (RoleManager) reactContext.getSystemService(Context.ROLE_SERVICE);
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_ASSISTANT)) {
                    Log.d(TAG, "Using RoleManager for assistant request");
                    intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT);
                } else {
                    Log.d(TAG, "RoleManager not available, falling back to voice input settings");
                    intent = new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS);
                }
            } else {
                Log.d(TAG, "Using voice input settings for pre-Q devices");
                intent = new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS);
            }
            Log.d(TAG, "Opening assistant settings");
            currentActivity.startActivityForResult(intent, REQUEST_CODE_ENABLE_ASSIST);
            promise.resolve(true);
        } catch (Exception e) {
            Log.e(TAG, "Error requesting assist permission: " + e.getMessage());
            promise.reject("ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void isAssistantEnabled(Promise promise) {
        try {
            boolean isEnabled;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                RoleManager roleManager = (RoleManager) reactContext.getSystemService(Context.ROLE_SERVICE);
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_ASSISTANT)) {
                    isEnabled = roleManager.isRoleHeld(RoleManager.ROLE_ASSISTANT);
                } else {
                    String assistComponent = Settings.Secure.getString(
                        reactContext.getContentResolver(),
                        "voice_interaction_service"
                    );
                    isEnabled = assistComponent != null &&
                            assistComponent.contains(reactContext.getPackageName());
                }
            } else {
                String assistComponent = Settings.Secure.getString(
                    reactContext.getContentResolver(),
                    "voice_interaction_service"
                );
                isEnabled = assistComponent != null &&
                        assistComponent.contains(reactContext.getPackageName());
            }
            Log.d(TAG, "isAssistantEnabled: " + isEnabled);
            promise.resolve(isEnabled);
        } catch (Exception e) {
            Log.e(TAG, "Error checking assistant status: " + e.getMessage());
            promise.reject("ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void showAssistOverlay() {
        Log.d(TAG, "Showing assist overlay via module");
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit("onAssistRequested", null);
    }

    @ReactMethod
    public void finishActivity() {
        Activity activity = getCurrentActivity();
        if (activity != null) {
            Log.d(TAG, "Finishing activity");
            activity.finish();
            activity.overridePendingTransition(0, 0);
        } else {
            Log.e(TAG, "No activity to finish");
        }
    }
}