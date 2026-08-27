package utilstest;

import org.joupen.utils.OperationMessages;
import org.joupen.utils.YamlUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OperationMessagesTest {
    @TempDir
    Path directory;

    @Test
    void defaultMessagesAreValidYamlAndKeepLineBreaks() throws Exception {
        Path config = directory.resolve("config.yml");
        Files.writeString(config, "messages:\n" + OperationMessages.defaultsYaml());
        Map<?, ?> messages = (Map<?, ?>) YamlUtils.loadYaml(config.toFile()).get("messages");
        assertTrue(messages.get("history-entry").toString().contains("\n"));
        assertNotNull(messages.get("operation-applied"));
    }

    @Test
    void userValuesAreNotInterpretedAsPlaceholders() {
        OperationMessages.configure(Map.of("history-entry", "{reason} #{id}"));
        try {
            assertEquals("{id} $5 #7", OperationMessages.format("history-entry", Map.of("reason", "{id} $5", "id", 7)));
        } finally {
            OperationMessages.configure(Map.of());
        }
    }
}
