package ru.practicum.shareit.item.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class CommentDtoJsonTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testSerialize() throws Exception {
        CommentDto dto = new CommentDto();
        dto.setId(1L);
        dto.setText("Отличная вещь!");
        dto.setAuthorName("John");
        dto.setCreated(LocalDateTime.of(2024, 12, 1, 10, 0, 0));

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).contains("\"id\":1");
        assertThat(json).contains("\"text\":\"Отличная вещь!\"");
        assertThat(json).contains("\"authorName\":\"John\"");
        assertThat(json).contains("\"created\":\"2024-12-01T10:00:00\"");
    }

    @Test
    void testDeserialize() throws Exception {
        String json = """
                {
                    "id": 1,
                    "text": "Очень полезная вещь",
                    "authorName": "Alice",
                    "created": "2024-12-01T10:00:00"
                }
                """;

        CommentDto dto = objectMapper.readValue(json, CommentDto.class);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getText()).isEqualTo("Очень полезная вещь");
        assertThat(dto.getAuthorName()).isEqualTo("Alice");
        assertThat(dto.getCreated()).isEqualTo(LocalDateTime.of(2024, 12, 1, 10, 0, 0));
    }

    @Test
    void testDeserializeWithoutOptionalFields() throws Exception {
        String json = """
                {
                    "text": "Простой комментарий"
                }
                """;

        CommentDto dto = objectMapper.readValue(json, CommentDto.class);

        assertThat(dto.getId()).isNull();
        assertThat(dto.getText()).isEqualTo("Простой комментарий");
        assertThat(dto.getAuthorName()).isNull();
        assertThat(dto.getCreated()).isNull();
    }
}