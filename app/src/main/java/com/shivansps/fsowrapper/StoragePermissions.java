package com.shivansps.fsowrapper;
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class StoragePermissions {
    public static final int REQ_STORAGE_PERMS = 1010;

    public static boolean hasLegacyExternalStoragePerms(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return true;
        }

        boolean hasRead  = ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) { // <=28
            boolean hasWrite = ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
            return hasRead && hasWrite;
        } else {
            return hasRead;
        }
    }

    /** Request all file access API 30+. */
    public static void requestAllFilesAccess(Activity act) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                i.setData(Uri.parse("package:" + act.getPackageName()));
                act.startActivity(i);
            } catch (Exception e) {
                Intent i = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                act.startActivity(i);
            }
        }
    }

    /** Request READ/WRITE_EXTERNAL_STORAGE  API <=29 */
    public static void requestLegacyExternalStoragePerms(Activity act) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) return;

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            ActivityCompat.requestPermissions(
                    act,
                    new String[] {
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    REQ_STORAGE_PERMS
            );
        } else {
            ActivityCompat.requestPermissions(
                    act,
                    new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
                    REQ_STORAGE_PERMS
            );
        }
    }

    public static boolean ensureStorageAccess(Activity act) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                requestAllFilesAccess(act);
                return false;
            }
        } else {
            if (!hasLegacyExternalStoragePerms(act)) {
                requestLegacyExternalStoragePerms(act);
                return false;
            }
        }
        return true;
    }
}