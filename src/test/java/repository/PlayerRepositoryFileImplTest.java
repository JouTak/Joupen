package repository;

import org.joupen.domain.PlayerEntity;
import org.joupen.repository.impl.PlayerRepositoryFileImpl;
import org.joupen.utils.JoupenProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

public class PlayerRepositoryFileImplTest {
    private PlayerRepositoryFileImpl repo;
    @TempDir Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        Path testFile = tempDir.resolve("players.json");
        Files.writeString(testFile, "[]");
        JoupenProperties.playersFilepath = testFile.toString();
        repo = new PlayerRepositoryFileImpl();
    }

    @Test
    void findByUuid_shouldWork() {
        UUID uuid = UUID.randomUUID();
        PlayerEntity entity = new PlayerEntity(1L, uuid, "Test", LocalDateTime.now(), LocalDateTime.now(), true);
        repo.save(entity);
        assertTrue(repo.findByUuid(uuid).isPresent());
    }

    @Test
    void findByName_shouldBeCaseInsensitive() {
        PlayerEntity entity = new PlayerEntity(1L, UUID.randomUUID(), "TestPlayer", LocalDateTime.now(), LocalDateTime.now(), true);
        repo.save(entity);
        assertTrue(repo.findByName("testplayer").isPresent());
    }

    @Test
    void delete_shouldRemove() {
        UUID uuid = UUID.randomUUID();
        PlayerEntity entity = new PlayerEntity(1L, uuid, "Test", LocalDateTime.now(), LocalDateTime.now(), true);
        repo.save(entity);
        repo.delete(uuid);
        assertFalse(repo.findByUuid(uuid).isPresent());
    }
}
