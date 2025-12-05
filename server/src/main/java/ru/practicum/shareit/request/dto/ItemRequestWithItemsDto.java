package ru.practicum.shareit.request.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ItemRequestWithItemsDto {
    private Long id;
    private String description;
    private LocalDateTime created;
    private List<ResponseItemDto> items;

    @Data
    public static class ResponseItemDto {
        private Long id;
        private String name;
        private Long ownerId;
    }
}