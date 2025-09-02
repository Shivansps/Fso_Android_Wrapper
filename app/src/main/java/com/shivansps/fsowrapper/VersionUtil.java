package com.shivansps.fsowrapper;

import java.util.Comparator;

public class VersionUtil {
    // Compare "24_3_0" as 24.3.0
    public static final Comparator<String> DESC = new Comparator<String>() {
        @Override
        public int compare(String a, String b) {
            int[] aa = parse(a), bb = parse(b);
            for (int i = 0; i < 3; i++) {
                int d = Integer.compare(bb[i], aa[i]);
                if (d != 0) return d;
            }
            return 0;
        }
    };

    private static int[] parse(String v) {
        String[] p = v.split("_");
        int[] r = new int[]{0,0,0};
        for (int i = 0; i < Math.min(3, p.length); i++) {
            try { r[i] = Integer.parseInt(p[i]); } catch (Exception ignored) {}
        }
        return r;
    }
}