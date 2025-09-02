package com.shivansps.fsowrapper;

public class StorageOption {
    public final String labelBase;
    public final String path;
    public final boolean primary;
    public final boolean removable;
    public final long freeBytes;

    public StorageOption(String label, String path, boolean primary, boolean removable, long freeBytes) {
        this.labelBase = label;
        this.path = path;
        this.primary = primary;
        this.removable = removable;
        this.freeBytes = freeBytes;
    }

    public String getDisplayLabel() {
        return labelBase + " ( " + ByteFormat.humanReadable(freeBytes) + " free )";
    }

    @Override
    public String toString() {
        return getDisplayLabel();
    }
}