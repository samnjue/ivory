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
    // NOTE: REQUEST_CODE_ENABLE_ASSIST is not strictly needed since we don't handle the result here, 
    // but keep it if you plan to use onActivityResult in MainActivity.
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

    // Required by React Native for event emitting, though often empty in the module itself
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
                // Android Q (API 29) and above: Use RoleManager for a dedicated prompt/screen
                RoleManager roleManager = (RoleManager) reactContext.getSystemService(Context.ROLE_SERVICE);
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_ASSISTANT)) {
                    Log.d(TAG, "Using RoleManager to request assistant role.");
                    // This creates the dedicated prompt/screen for setting the assistant
                    intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT);
                } else {
                    // Fallback for Q+ where RoleManager might not be fully functional or recognized
                    Log.d(TAG, "RoleManager unavailable or role not present. Falling back to ACTION_VOICE_INPUT_SETTINGS.");
                    intent = new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Android Oreo (API 26) to Pie (API 28): General voice settings
                Log.d(TAG, "Using ACTION_VOICE_INPUT_SETTINGS for O/P devices.");
                intent = new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS);
            } else {
                 // Pre-Oreo: General search settings often contains the assistant setting
                Log.d(TAG, "Using ACTION_ACCESSIBILITY_SETTINGS as a generic fallback for old devices.");
                intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            }

            // Ensure the activity resolves before starting
            if (intent.resolveActivity(currentActivity.getPackageManager()) != null) {
                Log.d(TAG, "Opening assistant settings via intent: " + intent.getAction());
                currentActivity.startActivityForResult(intent, REQUEST_CODE_ENABLE_ASSIST);
                promise.resolve(true);
            } else {
                Log.e(TAG, "Intent to open settings did not resolve.");
                promise.reject("INTENT_ERROR", "Could not find activity to handle settings intent.");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error requesting assist permission: " + e.getMessage(), e);
            promise.reject("ERROR", e.getMessage());
        }
    }
    
    // ... (isAssistantEnabled, showAssistOverlay, finishActivity methods remain the same)
    
    @ReactMethod
    public void isAssistantEnabled(Promise promise) {
        // ... (existing code for isAssistantEnabled)
        try {
            boolean isEnabled;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                RoleManager roleManager = (RoleManager) reactContext.getSystemService(Context.ROLE_SERVICE);
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_ASSISTANT)) {
                    // Modern check for API 29+
                    isEnabled = roleManager.isRoleHeld(RoleManager.ROLE_ASSISTANT);
                } else {
                    // Fallback check for API 29+
                    String assistComponent = Settings.Secure.getString(
                        reactContext.getContentResolver(),
                        "voice_interaction_service"
                    );
                    isEnabled = assistComponent != null &&
                            assistComponent.contains(reactContext.getPackageName());
                }
            } else {
                // Pre-API 29 check
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
            Log.e(TAG, "Error checking assistant status: " + e.getMessage(), e);
            promise.reject("ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void showAssistOverlay() {
        Log.d(TAG, "Showing assist overlay via module");
        // Emits the event to JS
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