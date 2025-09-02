package com.shivansps.fsowrapper;

public class EngineVariant {
    public final String version;   // "24_3_0"
    public final boolean debug;
    public final String baseName;
    public final String fileName;

    public EngineVariant(String version, boolean debug, String baseName, String fileName) {
        this.version = version;
        this.debug = debug;
        this.baseName = baseName;
        this.fileName = fileName;
    }

    @Override
    public String toString() {
        return baseName;
    }
}
