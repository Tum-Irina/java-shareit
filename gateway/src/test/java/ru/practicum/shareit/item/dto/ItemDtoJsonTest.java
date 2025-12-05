package ru.practicum.shareit.item.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class ItemDtoJsonTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testSerialize() throws Exception {
        ItemDto dto = new ItemDto();
        dto.setId(1L);
        dto.setName("Дрель");
        dto.setDescription("Мощная дрель");
        dto.setAvailable(true);
        dto.setRequestId(100L);
        dto.setOwnerId(2L);

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).contains("\"id\":1");
        assertThat(json).contains("\"name\":\"Дрель\"");
        assertThat(json).contains("\"description\":\"Мощная дрель\"");
        assertThat(json).contains("\"available\":true");
        assertThat(json).contains("\"requestId\":100");
        assertThat(json).contains("\"ownerId\":2");
    }

    @Test
    void testDeserialize() throws Exception {
        String json = "{"
                + "\"id\": 1,"
                + "\"name\": \"Дрель\","
                + "\"description\": \"Мощная дрель\","
                + "\"available\": true,"
                + "\"requestId\": 100,"
                + "\"ownerId\": 2"
                + "}";

        ItemDto dto = objectMapper.readValue(json, ItemDto.class);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("Дрель");
        assertThat(dto.getDescription()).isEqualTo("Мощная дрель");
        assertThat(dto.getAvailable()).isTrue();
        assertThat(dto.getRequestId()).isEqualTo(100L);
        assertThat(dto.getOwnerId()).isEqualTo(2L);
    }

    @Test
    void testDeserializeWithNullFields() throws Exception {
        String json = "{"
                + "\"name\": \"Молоток\","
                + "\"description\": \"Строительный молоток\","
                + "\"available\": false"
                + "}";

        ItemDto dto = objectMapper.readValue(json, ItemDto.class);

        assertThat(dto.getId()).isNull();
        assertThat(dto.getName()).isEqualTo("Молоток");
        assertThat(dto.getAvailable()).isFalse();
        assertThat(dto.getRequestId()).isNull();
        assertThat(dto.getOwnerId()).isNull();
    }
}