package com.shivansps.fsowrapper;
import android.content.Intent;

import java.util.ArrayList;

public class GameActivity extends org.libsdl.app.SDLActivity {
    @Override
    protected String[] getArguments() {
        android.content.Intent i = getIntent();
        java.util.ArrayList<String> args = (i != null)
                ? i.getStringArrayListExtra("fsoArgs")
                : null;

        if (args == null || args.isEmpty()) {
            return new String[0];
        }
        return args.toArray(new String[0]);
    }

    @Override
    protected String[] getLibraries() {
        ArrayList<String> libs = new ArrayList<>();
        libs.add("SDL2");

        Intent i = getIntent();
        String engineBase = (i != null) ? i.getStringExtra("engineLibName") : null;
        if (engineBase == null || engineBase.trim().isEmpty()) {
            engineBase = "fs2_open_24_3_0_arm64"; // fallback
        }
        libs.add(engineBase);
        return libs.toArray(new String[0]);
    }

    @Override
    protected String getMainFunction() {
        return "android_main";
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}