package com.example.reactnativeandroidfragmentexample;

import android.os.Bundle;
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
}