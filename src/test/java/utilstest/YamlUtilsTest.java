package utilstest;

import org.joupen.utils.YamlUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class YamlUtilsTest {
    @TempDir
    Path tempDir;

    @Test
    void createDefaultYaml_shouldCreateFile() throws IOException {
        File file = tempDir.resolve("config.yml").toFile();
        YamlUtils.createDefaultYaml(file, "test: value");
        assertTrue(file.exists());
    }

    @Test
    void loadYaml_validFile_shouldLoad() throws IOException {
        File file = tempDir.resolve("test.yml").toFile();
        YamlUtils.createDefaultYaml(file, "key: value\nnum: 123");
        Map<String, Object> data = YamlUtils.loadYaml(file);
        assertNotNull(data);
        assertEquals("value", data.get("key"));
    }
}
