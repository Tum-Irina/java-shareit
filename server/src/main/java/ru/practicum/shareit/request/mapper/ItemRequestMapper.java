package ru.practicum.shareit.request.mapper;

import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.request.dto.CreateItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestWithItemsDto;
import ru.practicum.shareit.request.model.ItemRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class ItemRequestMapper {

    public static ItemRequestDto toDto(ItemRequest request) {
        ItemRequestDto dto = new ItemRequestDto();
        dto.setId(request.getId());
        dto.setDescription(request.getDescription());
        dto.setCreated(request.getCreated());
        return dto;
    }

    public static ItemRequestWithItemsDto toRequestWithItemsDto(ItemRequest request, List<ItemDto> items) {
        ItemRequestWithItemsDto dto = new ItemRequestWithItemsDto();
        dto.setId(request.getId());
        dto.setDescription(request.getDescription());
        dto.setCreated(request.getCreated());

        if (items != null) {
            List<ItemRequestWithItemsDto.ResponseItemDto> responseItems = items.stream()
                    .map(item -> {
                        ItemRequestWithItemsDto.ResponseItemDto responseItem = new ItemRequestWithItemsDto.ResponseItemDto();
                        responseItem.setId(item.getId());
                        responseItem.setName(item.getName());
                        responseItem.setOwnerId(item.getOwnerId());
                        return responseItem;
                    })
                    .collect(Collectors.toList());
            dto.setItems(responseItems);
        }
        return dto;
    }

    public static ItemRequest toEntity(CreateItemRequestDto dto) {
        ItemRequest request = new ItemRequest();
        request.setDescription(dto.getDescription());
        request.setCreated(LocalDateTime.now());
        return request;
    }
}