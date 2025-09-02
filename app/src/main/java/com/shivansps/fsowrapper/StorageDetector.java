package com.shivansps.fsowrapper;

import static androidx.core.content.ContextCompat.getExternalFilesDirs;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class StorageDetector {
    public static List<StorageOption> listOptions(Context ctx) {
        List<StorageOption> out = new ArrayList<>();

        File[] dirs = getExternalFilesDirs(ctx, null);
        if (dirs != null) {
            for (int i = 0; i < dirs.length; i++) {
                File f = dirs[i];
                if (f == null) continue;

                String state = Environment.getExternalStorageState(f);
                if (!Environment.MEDIA_MOUNTED.equals(state) &&
                        !Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                    continue;
                }

                boolean removable = Build.VERSION.SDK_INT >= 21 && Environment.isExternalStorageRemovable(f);
                boolean primary = false;
                String niceLabel = null;

                if (Build.VERSION.SDK_INT >= 24) {
                    StorageManager sm = (StorageManager) ctx.getSystemService(Context.STORAGE_SERVICE);
                    if (sm != null) {
                        StorageVolume vol = sm.getStorageVolume(f);
                        if (vol != null) {
                            primary = vol.isPrimary();
                            niceLabel = vol.getDescription(ctx);
                        }
                    }
                }
                if (niceLabel == null) {
                    niceLabel = (i == 0) ? "Phone" : (removable ? "SDCard/USB" : "External Storage");
                    if (i == 0) primary = true;
                }

                long free = computeFreeBytesSafe(f);

                out.add(new StorageOption(niceLabel, f.getAbsolutePath(), primary, removable, free));
            }
        }

        if (out.isEmpty()) {
            File priv = ctx.getFilesDir();
            long free = computeFreeBytesSafe(priv);
            out.add(new StorageOption("Intern (private app)", priv.getAbsolutePath(), true, false, free));
        }

        java.util.Collections.sort(out, (a, b) -> {
            if (a.primary != b.primary) return a.primary ? -1 : 1;
            if (a.removable != b.removable) return a.removable ? 1 : -1;
            return a.labelBase.compareToIgnoreCase(b.labelBase);
        });

        return out;
    }

    private static long computeFreeBytesSafe(File dir) {
        try {
            StatFs stat = new StatFs(dir.getAbsolutePath());
            if (Build.VERSION.SDK_INT >= 18) {
                return stat.getAvailableBytes();
            } else {
                // Fallback pre-API 18
                long blocks = (long) stat.getAvailableBlocks();
                long size   = (long) stat.getBlockSize();
                return blocks * size;
            }
        } catch (Throwable t) {
            return 0L;
        }
    }

}