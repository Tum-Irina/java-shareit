package ru.practicum.shareit.item.service;

import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemWithBookingsDto;

import java.util.List;
import java.util.Map;

public interface ItemService {
    ItemDto createItem(ItemDto itemDto, Long userId);

    ItemDto updateItem(Long itemId, ItemDto itemDto, Long userId);

    ItemWithBookingsDto getItemById(Long itemId, Long userId);

    List<ItemWithBookingsDto> getAllUserItems(Long userId);

    List<ItemDto> searchItems(String text);

    void deleteItem(Long itemId, Long userId);

    CommentDto addComment(Long itemId, CommentDto commentDto, Long userId);

    List<ItemDto> getItemsByRequestId(Long requestId);

    Map<Long, List<ItemDto>> getItemsByRequestIds(List<Long> requestIds);
}