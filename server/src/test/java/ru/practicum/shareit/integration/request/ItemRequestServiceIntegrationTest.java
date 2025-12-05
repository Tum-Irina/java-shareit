package ru.practicum.shareit.integration.request;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.request.dto.CreateItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestWithItemsDto;
import ru.practicum.shareit.request.service.ItemRequestService;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.service.UserService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ItemRequestServiceIntegrationTest {

    @Autowired
    private ItemRequestService requestService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private UserService userService;

    private Long requestorId;
    private Long otherUserId;
    private Long ownerId;

    @BeforeEach
    void setUp() {
        UserDto requestorDto = new UserDto();
        requestorDto.setName("Requestor");
        requestorDto.setEmail("requestor@example.com");
        UserDto createdRequestor = userService.createUser(requestorDto);
        requestorId = createdRequestor.getId();

        UserDto otherUserDto = new UserDto();
        otherUserDto.setName("Other User");
        otherUserDto.setEmail("other@example.com");
        UserDto createdOther = userService.createUser(otherUserDto);
        otherUserId = createdOther.getId();

        UserDto ownerDto = new UserDto();
        ownerDto.setName("Item Owner");
        ownerDto.setEmail("owner@example.com");
        UserDto createdOwner = userService.createUser(ownerDto);
        ownerId = createdOwner.getId();
    }

    @Test
    void createRequest_whenValid_thenRequestCreated() {
        CreateItemRequestDto requestDto = new CreateItemRequestDto();
        requestDto.setDescription("Нужна дрель для ремонта");

        ItemRequestDto createdRequest = requestService.createRequest(requestDto, requestorId);

        assertNotNull(createdRequest.getId());
        assertEquals("Нужна дрель для ремонта", createdRequest.getDescription());
        assertNotNull(createdRequest.getCreated());
    }

    @Test
    void createRequest_whenUserNotFound_thenThrowNotFoundException() {
        CreateItemRequestDto requestDto = new CreateItemRequestDto();
        requestDto.setDescription("Test Request");

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> requestService.createRequest(requestDto, 999L)
        );

        assertTrue(exception.getMessage().contains("не найден"));
    }

    @Test
    void getUserRequests_whenUserHasNoRequests_thenReturnEmptyList() {
        List<ItemRequestWithItemsDto> requests = requestService.getUserRequests(requestorId);

        assertNotNull(requests);
        assertTrue(requests.isEmpty());
    }

    @Test
    void getAllRequests_whenMultipleUsersHaveRequests_thenReturnOtherUsersRequests() {
        CreateItemRequestDto requestorRequest = new CreateItemRequestDto();
        requestorRequest.setDescription("Request from requestor");
        requestService.createRequest(requestorRequest, requestorId);

        CreateItemRequestDto otherUserRequest = new CreateItemRequestDto();
        otherUserRequest.setDescription("Request from other user");
        requestService.createRequest(otherUserRequest, otherUserId);

        List<ItemRequestWithItemsDto> allRequests = requestService.getAllRequests(requestorId, 0, 10);

        assertEquals(1, allRequests.size());
        assertEquals("Request from other user", allRequests.get(0).getDescription());
    }

    @Test
    void getRequestById_whenRequestExists_thenReturnRequest() {
        CreateItemRequestDto requestDto = new CreateItemRequestDto();
        requestDto.setDescription("Test Request");
        ItemRequestDto createdRequest = requestService.createRequest(requestDto, requestorId);

        ItemRequestWithItemsDto foundRequest = requestService.getRequestById(createdRequest.getId(), requestorId);

        assertEquals(createdRequest.getId(), foundRequest.getId());
        assertEquals(createdRequest.getDescription(), foundRequest.getDescription());
        assertNotNull(foundRequest.getCreated());
        assertNotNull(foundRequest.getItems());
        assertTrue(foundRequest.getItems().isEmpty());
    }

    @Test
    void getRequestById_whenRequestNotFound_thenThrowNotFoundException() {
        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> requestService.getRequestById(999L, requestorId)
        );

        assertTrue(exception.getMessage().contains("Запрос с ID"));
    }

    @Test
    void getRequestById_whenRequestHasItems_thenReturnRequestWithItems() {
        CreateItemRequestDto requestDto = new CreateItemRequestDto();
        requestDto.setDescription("Need a drill");
        ItemRequestDto createdRequest = requestService.createRequest(requestDto, requestorId);

        ItemDto itemDto = new ItemDto();
        itemDto.setName("Power Drill");
        itemDto.setDescription("Electric drill for home use");
        itemDto.setAvailable(true);
        itemDto.setRequestId(createdRequest.getId());
        itemService.createItem(itemDto, ownerId);

        ItemRequestWithItemsDto foundRequest = requestService.getRequestById(createdRequest.getId(), requestorId);

        assertEquals(createdRequest.getId(), foundRequest.getId());
        assertFalse(foundRequest.getItems().isEmpty());
        assertEquals(1, foundRequest.getItems().size());
        assertEquals("Power Drill", foundRequest.getItems().get(0).getName());
        assertEquals(ownerId, foundRequest.getItems().get(0).getOwnerId());
    }

    @Test
    void testRequestWithMultipleItems() {
        CreateItemRequestDto requestDto = new CreateItemRequestDto();
        requestDto.setDescription("Need tools for construction");
        ItemRequestDto createdRequest = requestService.createRequest(requestDto, requestorId);

        String[] toolNames = {"Hammer", "Screwdriver", "Wrench"};
        for (String toolName : toolNames) {
            ItemDto itemDto = new ItemDto();
            itemDto.setName(toolName);
            itemDto.setDescription("Construction tool: " + toolName);
            itemDto.setAvailable(true);
            itemDto.setRequestId(createdRequest.getId());
            itemService.createItem(itemDto, ownerId);
        }

        ItemRequestWithItemsDto foundRequest = requestService.getRequestById(createdRequest.getId(), requestorId);

        assertEquals(3, foundRequest.getItems().size());
        assertTrue(foundRequest.getItems().stream().anyMatch(item -> item.getName().equals("Hammer")));
        assertTrue(foundRequest.getItems().stream().anyMatch(item -> item.getName().equals("Screwdriver")));
        assertTrue(foundRequest.getItems().stream().anyMatch(item -> item.getName().equals("Wrench")));
    }

    @Test
    void testRequestLifecycle() {
        CreateItemRequestDto createDto = new CreateItemRequestDto();
        createDto.setDescription("Need gardening tools");
        ItemRequestDto created = requestService.createRequest(createDto, requestorId);
        assertNotNull(created.getId());

        List<ItemRequestWithItemsDto> userRequests = requestService.getUserRequests(requestorId);
        assertEquals(1, userRequests.size());

        ItemRequestWithItemsDto found = requestService.getRequestById(created.getId(), requestorId);
        assertEquals(created.getId(), found.getId());

        List<ItemRequestWithItemsDto> allRequests = requestService.getAllRequests(otherUserId, 0, 10);
        assertEquals(1, allRequests.size());

        ItemDto itemDto = new ItemDto();
        itemDto.setName("Gardening Gloves");
        itemDto.setDescription("Leather gloves for gardening");
        itemDto.setAvailable(true);
        itemDto.setRequestId(created.getId());
        itemService.createItem(itemDto, ownerId);

        ItemRequestWithItemsDto updatedRequest = requestService.getRequestById(created.getId(), requestorId);
        assertFalse(updatedRequest.getItems().isEmpty());
        assertEquals("Gardening Gloves", updatedRequest.getItems().get(0).getName());
    }
}