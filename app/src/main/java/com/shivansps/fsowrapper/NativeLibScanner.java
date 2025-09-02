package com.shivansps.fsowrapper;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class NativeLibScanner {

    private static final Pattern PATTERN = Pattern.compile(
            "^lib(fs2_open)_(\\d+_\\d+_\\d+)(?:_([A-Za-z0-9_\\-]+))?(?:-DEBUG)?\\.so$"
    );

    public static class Catalog {
        // version -> {debug -> EngineVariant}
        public final Map<String, Map<Boolean, EngineVariant>> byVersion = new HashMap<>();
        public final Set<String> versions = new TreeSet<>(VersionUtil.DESC);
    }

    public static java.util.List<EngineVariant> flatListSorted(Catalog cat) {
        java.util.List<EngineVariant> all = new java.util.ArrayList<>();
        for (java.util.Map<Boolean, EngineVariant> m : cat.byVersion.values()) {
            all.addAll(m.values());
        }
        java.util.Collections.sort(all, new java.util.Comparator<EngineVariant>() {
            @Override
            public int compare(EngineVariant a, EngineVariant b) {
                int v = VersionUtil.DESC.compare(a.version, b.version);
                if (v != 0) return v;
                if (a.debug != b.debug) return a.debug ? 1 : -1;
                return a.baseName.compareTo(b.baseName);
            }
        });
        return all;
    }

    public static Catalog scan(Context ctx) {
        Catalog cat = new Catalog();

        scanNativeLibraryDir(ctx, cat);

        return cat;
    }

    private static void scanNativeLibraryDir(Context ctx, Catalog cat) {
        String libDir = ctx.getApplicationInfo().nativeLibraryDir; // /data/app/.../lib/<abi>
        if (TextUtils.isEmpty(libDir)) return;

        File dir = new File(libDir);
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) return;

        boolean any = false;
        for (File f : files) {
            if (addIfMatch(f.getName(), cat)) {
                any = true;
            }
        }
    }

    private static boolean addIfMatch(String fileName, Catalog cat) {
        Matcher m = PATTERN.matcher(fileName);
        if (!m.matches()) return false;

        String version = m.group(2);
        boolean isDebug = fileName.endsWith("-DEBUG.so");
        String baseName = fileName.substring(3, fileName.length() - 3);

        EngineVariant ev = new EngineVariant(version, isDebug, baseName, fileName);

        Map<Boolean, EngineVariant> variants = cat.byVersion.get(version);
        if (variants == null) {
            variants = new java.util.HashMap<>();
            cat.byVersion.put(version, variants);
        }
        variants.put(isDebug, ev);
        cat.versions.add(version);
        return true;
    }
}