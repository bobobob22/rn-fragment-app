package com.example.reactnativeandroidfragmentexample.Communication;

import android.util.Log;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.RCTNativeAppEventEmitter;
import androidx.annotation.NonNull;
import javax.annotation.Nullable;


public class CommunicationModule extends ReactContextBaseJavaModule {
    private static ReactApplicationContext reactContext;

    @NonNull
    @Override
    public String getName() {
        return "CommunicationModule";
    }

    public CommunicationModule(ReactApplicationContext reactContext) {
        super(reactContext);
        CommunicationModule.reactContext = reactContext;
    }

    @ReactMethod
    public static void sendMessageToNative(String payload) {
        Log.d("EVENT received from RN", payload);
    }
}
