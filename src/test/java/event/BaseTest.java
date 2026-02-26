package event;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import org.joupen.utils.JoupenProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class BaseTest {

    protected ServerMock server;
    protected PlayerMock player;

    protected static final String TEST_NAME = "TestPlayer";

    @BeforeEach
    void setupMockBukkit() {
        server = MockBukkit.mock();
        player = server.addPlayer();
        player.setName(TEST_NAME);

        // Создаём временный файл с тестовыми данными
        try {
            java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("test-player", ".json");
            String testPlayerJson = "{\n" +
                    "  \"playerDtoList\": [\n" +
                    "    {\n" +
                    "      \"id\": null,\n" +
                    "      \"name\": \"" + TEST_NAME + "\",\n" +
                    "      \"uuid\": \"994f326b-cfb5-4502-ce62-8eaddafd622d\",\n" +
                    "      \"lastProlongDate\": \"2025-02-28\",\n" +
                    "      \"validUntil\": \"2025-03-30\",\n" +
                    "      \"paid\": true\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";
            java.nio.file.Files.writeString(tempFile, testPlayerJson);
            JoupenProperties.playersFilepath = tempFile.toString();
            tempFile.toFile().deleteOnExit(); // Удаляется автоматически
        } catch (Exception e) {
            throw new RuntimeException("Failed to create temp test file", e);
        }
    }

    @AfterEach
    void tearDownMockBukkit() {
        MockBukkit.unmock();
    }
}
