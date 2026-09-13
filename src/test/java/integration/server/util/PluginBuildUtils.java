package integration.server.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PluginBuildUtils {

    public static Path getOrBuildJoupenJar(String configuredPath) throws Exception {
        Path targetDir = Paths.get("target");
        Path foundJar = searchForJar(targetDir);
        if (foundJar != null) {
            return foundJar;
        }

        System.out.println("⚠️ Joupen jar not found. Attempting to build it via Maven...");
        String mvnCommand = System.getProperty("os.name").toLowerCase().contains("win") ? "mvn.cmd" : "mvn";

        // Убрали 'clean', чтобы не удалялись скомпилированные тестовые классы IDE
        ProcessBuilder pb = new ProcessBuilder(mvnCommand, "package", "-DskipTests");
        pb.directory(new File("."));
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[MAVEN BUILD] " + line);
            }
        }
        process.waitFor();

        Path jarAfterBuild = searchForJar(targetDir);
        if (jarAfterBuild == null) {
            throw new IllegalStateException("❌ Joupen plugin jar was not created after 'mvn package'");
        }
        return jarAfterBuild;
    }

    private static Path searchForJar(Path targetDir) throws Exception {
        if (!Files.exists(targetDir))
            return null;
        try (var stream = Files.list(targetDir)) {
            return stream
                    .filter(p -> p.getFileName().toString().startsWith("Joupen-")
                            && p.getFileName().toString().endsWith(".jar")
                            && !p.getFileName().toString().contains("original"))
                    .findFirst()
                    .orElse(null);
        }
    }
}
