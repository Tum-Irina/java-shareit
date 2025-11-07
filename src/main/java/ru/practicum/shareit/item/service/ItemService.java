package ru.practicum.shareit.item.service;

import ru.practicum.shareit.item.dto.ItemDto;

import java.util.List;

public interface ItemService {
    ItemDto createItem(ItemDto itemDto, Long userId);

    ItemDto updateItem(Long itemId, ItemDto itemDto, Long userId);

    ItemDto getItemById(Long itemId);

    List<ItemDto> getAllUserItems(Long userId);

    List<ItemDto> searchItems(String text);

    void deleteItem(Long itemId, Long userId);
}