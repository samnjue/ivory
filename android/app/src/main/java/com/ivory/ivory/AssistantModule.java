package com.ivory.ivory;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
                    Log.d(TAG, "Using RoleManager to request assistant role.");
                    intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT);
                } else {
                    Log.d(TAG, "RoleManager unavailable. Falling back to ACTION_VOICE_INPUT_SETTINGS.");
                    intent = new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d(TAG, "Using ACTION_VOICE_INPUT_SETTINGS for O/P devices.");
                intent = new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS);
            } else {
                Log.d(TAG, "Using ACTION_ACCESSIBILITY_SETTINGS as fallback for old devices.");
                intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            }

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
    
    @ReactMethod
    public void requestOverlayPermission(Promise promise) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(reactContext)) {
                    Log.d(TAG, "Overlay permission already granted");
                    promise.resolve(true);
                    return;
                }
                
                Activity currentActivity = getCurrentActivity();
                if (currentActivity == null) {
                    Log.e(TAG, "No current activity available");
                    promise.reject("NO_ACTIVITY", "Activity doesn't exist");
                    return;
                }
                
                Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + reactContext.getPackageName())
                );
                
                currentActivity.startActivity(intent);
                Log.d(TAG, "Requested overlay permission");
                promise.resolve(true);
            } else {
                // Permission not needed on older versions
                Log.d(TAG, "Overlay permission not required on this Android version");
                promise.resolve(true);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error requesting overlay permission: " + e.getMessage(), e);
            promise.reject("ERROR", e.getMessage());
        }
    }
    
    @ReactMethod
    public void hasOverlayPermission(Promise promise) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                boolean hasPermission = Settings.canDrawOverlays(reactContext);
                Log.d(TAG, "Overlay permission status: " + hasPermission);
                promise.resolve(hasPermission);
            } else {
                // Always granted on older versions
                promise.resolve(true);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking overlay permission: " + e.getMessage(), e);
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
            Log.e(TAG, "Error checking assistant status: " + e.getMessage(), e);
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
        Log.d(TAG, "finishActivity called");
        Intent intent = new Intent(getReactApplicationContext(), SystemOverlayManager.class);
        getReactApplicationContext().stopService(intent);
        // Finish any running AssistOverlayActivity
        Intent activityIntent = new Intent(getReactApplicationContext(), AssistOverlayActivity.class);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        getReactApplicationContext().stopActivity(activityIntent);
    }
}