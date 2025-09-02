package com.shivansps.fsowrapper;

import java.util.Locale;

public class ByteFormat {
    public static String humanReadable(long bytes) {
        final String[] units = { "B", "KB", "MB", "GB", "TB", "PB" };
        double val = bytes;
        int i = 0;

        while (val >= 1024.0 && i < units.length - 1) {
            val /= 1024.0;
            i++;
        }

        if (i <= 1) {
            return String.format(Locale.US, "%.0f %s", val, units[i]);
        } else {
            return String.format(Locale.US, "%.1f %s", val, units[i]);
        }
    }
}