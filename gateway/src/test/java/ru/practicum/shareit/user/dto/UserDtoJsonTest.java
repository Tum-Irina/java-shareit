package ru.practicum.shareit.user.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class UserDtoJsonTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testSerialize() throws Exception {
        UserDto dto = new UserDto();
        dto.setId(1L);
        dto.setName("John Doe");
        dto.setEmail("john@example.com");

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).contains("\"id\":1");
        assertThat(json).contains("\"name\":\"John Doe\"");
        assertThat(json).contains("\"email\":\"john@example.com\"");
    }

    @Test
    void testDeserialize() throws Exception {
        String json = """
                {
                    "id": 1,
                    "name": "John Doe",
                    "email": "john@example.com"
                }
                """;

        UserDto dto = objectMapper.readValue(json, UserDto.class);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("John Doe");
        assertThat(dto.getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void testDeserializeWithoutId() throws Exception {
        String json = """
                {
                    "name": "Jane Smith",
                    "email": "jane@example.com"
                }
                """;

        UserDto dto = objectMapper.readValue(json, UserDto.class);

        assertThat(dto.getId()).isNull();
        assertThat(dto.getName()).isEqualTo("Jane Smith");
        assertThat(dto.getEmail()).isEqualTo("jane@example.com");
    }
}