package event;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import org.joutak.loginpluginforjoutak.utils.JoutakProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.UUID;

public abstract class BaseTest {

    protected ServerMock server;
    protected PlayerMock player;

    protected static final String TEST_NAME = "TestPlayer";
    protected static final UUID TEST_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    protected static final String TEST_FILE_PATH = "src/test/resources/player.json";

    @BeforeEach
    void setupMockBukkit() {
        server = MockBukkit.mock();
        player = server.addPlayer();
        player.setName(TEST_NAME);
        JoutakProperties.playersFilepath = TEST_FILE_PATH;
    }

    @AfterEach
    void tearDownMockBukkit() {
        MockBukkit.unmock();
    }
}
