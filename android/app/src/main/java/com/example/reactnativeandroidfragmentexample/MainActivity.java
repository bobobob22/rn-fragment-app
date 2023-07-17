package com.example.reactnativeandroidfragmentexample;

import android.os.Bundle;
import android.security.keystore.KeyInfo;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.facebook.react.ReactActivity;
import com.facebook.react.ReactActivityDelegate;
import com.facebook.react.ReactFragment;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DefaultHardwareBackBtnHandler;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends ReactActivity {
    private Button mButton;
    private Button mButton2;

    @Override
    protected ReactActivityDelegate createReactActivityDelegate() {
        return new ReactActivityDelegate(this, getMainComponentName()) {
            @Override
            protected Bundle getLaunchOptions() {
                Bundle initialProperties = new Bundle();
                ArrayList<String> imageList = new ArrayList<String>(Arrays.asList(
                        "http://foo.com/bar1.png",
                        "http://foo.com/bar2.png"
                ));
                initialProperties.putStringArrayList("images", imageList);
                return initialProperties;
            }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);

        mButton = findViewById(R.id.button);
        mButton2 = findViewById(R.id.button2);

        Bundle initialProperties = new Bundle();
        initialProperties.putString("msg", "TES MSG from native ON START!!");
        setupBouncyCastle();
        mButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Fragment reactNativeFragment = new ReactFragment.Builder()
                        .setComponentName("HelloWorld")
                        .setLaunchOptions(initialProperties)
                        .build();

                // pass the id from the <FrameLayout> and the name of the Fragment reference we created
                getSupportFragmentManager()
                        .beginTransaction()
                        .add(R.id.reactNativeFragment, reactNativeFragment)
                        .commit();

            }
        });

        mButton2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                getReactInstanceManager().getCurrentReactContext()
                        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                        .emit("customEventName", "data from native");

            }
        });

    }


    private Bundle getLaunchOptions(String message) {
        Bundle initialProperties = new Bundle();
        initialProperties.putString("messagee", message);
        return initialProperties;
    }

    private void sendMessageFromNativeToRN(){
        Bundle initialProperties = new Bundle();
        initialProperties.putString("Something", "sth");
    }

    @Override
    public void invokeDefaultOnBackPressed() {
        super.onBackPressed();
    }


    private void setupBouncyCastle() {
        final Provider provider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);
        if (provider == null) {
            // Web3j will set up the provider lazily when it's first used.
            return;
        }
        if (provider.getClass().equals(BouncyCastleProvider.class)) {
            // BC with same package name, shouldn't happen in real life.
            return;
        }
        // Android registers its own BC provider. As it might be outdated and might not include
        // all needed ciphers, we substitute it with a known BC bundled in the app.
        // Android's BC has its package rewritten to "com.android.org.bouncycastle" and because
        // of that it's possible to have another BC implementation loaded in VM.
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
    }
}