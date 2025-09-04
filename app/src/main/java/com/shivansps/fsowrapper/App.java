package com.shivansps.fsowrapper;

import android.app.Application;

import com.shivansps.fsowrapper.tts.TTSManager;

public class App extends Application {
    @Override public void onCreate() {
        TTSManager.init(this);
        TTSManager.setLanguageTag("en-US");
        TTSManager.setRate(1.0f);
        TTSManager.setPitch(1.0f);
        super.onCreate();
    }
}