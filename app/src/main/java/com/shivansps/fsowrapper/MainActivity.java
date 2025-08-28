package com.shivansps.fsowrapper;
import android.content.SharedPreferences;

import org.libsdl.app.SDLActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends SDLActivity {
    @Override
    protected String[] getArguments() {
        return new String[] {
                "-fps",
                "-no_large_shaders",
                "-no_geo_effects",
                "-no_post_process",
                "-no_deferred"
        };
    }

    @Override
    protected String[] getLibraries() {
        return new String[] {
                "SDL2",
                "fs2_open_24_3_0_arm64"
        };
    }

    @Override
    protected String getMainFunction() {
        return "android_main";
    }

    @Override
    protected void onStart() {
        super.onStart();
        copyGameAssetsOnce(Arrays.asList("fs2_demo.vpc", "shaders_v1.vp"));
    }

    private void copyGameAssetsOnce(List<String> names) {
        SharedPreferences p = getSharedPreferences("first_run", MODE_PRIVATE);
        if (p.getBoolean("copied", false)) return;

        File destDir = getExternalFilesDir(null);
        if (destDir == null) destDir = getFilesDir();

        for (String n : names) copyAssetTo(new File(destDir, n), n);
        p.edit().putBoolean("copied", true).apply();
    }

    private void copyAssetTo(File dest, String assetName) {
        if (dest.exists()) return;
        if (dest.getParentFile() != null) dest.getParentFile().mkdirs();
        try (InputStream in = getAssets().open(assetName);
             OutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) > 0) out.write(buf, 0, r);
            out.flush();
        } catch (IOException e) { e.printStackTrace(); }
    }

}