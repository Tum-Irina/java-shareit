package ru.practicum.shareit.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.CreateBookingDto;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemWithBookingsDto;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.request.dto.CreateItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestWithItemsDto;
import ru.practicum.shareit.request.service.ItemRequestService;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.service.UserService;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FullIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private ItemRequestService requestService;

    @Test
    void testCompleteScenario() {
        UserDto ownerDto = new UserDto();
        ownerDto.setName("Owner");
        ownerDto.setEmail("owner@example.com");
        UserDto owner = userService.createUser(ownerDto);

        UserDto bookerDto = new UserDto();
        bookerDto.setName("Booker");
        bookerDto.setEmail("booker@example.com");
        UserDto booker = userService.createUser(bookerDto);

        UserDto anotherUserDto = new UserDto();
        anotherUserDto.setName("Another User");
        anotherUserDto.setEmail("another@example.com");
        UserDto anotherUser = userService.createUser(anotherUserDto);

        CreateItemRequestDto requestDto = new CreateItemRequestDto();
        requestDto.setDescription("Need a power drill for home repairs");
        ItemRequestDto itemRequest = requestService.createRequest(requestDto, anotherUser.getId());

        ItemDto itemDto = new ItemDto();
        itemDto.setName("Bosch Power Drill");
        itemDto.setDescription("Professional power drill 800W");
        itemDto.setAvailable(true);
        itemDto.setRequestId(itemRequest.getId());
        ItemDto item = itemService.createItem(itemDto, owner.getId());

        ItemRequestWithItemsDto requestWithItems = requestService.getRequestById(itemRequest.getId(), anotherUser.getId());
        assertFalse(requestWithItems.getItems().isEmpty());
        assertEquals("Bosch Power Drill", requestWithItems.getItems().get(0).getName());

        CreateBookingDto bookingDto = new CreateBookingDto();
        bookingDto.setItemId(item.getId());
        bookingDto.setStart(LocalDateTime.now().plusDays(1).toString());
        bookingDto.setEnd(LocalDateTime.now().plusDays(3).toString());
        BookingDto booking = bookingService.createBooking(bookingDto, booker.getId());
        assertEquals(BookingStatus.WAITING, booking.getStatus());

        BookingDto approvedBooking = bookingService.approveBooking(booking.getId(), true, owner.getId());
        assertEquals(BookingStatus.APPROVED, approvedBooking.getStatus());

        CreateBookingDto pastBookingDto = new CreateBookingDto();
        pastBookingDto.setItemId(item.getId());
        pastBookingDto.setStart(LocalDateTime.now().minusDays(3).toString());
        pastBookingDto.setEnd(LocalDateTime.now().minusDays(1).toString());
        BookingDto pastBooking = bookingService.createBooking(pastBookingDto, booker.getId());
        bookingService.approveBooking(pastBooking.getId(), true, owner.getId());

        CommentDto commentDto = new CommentDto();
        commentDto.setText("Excellent drill, worked perfectly!");
        CommentDto comment = itemService.addComment(item.getId(), commentDto, booker.getId());
        assertEquals("Excellent drill, worked perfectly!", comment.getText());

        ItemWithBookingsDto itemWithDetails = itemService.getItemById(item.getId(), owner.getId());
        assertFalse(itemWithDetails.getComments().isEmpty());
        assertEquals(1, itemWithDetails.getComments().size());

        List<BookingDto> userBookings = bookingService.getUserBookings(booker.getId(), ru.practicum.shareit.booking.model.BookingState.ALL);
        assertFalse(userBookings.isEmpty());

        List<BookingDto> ownerBookings = bookingService.getOwnerBookings(owner.getId(), ru.practicum.shareit.booking.model.BookingState.ALL);
        assertFalse(ownerBookings.isEmpty());

        List<ItemDto> searchResults = itemService.searchItems("drill");
        assertFalse(searchResults.isEmpty());

        ItemDto updateDto = new ItemDto();
        updateDto.setAvailable(false);
        ItemDto updatedItem = itemService.updateItem(item.getId(), updateDto, owner.getId());
        assertFalse(updatedItem.getAvailable());

        CreateBookingDto newBookingDto = new CreateBookingDto();
        newBookingDto.setItemId(item.getId());
        newBookingDto.setStart(LocalDateTime.now().plusDays(5).toString());
        newBookingDto.setEnd(LocalDateTime.now().plusDays(7).toString());

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> bookingService.createBooking(newBookingDto, booker.getId())
        );
        assertTrue(exception.getMessage().contains("недоступна"));

        itemService.deleteItem(item.getId(), owner.getId());

        NotFoundException notFoundException = assertThrows(
                NotFoundException.class,
                () -> itemService.getItemById(item.getId(), owner.getId())
        );
        assertTrue(notFoundException.getMessage().contains("не найдена"));
    }

    @Test
    void testUserCannotBookOwnItem() {
        UserDto userDto = new UserDto();
        userDto.setName("Test User");
        userDto.setEmail("test@example.com");
        UserDto user = userService.createUser(userDto);

        ItemDto itemDto = new ItemDto();
        itemDto.setName("Test Item");
        itemDto.setDescription("Test Description");
        itemDto.setAvailable(true);
        ItemDto item = itemService.createItem(itemDto, user.getId());

        CreateBookingDto bookingDto = new CreateBookingDto();
        bookingDto.setItemId(item.getId());
        bookingDto.setStart(LocalDateTime.now().plusDays(1).toString());
        bookingDto.setEnd(LocalDateTime.now().plusDays(2).toString());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> bookingService.createBooking(bookingDto, user.getId())
        );
        assertTrue(exception.getMessage().contains("Нельзя бронировать свою вещь"));
    }

    @Test
    void testUserCannotCommentWithoutBooking() {
        UserDto ownerDto = new UserDto();
        ownerDto.setName("Owner");
        ownerDto.setEmail("owner@example.com");
        UserDto owner = userService.createUser(ownerDto);

        UserDto bookerDto = new UserDto();
        bookerDto.setName("Booker");
        bookerDto.setEmail("booker@example.com");
        UserDto booker = userService.createUser(bookerDto);

        UserDto strangerDto = new UserDto();
        strangerDto.setName("Stranger");
        strangerDto.setEmail("stranger@example.com");
        UserDto stranger = userService.createUser(strangerDto);

        ItemDto itemDto = new ItemDto();
        itemDto.setName("Test Item");
        itemDto.setDescription("Test Description");
        itemDto.setAvailable(true);
        ItemDto item = itemService.createItem(itemDto, owner.getId());

        CreateBookingDto bookingDto = new CreateBookingDto();
        bookingDto.setItemId(item.getId());
        bookingDto.setStart(LocalDateTime.now().minusDays(3).toString());
        bookingDto.setEnd(LocalDateTime.now().minusDays(1).toString());
        BookingDto booking = bookingService.createBooking(bookingDto, booker.getId());
        bookingService.approveBooking(booking.getId(), true, owner.getId());

        CommentDto commentDto = new CommentDto();
        commentDto.setText("Great item!");
        CommentDto comment = itemService.addComment(item.getId(), commentDto, booker.getId());
        assertNotNull(comment);

        CommentDto strangerCommentDto = new CommentDto();
        strangerCommentDto.setText("I want to comment");

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> itemService.addComment(item.getId(), strangerCommentDto, stranger.getId())
        );
        assertTrue(exception.getMessage().contains("не брал эту вещь"));
    }
}