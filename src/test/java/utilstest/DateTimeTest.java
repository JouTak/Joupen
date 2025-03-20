package utilstest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.joutak.loginpluginforjoutak.dto.PlayerDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class DateTimeTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    public void testSerializationAndDeserialization() throws Exception {
        PlayerDto originalDto = PlayerDto.builder()
                .id(1L)
                .name("Player1")
                .uuid(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
                .lastProlongDate(LocalDate.of(2025, 1, 1))
                .validUntil(LocalDate.of(2025, 5, 18))
                .paid(true)
                .build();

        String json = objectMapper.writeValueAsString(originalDto);
        System.out.println("Serialized JSON: " + json);

        assertTrue(json.contains("\"lastProlongDate\":\"2025-01-01\""),
                "lastProlongDate should be in 'yyyy-MM-dd' format");
        assertTrue(json.contains("\"validUntil\":\"2025-05-18\""),
                "validUntil should be in 'yyyy-MM-dd' format");
        assertTrue(json.contains("\"paid\":true"),
                "paid should be present as 'paid'");

        PlayerDto deserializedDto = objectMapper.readValue(json, PlayerDto.class);

        assertEquals(originalDto.getId(), deserializedDto.getId(), "ID should match");
        assertEquals(originalDto.getName(), deserializedDto.getName(), "Name should match");
        assertEquals(originalDto.getUuid(), deserializedDto.getUuid(), "UUID should match");
        assertEquals(originalDto.getLastProlongDate(), deserializedDto.getLastProlongDate(),
                "lastProlongDate should match");
        assertEquals(originalDto.getValidUntil(), deserializedDto.getValidUntil(),
                "validUntil should match");
        assertTrue(deserializedDto.isPaid(), "isPaid should be true"); // Исправлено
    }

    @Test
    public void testDeserializationFromJsonString() throws Exception {
        String json = """
                {
                    "id": 2,
                    "name": "Player2",
                    "uuid": "123e4567-e89b-12d3-a456-426614174000",
                    "lastProlongDate": "2024-12-31",
                    "validUntil": "2025-06-30",
                    "paid": false
                }
                """;

        PlayerDto dto = objectMapper.readValue(json, PlayerDto.class);

        assertEquals(2L, dto.getId(), "ID should be 2");
        assertEquals("Player2", dto.getName(), "Name should be Player2");
        assertEquals(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), dto.getUuid(),
                "UUID should match");
        assertEquals(LocalDate.of(2024, 12, 31), dto.getLastProlongDate(),
                "lastProlongDate should be 2024-12-31");
        assertEquals(LocalDate.of(2025, 6, 30), dto.getValidUntil(),
                "validUntil should be 2025-06-30");
        assertFalse(dto.isPaid(), "isPaid should be false"); // Исправлено
    }
}