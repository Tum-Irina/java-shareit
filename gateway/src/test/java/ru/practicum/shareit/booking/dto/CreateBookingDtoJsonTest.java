package ru.practicum.shareit.booking.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class CreateBookingDtoJsonTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testSerialize() throws Exception {
        CreateBookingDto dto = new CreateBookingDto();
        dto.setItemId(1L);
        dto.setStart("2024-12-01T10:00:00");
        dto.setEnd("2024-12-02T10:00:00");

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).contains("\"itemId\":1");
        assertThat(json).contains("\"start\":\"2024-12-01T10:00:00\"");
        assertThat(json).contains("\"end\":\"2024-12-02T10:00:00\"");
    }

    @Test
    void testDeserialize() throws Exception {
        String json = """
                {
                    "itemId": 1,
                    "start": "2024-12-01T10:00:00",
                    "end": "2024-12-02T10:00:00"
                }
                """;

        CreateBookingDto dto = objectMapper.readValue(json, CreateBookingDto.class);

        assertThat(dto.getItemId()).isEqualTo(1L);
        assertThat(dto.getStart()).isEqualTo("2024-12-01T10:00:00");
        assertThat(dto.getEnd()).isEqualTo("2024-12-02T10:00:00");
    }

    @Test
    void testDeserializeWithNullFields() throws Exception {
        String json = """
                {
                    "itemId": null,
                    "start": null,
                    "end": null
                }
                """;

        CreateBookingDto dto = objectMapper.readValue(json, CreateBookingDto.class);

        assertThat(dto.getItemId()).isNull();
        assertThat(dto.getStart()).isNull();
        assertThat(dto.getEnd()).isNull();
    }
}