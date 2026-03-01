package integration.server.util;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;

public class TestCacheUtils {

    public static void downloadDiscordSrv(String version, Path tempPluginsDir) {
        String fileName = "DiscordSRV-Build-" + version.substring(1) + ".jar";
        String url = "https://github.com/DiscordSRV/DiscordSRV/releases/download/" + version + "/" + fileName;
        System.out.println("📥 Downloading DiscordSRV " + version);
        try (InputStream in = new URL(url).openStream()) {
            Files.copy(in, tempPluginsDir.resolve("DiscordSRV.jar"), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            System.err.println("⚠️ Failed to download DiscordSRV: " + e.getMessage());
        }
    }

    public static Path preparePurpurCache(Path tempPluginsDir) throws Exception {
        Path purpurCache = Paths.get(System.getProperty("user.home"), ".joupen-test-cache", "purpur-data");
        Files.createDirectories(purpurCache);

        Path cachedPluginsDir = purpurCache.resolve("plugins");
        if (Files.exists(cachedPluginsDir)) {
            Files.walk(cachedPluginsDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (Exception ignored) {
                        }
                    });
        }

        Files.createDirectories(cachedPluginsDir);
        Files.walk(tempPluginsDir).forEach(src -> {
            try {
                Path rel = tempPluginsDir.relativize(src);
                Path dest = cachedPluginsDir.resolve(rel);
                if (Files.isDirectory(src)) {
                    Files.createDirectories(dest);
                } else {
                    Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception e) {
                System.err.println("⚠️ Сould not copy " + src + " to cache: " + e.getMessage());
            }
        });

        System.out.println("💾 Purpur cache dir: " + purpurCache);
        return purpurCache;
    }

    public static void cleanTempDir(Path tempDir) {
        if (tempDir != null) {
            try {
                Files.walk(tempDir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(java.io.File::delete);
            } catch (Exception ignored) {
            }
        }
    }
}
