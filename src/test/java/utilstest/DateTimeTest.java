package utilstest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.joupen.dto.PlayerDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class DateTimeTest {

    private Gson gson;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @BeforeEach
    public void setUp() {
        gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDateTime.class, (com.google.gson.JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                        new com.google.gson.JsonPrimitive(src.format(DATE_FORMATTER)))
                .registerTypeAdapter(LocalDateTime.class, (com.google.gson.JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                        LocalDateTime.parse(json.getAsJsonPrimitive().getAsString(), DATE_FORMATTER))
                .create();
    }

    @Test
    public void testSerializationAndDeserialization() {
        PlayerDto originalDto = PlayerDto.builder()
                .id(1L)
                .name("Player1")
                .uuid(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
                .lastProlongDate(LocalDateTime.of(2025, 1, 1, 0, 0))
                .validUntil(LocalDateTime.of(2025, 5, 18, 0, 0))
                .paid(true)
                .build();

        String json = gson.toJson(originalDto);
        System.out.println("Serialized JSON: " + json);

        assertTrue(json.contains("\"lastProlongDate\": \"2025-01-01T00:00:00\""), "lastProlongDate should be in 'yyyy-MM-dd'T'HH:mm:ss' format");
        assertTrue(json.contains("\"validUntil\": \"2025-05-18T00:00:00\""), "validUntil should be in 'yyyy-MM-dd'T'HH:mm:ss' format");
        assertTrue(json.contains("\"paid\": true"), "paid should be present as 'paid'");

        PlayerDto deserializedDto = gson.fromJson(json, PlayerDto.class);

        assertEquals(originalDto.getId(), deserializedDto.getId(), "ID should match");
        assertEquals(originalDto.getName(), deserializedDto.getName(), "Name should match");
        assertEquals(originalDto.getUuid(), deserializedDto.getUuid(), "UUID should match");
        assertEquals(originalDto.getLastProlongDate(), deserializedDto.getLastProlongDate(), "lastProlongDate should match");
        assertEquals(originalDto.getValidUntil(), deserializedDto.getValidUntil(), "validUntil should match");
        assertTrue(deserializedDto.getPaid(), "isPaid should be true");
    }

    @Test
    public void testDeserializationFromJsonString() {
        String json = """
                {
                    "id": 2,
                    "name": "Player2",
                    "uuid": "123e4567-e89b-12d3-a456-426614174000",
                    "lastProlongDate": "2024-12-31T00:00:00",
                    "validUntil": "2025-06-30T00:00:00",
                    "paid": false
                }
                """;

        PlayerDto dto = gson.fromJson(json, PlayerDto.class);

        assertEquals(2L, dto.getId(), "ID should be 2");
        assertEquals("Player2", dto.getName(), "Name should be Player2");
        assertEquals(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), dto.getUuid(), "UUID should match");
        assertEquals(LocalDateTime.of(2024, 12, 31, 0, 0, 0), dto.getLastProlongDate(), "lastProlongDate should be 2024-12-31 00:00:00");
        assertEquals(LocalDateTime.of(2025, 6, 30, 0, 0, 0), dto.getValidUntil(), "validUntil should be 2025-06-30 00:00:00");
        assertFalse(dto.getPaid(), "isPaid should be false");
    }
}
