package org.joupen.utils;

import java.io.File;

public class FileUtils {

    public static void ensureDirectoryExists(File directory) {
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }
}
