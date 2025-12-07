package ru.practicum.shareit.request.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.request.dto.CreateItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestWithItemsDto;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemRequestServiceImplTest {

    @Mock
    private ItemRequestRepository requestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ItemService itemService;

    @InjectMocks
    private ItemRequestServiceImpl requestService;

    private User requestor;
    private ItemRequest itemRequest;
    private CreateItemRequestDto createRequestDto;
    private ItemDto itemDto;

    @BeforeEach
    void setUp() {
        requestor = new User();
        requestor.setId(1L);
        requestor.setName("Requestor");
        requestor.setEmail("requestor@example.com");

        itemRequest = new ItemRequest();
        itemRequest.setId(1L);
        itemRequest.setDescription("Нужна дрель для ремонта");
        itemRequest.setRequestor(requestor);
        itemRequest.setCreated(LocalDateTime.now());

        createRequestDto = new CreateItemRequestDto();
        createRequestDto.setDescription("Нужна дрель для ремонта");

        itemDto = new ItemDto();
        itemDto.setId(1L);
        itemDto.setName("Дрель");
        itemDto.setDescription("Мощная дрель");
        itemDto.setAvailable(true);
        itemDto.setOwnerId(2L);
        itemDto.setRequestId(itemRequest.getId());
    }

    @Test
    void createRequest_whenValid_thenRequestCreated() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(requestor));
        when(requestRepository.save(any(ItemRequest.class))).thenReturn(itemRequest);

        ItemRequestDto result = requestService.createRequest(createRequestDto, requestor.getId());

        assertNotNull(result);
        assertEquals(itemRequest.getId(), result.getId());
        assertEquals(itemRequest.getDescription(), result.getDescription());
        verify(userRepository).findById(requestor.getId());
        verify(requestRepository).save(any(ItemRequest.class));
    }

    @Test
    void createRequest_whenUserNotFound_thenThrowNotFoundException() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> requestService.createRequest(createRequestDto, 999L)
        );

        assertEquals("Пользователь с ID 999 не найден", exception.getMessage());
        verify(userRepository).findById(999L);
        verify(requestRepository, never()).save(any(ItemRequest.class));
    }

    @Test
    void getUserRequests_whenUserHasRequests_thenReturnRequestsWithItems() {
        List<ItemRequest> requests = List.of(itemRequest);
        List<ItemDto> items = List.of(itemDto);

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(requestor));
        when(requestRepository.findAllByRequestorIdOrderByCreatedDesc(anyLong())).thenReturn(requests);
        when(itemService.getItemsByRequestIds(anyList())).thenReturn(
                Collections.singletonMap(itemRequest.getId(), items)
        );

        List<ItemRequestWithItemsDto> result = requestService.getUserRequests(requestor.getId());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(itemRequest.getId(), result.get(0).getId());
        assertFalse(result.get(0).getItems().isEmpty());
        assertEquals(itemDto.getId(), result.get(0).getItems().get(0).getId());

        verify(userRepository).findById(requestor.getId());
        verify(requestRepository).findAllByRequestorIdOrderByCreatedDesc(requestor.getId());
        verify(itemService).getItemsByRequestIds(List.of(itemRequest.getId()));
    }

    @Test
    void getUserRequests_whenUserNotFound_thenThrowNotFoundException() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> requestService.getUserRequests(999L)
        );

        assertEquals("Пользователь с ID 999 не найден", exception.getMessage());
        verify(userRepository).findById(999L);
        verify(requestRepository, never()).findAllByRequestorIdOrderByCreatedDesc(anyLong());
    }

    @Test
    void getAllRequests_whenValidPagination_thenReturnRequests() {
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setName("Other User");

        ItemRequest otherRequest = new ItemRequest();
        otherRequest.setId(2L);
        otherRequest.setDescription("Нужен молоток");
        otherRequest.setRequestor(otherUser);
        otherRequest.setCreated(LocalDateTime.now().minusDays(1));

        List<ItemRequest> allRequests = List.of(itemRequest, otherRequest);

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(requestor));
        when(requestRepository.findAllByRequestorIdNotOrderByCreatedDesc(anyLong())).thenReturn(allRequests);
        when(itemService.getItemsByRequestIds(anyList())).thenReturn(Collections.emptyMap());

        List<ItemRequestWithItemsDto> result = requestService.getAllRequests(requestor.getId(), null, null);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(userRepository).findById(requestor.getId());
        verify(requestRepository).findAllByRequestorIdNotOrderByCreatedDesc(requestor.getId());
    }

    @Test
    void getAllRequests_whenWithPagination_thenReturnPaginatedResults() {
        List<ItemRequest> allRequests = List.of(
                itemRequest,
                createRequest(2L, "Request 2"),
                createRequest(3L, "Request 3"),
                createRequest(4L, "Request 4"),
                createRequest(5L, "Request 5")
        );

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(requestor));
        when(requestRepository.findAllByRequestorIdNotOrderByCreatedDesc(anyLong())).thenReturn(allRequests);
        when(itemService.getItemsByRequestIds(anyList())).thenReturn(Collections.emptyMap());

        List<ItemRequestWithItemsDto> result = requestService.getAllRequests(requestor.getId(), 1, 2);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(userRepository).findById(requestor.getId());
        verify(requestRepository).findAllByRequestorIdNotOrderByCreatedDesc(requestor.getId());
    }

    @Test
    void getAllRequests_whenFromOutOfRange_thenReturnEmptyList() {
        List<ItemRequest> allRequests = List.of(itemRequest);

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(requestor));
        when(requestRepository.findAllByRequestorIdNotOrderByCreatedDesc(anyLong())).thenReturn(allRequests);

        List<ItemRequestWithItemsDto> result = requestService.getAllRequests(requestor.getId(), 10, 5);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getAllRequests_whenUserNotFound_thenThrowNotFoundException() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> requestService.getAllRequests(999L, 0, 10)
        );

        assertEquals("Пользователь с ID 999 не найден", exception.getMessage());
        verify(userRepository).findById(999L);
    }

    @Test
    void getRequestById_whenRequestExists_thenReturnRequestWithItems() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(requestor));
        when(requestRepository.findById(anyLong())).thenReturn(Optional.of(itemRequest));
        when(itemService.getItemsByRequestId(anyLong())).thenReturn(List.of(itemDto));

        ItemRequestWithItemsDto result = requestService.getRequestById(itemRequest.getId(), requestor.getId());

        assertNotNull(result);
        assertEquals(itemRequest.getId(), result.getId());
        assertEquals(itemRequest.getDescription(), result.getDescription());
        assertFalse(result.getItems().isEmpty());
        assertEquals(itemDto.getId(), result.getItems().get(0).getId());

        verify(userRepository).findById(requestor.getId());
        verify(requestRepository).findById(itemRequest.getId());
        verify(itemService).getItemsByRequestId(itemRequest.getId());
    }

    @Test
    void getRequestById_whenRequestNotFound_thenThrowNotFoundException() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(requestor));
        when(requestRepository.findById(anyLong())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> requestService.getRequestById(999L, requestor.getId())
        );

        assertEquals("Запрос с ID 999 не найден", exception.getMessage());
        verify(requestRepository).findById(999L);
        verify(itemService, never()).getItemsByRequestId(anyLong());
    }

    @Test
    void getRequestById_whenUserNotFound_thenThrowNotFoundException() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> requestService.getRequestById(itemRequest.getId(), 999L)
        );

        assertEquals("Пользователь с ID 999 не найден", exception.getMessage());
        verify(userRepository).findById(999L);
        verify(requestRepository, never()).findById(anyLong());
    }

    @Test
    void getRequestById_whenNoItemsForRequest_thenReturnRequestWithEmptyItems() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(requestor));
        when(requestRepository.findById(anyLong())).thenReturn(Optional.of(itemRequest));
        when(itemService.getItemsByRequestId(anyLong())).thenReturn(List.of());

        ItemRequestWithItemsDto result = requestService.getRequestById(itemRequest.getId(), requestor.getId());

        assertNotNull(result);
        assertEquals(itemRequest.getId(), result.getId());
        assertTrue(result.getItems().isEmpty());
    }

    private ItemRequest createRequest(Long id, String description) {
        User user = new User();
        user.setId(id + 100);

        ItemRequest request = new ItemRequest();
        request.setId(id);
        request.setDescription(description);
        request.setRequestor(user);
        request.setCreated(LocalDateTime.now().minusDays(id));
        return request;
    }
}