package ru.practicum.shareit.request.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.request.dto.CreateItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestWithItemsDto;
import ru.practicum.shareit.request.mapper.ItemRequestMapper;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemRequestServiceImpl implements ItemRequestService {

    private final ItemRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final ItemService itemService;

    @Override
    @Transactional
    public ItemRequestDto createRequest(CreateItemRequestDto requestDto, Long userId) {
        User requestor = findUserOrThrow(userId);
        ItemRequest request = ItemRequestMapper.toEntity(requestDto);
        request.setRequestor(requestor);
        ItemRequest savedRequest = requestRepository.save(request);
        return ItemRequestMapper.toDto(savedRequest);
    }

    @Override
    public List<ItemRequestWithItemsDto> getUserRequests(Long userId) {
        findUserOrThrow(userId);
        List<ItemRequest> requests = requestRepository.findAllByRequestorIdOrderByCreatedDesc(userId);
        return enrichRequestsWithItems(requests);
    }

    @Override
    public List<ItemRequestWithItemsDto> getAllRequests(Long userId, Integer from, Integer size) {
        findUserOrThrow(userId);
        List<ItemRequest> requests = requestRepository.findAllByRequestorIdNotOrderByCreatedDesc(userId);

        if (from != null && size != null) {
            int start = from;
            int end = Math.min(start + size, requests.size());
            if (start >= requests.size()) {
                return Collections.emptyList();
            }
            requests = requests.subList(start, end);
        }
        return enrichRequestsWithItems(requests);
    }

    @Override
    public ItemRequestWithItemsDto getRequestById(Long requestId, Long userId) {
        findUserOrThrow(userId);
        ItemRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос с ID " + requestId + " не найден"));

        List<ItemDto> items = itemService.getItemsByRequestId(requestId);
        return ItemRequestMapper.toRequestWithItemsDto(request, items);
    }

    private List<ItemRequestWithItemsDto> enrichRequestsWithItems(List<ItemRequest> requests) {
        if (requests.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> requestIds = requests.stream()
                .map(ItemRequest::getId)
                .collect(Collectors.toList());

        Map<Long, List<ItemDto>> itemsByRequestId = itemService.getItemsByRequestIds(requestIds);

        return requests.stream()
                .map(request -> {
                    List<ItemDto> items = itemsByRequestId.getOrDefault(request.getId(), Collections.emptyList());
                    return ItemRequestMapper.toRequestWithItemsDto(request, items);
                })
                .collect(Collectors.toList());
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + userId + " не найден"));
    }
}