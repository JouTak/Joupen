package org.joupen.service;

import org.joupen.utils.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public final class GiftQueueFile {
    private GiftQueueFile() {
    }

    public static List<String> read(Path path, int fields) throws IOException {
        List<String> lines = Files.readAllLines(path);
        boolean changed = false;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (!line.isEmpty() && !line.startsWith("#") && line.split("\\s+").length == fields) {
                lines.set(i, line + " " + UUID.randomUUID());
                changed = true;
            }
        }
        if (changed) write(path, lines);
        return lines;
    }

    public static void write(Path path, List<String> lines) throws IOException {
        FileUtils.writeAtomic(path, String.join(System.lineSeparator(), lines));
    }
}
