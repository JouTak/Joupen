package utilstest;

import org.joupen.utils.Utils;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class UtilsTest {

    @Test
    void toJson_validObject_shouldSerialize() {
        Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        String json = Utils.toJson(map);
        assertNotNull(json);
        assertTrue(json.contains("key"));
        assertTrue(json.contains("value"));
    }

    @Test
    void fromJson_validJson_shouldDeserialize() {
        String json = "{\"name\":\"Test\"}";
        Map result = Utils.fromJson(json, Map.class);
        assertNotNull(result);
        assertEquals("Test", result.get("name"));
    }

    @Test
    void fromJson_invalidJson_shouldReturnNull() {
        String invalidJson = "{invalid}";
        Map result = Utils.fromJson(invalidJson, Map.class);
        assertNull(result);
    }

    @Test
    void toJson_nullObject_shouldHandleGracefully() {
        assertDoesNotThrow(() -> Utils.toJson(null));
    }
}
