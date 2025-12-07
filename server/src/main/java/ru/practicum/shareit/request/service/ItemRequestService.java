package ru.practicum.shareit.request.service;

import ru.practicum.shareit.request.dto.CreateItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestWithItemsDto;

import java.util.List;

public interface ItemRequestService {
    ItemRequestDto createRequest(CreateItemRequestDto requestDto, Long userId);

    List<ItemRequestWithItemsDto> getUserRequests(Long userId);

    List<ItemRequestWithItemsDto> getAllRequests(Long userId, Integer from, Integer size);

    ItemRequestWithItemsDto getRequestById(Long requestId, Long userId);
}