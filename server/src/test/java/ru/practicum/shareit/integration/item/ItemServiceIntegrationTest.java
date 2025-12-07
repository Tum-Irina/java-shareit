package ru.practicum.shareit.integration.item;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemWithBookingsDto;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.service.UserService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ItemServiceIntegrationTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private UserService userService;

    private Long ownerId;
    private Long bookerId;

    @BeforeEach
    void setUp() {
        UserDto ownerDto = new UserDto();
        ownerDto.setName("Item Owner");
        ownerDto.setEmail("owner@example.com");
        UserDto createdOwner = userService.createUser(ownerDto);
        ownerId = createdOwner.getId();

        UserDto bookerDto = new UserDto();
        bookerDto.setName("Booker");
        bookerDto.setEmail("booker@example.com");
        UserDto createdBooker = userService.createUser(bookerDto);
        bookerId = createdBooker.getId();
    }

    @Test
    void createItem_whenValid_thenItemCreated() {
        ItemDto itemDto = new ItemDto();
        itemDto.setName("Test Item");
        itemDto.setDescription("Test Description");
        itemDto.setAvailable(true);

        ItemDto createdItem = itemService.createItem(itemDto, ownerId);

        assertNotNull(createdItem.getId());
        assertEquals("Test Item", createdItem.getName());
        assertEquals("Test Description", createdItem.getDescription());
        assertTrue(createdItem.getAvailable());
        assertEquals(ownerId, createdItem.getOwnerId());
    }

    @Test
    void createItem_whenUserNotFound_thenThrowNotFoundException() {
        ItemDto itemDto = new ItemDto();
        itemDto.setName("Test Item");
        itemDto.setDescription("Test Description");
        itemDto.setAvailable(true);

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> itemService.createItem(itemDto, 999L)
        );

        assertTrue(exception.getMessage().contains("не найден"));
    }

    @Test
    void updateItem_whenValidUpdate_thenItemUpdated() {
        ItemDto createDto = new ItemDto();
        createDto.setName("Original Name");
        createDto.setDescription("Original Description");
        createDto.setAvailable(true);
        ItemDto createdItem = itemService.createItem(createDto, ownerId);

        ItemDto updateDto = new ItemDto();
        updateDto.setName("Updated Name");
        updateDto.setDescription("Updated Description");
        updateDto.setAvailable(false);

        ItemDto updatedItem = itemService.updateItem(createdItem.getId(), updateDto, ownerId);

        assertEquals(createdItem.getId(), updatedItem.getId());
        assertEquals("Updated Name", updatedItem.getName());
        assertEquals("Updated Description", updatedItem.getDescription());
        assertFalse(updatedItem.getAvailable());
    }

    @Test
    void updateItem_whenNotOwner_thenThrowNotFoundException() {
        ItemDto createDto = new ItemDto();
        createDto.setName("Test Item");
        createDto.setDescription("Test Description");
        createDto.setAvailable(true);
        ItemDto createdItem = itemService.createItem(createDto, ownerId);

        ItemDto updateDto = new ItemDto();
        updateDto.setName("Updated Name");

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> itemService.updateItem(createdItem.getId(), updateDto, bookerId)
        );

        assertTrue(exception.getMessage().contains("Только владелец"));
    }

    @Test
    void getItemById_whenItemExists_thenReturnItem() {
        ItemDto createDto = new ItemDto();
        createDto.setName("Test Item");
        createDto.setDescription("Test Description");
        createDto.setAvailable(true);
        ItemDto createdItem = itemService.createItem(createDto, ownerId);

        ItemWithBookingsDto foundItem = itemService.getItemById(createdItem.getId(), ownerId);

        assertEquals(createdItem.getId(), foundItem.getId());
        assertEquals(createdItem.getName(), foundItem.getName());
        assertEquals(createdItem.getDescription(), foundItem.getDescription());
    }

    @Test
    void getItemById_whenItemNotFound_thenThrowNotFoundException() {
        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> itemService.getItemById(999L, ownerId)
        );

        assertTrue(exception.getMessage().contains("не найдена"));
    }

    @Test
    void getAllUserItems_whenUserHasItems_thenReturnItems() {
        ItemDto item1 = new ItemDto();
        item1.setName("Item 1");
        item1.setDescription("Description 1");
        item1.setAvailable(true);
        itemService.createItem(item1, ownerId);

        ItemDto item2 = new ItemDto();
        item2.setName("Item 2");
        item2.setDescription("Description 2");
        item2.setAvailable(true);
        itemService.createItem(item2, ownerId);

        List<ItemWithBookingsDto> items = itemService.getAllUserItems(ownerId);

        assertNotNull(items);
        assertEquals(2, items.size());
    }

    @Test
    void getAllUserItems_whenUserHasNoItems_thenReturnEmptyList() {
        UserDto otherUser = new UserDto();
        otherUser.setName("Other User");
        otherUser.setEmail("other@example.com");
        UserDto createdUser = userService.createUser(otherUser);

        List<ItemWithBookingsDto> items = itemService.getAllUserItems(createdUser.getId());

        assertNotNull(items);
        assertTrue(items.isEmpty());
    }

    @Test
    void searchItems_whenTextMatches_thenReturnItems() {
        ItemDto item1 = new ItemDto();
        item1.setName("Дрель аккумуляторная");
        item1.setDescription("Мощная дрель");
        item1.setAvailable(true);
        itemService.createItem(item1, ownerId);

        ItemDto item2 = new ItemDto();
        item2.setName("Молоток");
        item2.setDescription("Строительный молоток");
        item2.setAvailable(true);
        itemService.createItem(item2, ownerId);

        ItemDto item3 = new ItemDto();
        item3.setName("Другая дрель");
        item3.setDescription("Сетевая дрель");
        item3.setAvailable(false);
        itemService.createItem(item3, ownerId);

        List<ItemDto> searchResults = itemService.searchItems("дрель");

        assertEquals(1, searchResults.size());
        assertEquals("Дрель аккумуляторная", searchResults.get(0).getName());
    }

    @Test
    void searchItems_whenEmptyText_thenReturnEmptyList() {
        ItemDto item = new ItemDto();
        item.setName("Test Item");
        item.setDescription("Test Description");
        item.setAvailable(true);
        itemService.createItem(item, ownerId);

        List<ItemDto> searchResults = itemService.searchItems("");

        assertNotNull(searchResults);
        assertTrue(searchResults.isEmpty());
    }

    @Test
    void deleteItem_whenOwner_thenItemDeleted() {
        ItemDto createDto = new ItemDto();
        createDto.setName("To Delete");
        createDto.setDescription("Will be deleted");
        createDto.setAvailable(true);
        ItemDto createdItem = itemService.createItem(createDto, ownerId);

        itemService.deleteItem(createdItem.getId(), ownerId);

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> itemService.getItemById(createdItem.getId(), ownerId)
        );

        assertTrue(exception.getMessage().contains("не найдена"));
    }

    @Test
    void deleteItem_whenNotOwner_thenThrowNotFoundException() {
        ItemDto createDto = new ItemDto();
        createDto.setName("Test Item");
        createDto.setDescription("Test Description");
        createDto.setAvailable(true);
        ItemDto createdItem = itemService.createItem(createDto, ownerId);

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> itemService.deleteItem(createdItem.getId(), bookerId)
        );

        assertTrue(exception.getMessage().contains("Только владелец"));
    }

    @Test
    void testItemLifecycle() {
        ItemDto createDto = new ItemDto();
        createDto.setName("Lifecycle Item");
        createDto.setDescription("Item for lifecycle test");
        createDto.setAvailable(true);
        ItemDto created = itemService.createItem(createDto, ownerId);
        assertNotNull(created.getId());

        ItemWithBookingsDto found = itemService.getItemById(created.getId(), ownerId);
        assertEquals(created.getId(), found.getId());

        ItemDto updateDto = new ItemDto();
        updateDto.setName("Updated Item");
        ItemDto updated = itemService.updateItem(created.getId(), updateDto, ownerId);
        assertEquals("Updated Item", updated.getName());

        List<ItemDto> searchResults = itemService.searchItems("Item");
        assertFalse(searchResults.isEmpty());

        List<ItemWithBookingsDto> allItems = itemService.getAllUserItems(ownerId);
        assertFalse(allItems.isEmpty());

        itemService.deleteItem(created.getId(), ownerId);

        assertThrows(NotFoundException.class,
                () -> itemService.getItemById(created.getId(), ownerId));
    }
}