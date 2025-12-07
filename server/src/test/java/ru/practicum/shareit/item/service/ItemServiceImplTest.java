package ru.practicum.shareit.item.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemWithBookingsDto;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.CommentRepository;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private ItemServiceImpl itemService;

    private User owner;
    private User booker;
    private Item item;
    private ItemDto itemDto;
    private Comment comment;
    private Booking booking;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);
        owner.setName("Owner");
        owner.setEmail("owner@example.com");

        booker = new User();
        booker.setId(2L);
        booker.setName("Booker");
        booker.setEmail("booker@example.com");

        item = new Item();
        item.setId(1L);
        item.setName("Дрель");
        item.setDescription("Мощная дрель");
        item.setAvailable(true);
        item.setOwner(owner);

        itemDto = new ItemDto();
        itemDto.setId(1L);
        itemDto.setName("Дрель");
        itemDto.setDescription("Мощная дрель");
        itemDto.setAvailable(true);
        itemDto.setOwnerId(owner.getId());

        comment = new Comment();
        comment.setId(1L);
        comment.setText("Отличная дрель!");
        comment.setItem(item);
        comment.setAuthor(booker);
        comment.setCreated(LocalDateTime.now());

        booking = new Booking();
        booking.setId(1L);
        booking.setStart(LocalDateTime.now().minusDays(2));
        booking.setEnd(LocalDateTime.now().minusDays(1));
        booking.setItem(item);
        booking.setBooker(booker);
        booking.setStatus(BookingStatus.APPROVED);
    }

    @Test
    void createItem_whenValid_thenItemCreated() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(owner));
        when(itemRepository.save(any(Item.class))).thenReturn(item);

        ItemDto result = itemService.createItem(itemDto, owner.getId());

        assertNotNull(result);
        assertEquals(item.getId(), result.getId());
        assertEquals(item.getName(), result.getName());
        verify(userRepository).findById(owner.getId());
        verify(itemRepository).save(any(Item.class));
    }

    @Test
    void createItem_whenUserNotFound_thenThrowNotFoundException() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> itemService.createItem(itemDto, 999L)
        );

        assertEquals("Пользователь с ID 999 не найден", exception.getMessage());
        verify(userRepository).findById(999L);
        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    void createItem_whenWithRequestId_thenItemCreatedWithRequest() {
        itemDto.setRequestId(100L);
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(owner));
        when(itemRepository.save(any(Item.class))).thenReturn(item);

        ItemDto result = itemService.createItem(itemDto, owner.getId());

        assertNotNull(result);
        verify(itemRepository).save(argThat(savedItem ->
                savedItem.getRequestId() != null && savedItem.getRequestId().equals(100L)
        ));
    }

    @Test
    void updateItem_whenValidUpdate_thenItemUpdated() {
        ItemDto updateDto = new ItemDto();
        updateDto.setName("Обновленная дрель");
        updateDto.setDescription("Новое описание");

        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));
        when(itemRepository.save(any(Item.class))).thenReturn(item);

        ItemDto result = itemService.updateItem(item.getId(), updateDto, owner.getId());

        assertEquals("Обновленная дрель", result.getName());
        assertEquals("Новое описание", result.getDescription());
        verify(itemRepository).findById(item.getId());
        verify(itemRepository).save(any(Item.class));
    }

    @Test
    void updateItem_whenItemNotFound_thenThrowNotFoundException() {
        when(itemRepository.findById(anyLong())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> itemService.updateItem(999L, itemDto, owner.getId())
        );

        assertEquals("Вещь с ID 999 не найдена", exception.getMessage());
        verify(itemRepository).findById(999L);
        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    void updateItem_whenNotOwner_thenThrowNotFoundException() {
        Long otherUserId = 999L;
        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> itemService.updateItem(item.getId(), itemDto, otherUserId)
        );

        assertEquals("Только владелец может выполнить это действие", exception.getMessage());
        verify(itemRepository).findById(item.getId());
        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    void updateItem_whenPartialUpdate_thenOnlySpecifiedFieldsUpdated() {
        Item existingItem = new Item();
        existingItem.setId(1L);
        existingItem.setName("Старое имя");
        existingItem.setDescription("Старое описание");
        existingItem.setAvailable(true);
        existingItem.setOwner(owner);

        ItemDto updateDto = new ItemDto();
        updateDto.setName("Новое имя");

        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(existingItem));
        when(itemRepository.save(any(Item.class))).thenReturn(existingItem);

        ItemDto result = itemService.updateItem(existingItem.getId(), updateDto, owner.getId());

        assertEquals("Новое имя", result.getName());
        assertEquals("Старое описание", result.getDescription());
        assertTrue(result.getAvailable());
    }

    @Test
    void getItemById_whenItemExists_thenReturnItem() {
        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));
        when(commentRepository.findByItemId(anyLong())).thenReturn(List.of());

        ItemWithBookingsDto result = itemService.getItemById(item.getId(), owner.getId());

        assertNotNull(result);
        assertEquals(item.getId(), result.getId());
        assertEquals(item.getName(), result.getName());
        verify(itemRepository).findById(item.getId());
        verify(commentRepository).findByItemId(item.getId());
    }

    @Test
    void getItemById_whenItemNotFound_thenThrowNotFoundException() {
        when(itemRepository.findById(anyLong())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> itemService.getItemById(999L, owner.getId())
        );

        assertEquals("Вещь с ID 999 не найдена", exception.getMessage());
        verify(itemRepository).findById(999L);
    }

    @Test
    void getItemById_whenOwner_thenIncludeBookingInfo() {
        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));
        when(bookingRepository.findLastBooking(anyLong(), any(LocalDateTime.class)))
                .thenReturn(List.of(booking));
        when(bookingRepository.findNextBooking(anyLong(), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(commentRepository.findByItemId(anyLong())).thenReturn(List.of());

        ItemWithBookingsDto result = itemService.getItemById(item.getId(), owner.getId());

        assertNotNull(result);
        assertNotNull(result.getLastBooking());
        verify(bookingRepository).findLastBooking(eq(item.getId()), any(LocalDateTime.class));
    }

    @Test
    void getItemById_whenNotOwner_thenExcludeBookingInfo() {
        Long notOwnerId = 999L;
        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));
        when(commentRepository.findByItemId(anyLong())).thenReturn(List.of());

        ItemWithBookingsDto result = itemService.getItemById(item.getId(), notOwnerId);

        assertNotNull(result);
        assertNull(result.getLastBooking());
        assertNull(result.getNextBooking());
        verify(bookingRepository, never()).findLastBooking(anyLong(), any(LocalDateTime.class));
    }

    @Test
    void getAllUserItems_whenUserHasItems_thenReturnItems() {
        List<Item> items = List.of(item);
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(owner));
        when(itemRepository.findAllByOwnerId(anyLong())).thenReturn(items);
        when(commentRepository.findByItemIdIn(anyList())).thenReturn(List.of());
        when(bookingRepository.findLastBookingsForItems(anyList(), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(bookingRepository.findNextBookingsForItems(anyList(), any(LocalDateTime.class)))
                .thenReturn(List.of());

        List<ItemWithBookingsDto> result = itemService.getAllUserItems(owner.getId());

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userRepository).findById(owner.getId());
        verify(itemRepository).findAllByOwnerId(owner.getId());
    }

    @Test
    void getAllUserItems_whenUserNotFound_thenThrowNotFoundException() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> itemService.getAllUserItems(999L)
        );

        assertEquals("Пользователь с ID 999 не найден", exception.getMessage());
        verify(userRepository).findById(999L);
        verify(itemRepository, never()).findAllByOwnerId(anyLong());
    }

    @Test
    void searchItems_whenValidText_thenReturnItems() {
        String searchText = "дрель";
        List<Item> items = List.of(item);

        when(itemRepository.searchAvailableItems(anyString())).thenReturn(items);

        List<ItemDto> result = itemService.searchItems(searchText);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(itemRepository).searchAvailableItems(searchText.toLowerCase());
    }

    @Test
    void searchItems_whenEmptyText_thenReturnEmptyList() {
        List<ItemDto> result = itemService.searchItems("");

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(itemRepository, never()).searchAvailableItems(anyString());
    }

    @Test
    void searchItems_whenNullText_thenReturnEmptyList() {
        List<ItemDto> result = itemService.searchItems(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(itemRepository, never()).searchAvailableItems(anyString());
    }

    @Test
    void deleteItem_whenOwner_thenItemDeleted() {
        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));
        doNothing().when(itemRepository).deleteById(anyLong());

        itemService.deleteItem(item.getId(), owner.getId());

        verify(itemRepository).findById(item.getId());
        verify(itemRepository).deleteById(item.getId());
    }

    @Test
    void deleteItem_whenNotOwner_thenThrowNotFoundException() {
        Long notOwnerId = 999L;
        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> itemService.deleteItem(item.getId(), notOwnerId)
        );

        assertEquals("Только владелец может выполнить это действие", exception.getMessage());
        verify(itemRepository).findById(item.getId());
        verify(itemRepository, never()).deleteById(anyLong());
    }

    @Test
    void addComment_whenValid_thenCommentCreated() {
        CommentDto commentDto = new CommentDto();
        commentDto.setText("Отличная вещь!");

        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(booker));
        when(bookingRepository.findByBookerIdAndItemIdAndEndBefore(anyLong(), anyLong(), any(LocalDateTime.class)))
                .thenReturn(List.of(booking));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        CommentDto result = itemService.addComment(item.getId(), commentDto, booker.getId());

        assertNotNull(result);
        assertEquals(comment.getText(), result.getText());
        verify(itemRepository).findById(item.getId());
        verify(userRepository).findById(booker.getId());
        verify(bookingRepository).findByBookerIdAndItemIdAndEndBefore(
                eq(booker.getId()), eq(item.getId()), any(LocalDateTime.class));
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void addComment_whenUserDidNotBookItem_thenThrowValidationException() {
        CommentDto commentDto = new CommentDto();
        commentDto.setText("Пытаюсь оставить комментарий");

        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(booker));
        when(bookingRepository.findByBookerIdAndItemIdAndEndBefore(anyLong(), anyLong(), any(LocalDateTime.class)))
                .thenReturn(List.of());

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> itemService.addComment(item.getId(), commentDto, booker.getId())
        );

        assertTrue(exception.getMessage().contains("не брал эту вещь"));
        verify(bookingRepository).findByBookerIdAndItemIdAndEndBefore(
                eq(booker.getId()), eq(item.getId()), any(LocalDateTime.class));
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void addComment_whenItemNotFound_thenThrowNotFoundException() {
        CommentDto commentDto = new CommentDto();
        commentDto.setText("Комментарий");

        when(itemRepository.findById(anyLong())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> itemService.addComment(999L, commentDto, booker.getId())
        );

        assertEquals("Вещь с ID 999 не найдена", exception.getMessage());
        verify(itemRepository).findById(999L);
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void addComment_whenUserNotFound_thenThrowNotFoundException() {
        CommentDto commentDto = new CommentDto();
        commentDto.setText("Комментарий");

        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> itemService.addComment(item.getId(), commentDto, 999L)
        );

        assertEquals("Пользователь с ID 999 не найден", exception.getMessage());
        verify(userRepository).findById(999L);
        verify(commentRepository, never()).save(any(Comment.class));
    }
}