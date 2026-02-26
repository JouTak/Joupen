package service;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import org.joupen.domain.PlayerEntity;
import org.joupen.repository.PlayerRepository;
import org.joupen.service.PlayerImportService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class PlayerImportServiceTest {

    private ServerMock server;
    private PlayerRepository repo;
    private PlayerImportService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        repo = mock(PlayerRepository.class);
        service = new PlayerImportService(repo);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void buildNewPlayerFromFileWithNames_validFile_shouldImportAll() throws IOException {
        Path testFile = tempDir.resolve("players.txt");
        Files.writeString(testFile, "Player1\nPlayer2\nPlayer3");
        when(repo.findByName(anyString())).thenReturn(Optional.empty());
        List<PlayerEntity> result = service.buildNewPlayerFromFileWithNames(testFile, 30);
        assertEquals(3, result.size());
    }

    @Test
    void buildNewPlayerFromFileWithNames_emptyLines_shouldSkip() throws IOException {
        Path testFile = tempDir.resolve("players_empty.txt");
        Files.writeString(testFile, "Player1\n\n  \nPlayer2");
        when(repo.findByName(anyString())).thenReturn(Optional.empty());
        List<PlayerEntity> result = service.buildNewPlayerFromFileWithNames(testFile, 15);
        assertEquals(2, result.size());
    }

    @Test
    void buildNewPlayerFromFileWithNames_nonExistentFile_shouldThrow() {
        Path nonExistent = tempDir.resolve("nonexistent.txt");
        assertThrows(IOException.class, () -> service.buildNewPlayerFromFileWithNames(nonExistent, 10));
    }
}
