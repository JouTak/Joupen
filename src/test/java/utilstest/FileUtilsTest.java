package utilstest;

import org.joupen.utils.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class FileUtilsTest {
    @TempDir
    Path tempDir;

    @Test
    void ensureDirectoryExists_nonExistent_shouldCreate() {
        File dir = tempDir.resolve("testdir").toFile();
        assertFalse(dir.exists());
        FileUtils.ensureDirectoryExists(dir);
        assertTrue(dir.exists());
    }

    @Test
    void ensureDirectoryExists_alreadyExists_shouldNotFail() {
        File dir = tempDir.toFile();
        assertTrue(dir.exists());
        assertDoesNotThrow(() -> FileUtils.ensureDirectoryExists(dir));
    }
}
