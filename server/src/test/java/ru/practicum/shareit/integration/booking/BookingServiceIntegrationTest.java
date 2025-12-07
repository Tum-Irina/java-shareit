package ru.practicum.shareit.integration.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.CreateBookingDto;
import ru.practicum.shareit.booking.model.BookingState;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.service.UserService;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BookingServiceIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private UserService userService;

    private Long ownerId;
    private Long bookerId;
    private Long itemId;

    @BeforeEach
    void setUp() {
        UserDto ownerDto = new UserDto();
        ownerDto.setName("Booking Owner");
        ownerDto.setEmail("owner@booking.com");
        UserDto createdOwner = userService.createUser(ownerDto);
        ownerId = createdOwner.getId();

        UserDto bookerDto = new UserDto();
        bookerDto.setName("Booker");
        bookerDto.setEmail("booker@booking.com");
        UserDto createdBooker = userService.createUser(bookerDto);
        bookerId = createdBooker.getId();

        ItemDto itemDto = new ItemDto();
        itemDto.setName("Bookable Item");
        itemDto.setDescription("Item for booking tests");
        itemDto.setAvailable(true);
        ItemDto createdItem = itemService.createItem(itemDto, ownerId);
        itemId = createdItem.getId();
    }

    @Test
    void createBooking_whenValid_thenBookingCreated() {
        CreateBookingDto bookingDto = new CreateBookingDto();
        bookingDto.setItemId(itemId);
        bookingDto.setStart(LocalDateTime.now().plusDays(1).toString());
        bookingDto.setEnd(LocalDateTime.now().plusDays(2).toString());

        BookingDto createdBooking = bookingService.createBooking(bookingDto, bookerId);

        assertNotNull(createdBooking.getId());
        assertEquals(BookingStatus.WAITING, createdBooking.getStatus());
        assertEquals(itemId, createdBooking.getItem().getId());
        assertEquals(bookerId, createdBooking.getBooker().getId());
    }

    @Test
    void createBooking_whenBookOwnItem_thenThrowNotFoundException() {
        CreateBookingDto bookingDto = new CreateBookingDto();
        bookingDto.setItemId(itemId);
        bookingDto.setStart(LocalDateTime.now().plusDays(1).toString());
        bookingDto.setEnd(LocalDateTime.now().plusDays(2).toString());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> bookingService.createBooking(bookingDto, ownerId)
        );

        assertTrue(exception.getMessage().contains("Нельзя бронировать свою вещь"));
    }

    @Test
    void createBooking_whenItemNotAvailable_thenThrowValidationException() {
        ItemDto unavailableItemDto = new ItemDto();
        unavailableItemDto.setName("Unavailable Item");
        unavailableItemDto.setDescription("Not available for booking");
        unavailableItemDto.setAvailable(false);
        ItemDto createdItem = itemService.createItem(unavailableItemDto, ownerId);

        CreateBookingDto bookingDto = new CreateBookingDto();
        bookingDto.setItemId(createdItem.getId());
        bookingDto.setStart(LocalDateTime.now().plusDays(1).toString());
        bookingDto.setEnd(LocalDateTime.now().plusDays(2).toString());

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> bookingService.createBooking(bookingDto, bookerId)
        );

        assertTrue(exception.getMessage().contains("недоступна для бронирования"));
    }

    @Test
    void approveBooking_whenApprove_thenBookingApproved() {
        CreateBookingDto createDto = new CreateBookingDto();
        createDto.setItemId(itemId);
        createDto.setStart(LocalDateTime.now().plusDays(1).toString());
        createDto.setEnd(LocalDateTime.now().plusDays(2).toString());
        BookingDto createdBooking = bookingService.createBooking(createDto, bookerId);

        BookingDto approvedBooking = bookingService.approveBooking(createdBooking.getId(), true, ownerId);

        assertEquals(BookingStatus.APPROVED, approvedBooking.getStatus());
        assertEquals(createdBooking.getId(), approvedBooking.getId());
    }

    @Test
    void approveBooking_whenReject_thenBookingRejected() {
        CreateBookingDto createDto = new CreateBookingDto();
        createDto.setItemId(itemId);
        createDto.setStart(LocalDateTime.now().plusDays(1).toString());
        createDto.setEnd(LocalDateTime.now().plusDays(2).toString());
        BookingDto createdBooking = bookingService.createBooking(createDto, bookerId);

        BookingDto rejectedBooking = bookingService.approveBooking(createdBooking.getId(), false, ownerId);

        assertEquals(BookingStatus.REJECTED, rejectedBooking.getStatus());
    }

    @Test
    void approveBooking_whenNotOwner_thenThrowValidationException() {
        CreateBookingDto createDto = new CreateBookingDto();
        createDto.setItemId(itemId);
        createDto.setStart(LocalDateTime.now().plusDays(1).toString());
        createDto.setEnd(LocalDateTime.now().plusDays(2).toString());
        BookingDto createdBooking = bookingService.createBooking(createDto, bookerId);

        UserDto anotherUser = new UserDto();
        anotherUser.setName("Another User");
        anotherUser.setEmail("another@example.com");
        UserDto createdAnother = userService.createUser(anotherUser);

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> bookingService.approveBooking(createdBooking.getId(), true, createdAnother.getId())
        );

        assertTrue(exception.getMessage().contains("Только владелец вещи"));
    }

    @Test
    void getBookingById_whenBookerRequest_thenReturnBooking() {
        CreateBookingDto createDto = new CreateBookingDto();
        createDto.setItemId(itemId);
        createDto.setStart(LocalDateTime.now().plusDays(1).toString());
        createDto.setEnd(LocalDateTime.now().plusDays(2).toString());
        BookingDto createdBooking = bookingService.createBooking(createDto, bookerId);

        BookingDto foundBooking = bookingService.getBookingById(createdBooking.getId(), bookerId);

        assertEquals(createdBooking.getId(), foundBooking.getId());
        assertEquals(createdBooking.getStatus(), foundBooking.getStatus());
    }

    @Test
    void getBookingById_whenOwnerRequest_thenReturnBooking() {
        CreateBookingDto createDto = new CreateBookingDto();
        createDto.setItemId(itemId);
        createDto.setStart(LocalDateTime.now().plusDays(1).toString());
        createDto.setEnd(LocalDateTime.now().plusDays(2).toString());
        BookingDto createdBooking = bookingService.createBooking(createDto, bookerId);

        BookingDto foundBooking = bookingService.getBookingById(createdBooking.getId(), ownerId);

        assertEquals(createdBooking.getId(), foundBooking.getId());
    }

    @Test
    void getBookingById_whenNotBookerOrOwner_thenThrowNotFoundException() {
        CreateBookingDto createDto = new CreateBookingDto();
        createDto.setItemId(itemId);
        createDto.setStart(LocalDateTime.now().plusDays(1).toString());
        createDto.setEnd(LocalDateTime.now().plusDays(2).toString());
        BookingDto createdBooking = bookingService.createBooking(createDto, bookerId);

        UserDto stranger = new UserDto();
        stranger.setName("Stranger");
        stranger.setEmail("stranger@example.com");
        UserDto createdStranger = userService.createUser(stranger);

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> bookingService.getBookingById(createdBooking.getId(), createdStranger.getId())
        );

        assertTrue(exception.getMessage().contains("Доступ запрещен"));
    }

    @Test
    void getUserBookings_whenAllState_thenReturnAllBookings() {
        for (int i = 1; i <= 3; i++) {
            CreateBookingDto createDto = new CreateBookingDto();
            createDto.setItemId(itemId);
            createDto.setStart(LocalDateTime.now().plusDays(i).toString());
            createDto.setEnd(LocalDateTime.now().plusDays(i + 1).toString());
            bookingService.createBooking(createDto, bookerId);
        }

        List<BookingDto> bookings = bookingService.getUserBookings(bookerId, BookingState.ALL);

        assertEquals(3, bookings.size());
    }

    @Test
    void getUserBookings_whenWaitingState_thenReturnWaitingBookings() {
        CreateBookingDto waitingDto = new CreateBookingDto();
        waitingDto.setItemId(itemId);
        waitingDto.setStart(LocalDateTime.now().plusDays(1).toString());
        waitingDto.setEnd(LocalDateTime.now().plusDays(2).toString());
        BookingDto waitingBooking = bookingService.createBooking(waitingDto, bookerId);

        CreateBookingDto approvedDto = new CreateBookingDto();
        approvedDto.setItemId(itemId);
        approvedDto.setStart(LocalDateTime.now().plusDays(3).toString());
        approvedDto.setEnd(LocalDateTime.now().plusDays(4).toString());
        BookingDto approvedBooking = bookingService.createBooking(approvedDto, bookerId);
        bookingService.approveBooking(approvedBooking.getId(), true, ownerId);

        List<BookingDto> waitingBookings = bookingService.getUserBookings(bookerId, BookingState.WAITING);

        assertEquals(1, waitingBookings.size());
        assertEquals(BookingStatus.WAITING, waitingBookings.get(0).getStatus());
    }

    @Test
    void getOwnerBookings_whenAllState_thenReturnAllBookings() {
        for (int i = 1; i <= 3; i++) {
            CreateBookingDto createDto = new CreateBookingDto();
            createDto.setItemId(itemId);
            createDto.setStart(LocalDateTime.now().plusDays(i).toString());
            createDto.setEnd(LocalDateTime.now().plusDays(i + 1).toString());
            bookingService.createBooking(createDto, bookerId);
        }

        List<BookingDto> bookings = bookingService.getOwnerBookings(ownerId, BookingState.ALL);

        assertEquals(3, bookings.size());
    }

    @Test
    void testBookingStateFilters() {
        ItemDto pastItem = new ItemDto();
        pastItem.setName("Past Item");
        pastItem.setDescription("For past booking");
        pastItem.setAvailable(true);
        ItemDto createdPastItem = itemService.createItem(pastItem, ownerId);

        ItemDto currentItem = new ItemDto();
        currentItem.setName("Current Item");
        currentItem.setDescription("For current booking");
        currentItem.setAvailable(true);
        ItemDto createdCurrentItem = itemService.createItem(currentItem, ownerId);

        ItemDto futureItem = new ItemDto();
        futureItem.setName("Future Item");
        futureItem.setDescription("For future booking");
        futureItem.setAvailable(true);
        ItemDto createdFutureItem = itemService.createItem(futureItem, ownerId);

        CreateBookingDto pastBookingDto = new CreateBookingDto();
        pastBookingDto.setItemId(createdPastItem.getId());
        pastBookingDto.setStart(LocalDateTime.now().minusDays(3).toString());
        pastBookingDto.setEnd(LocalDateTime.now().minusDays(1).toString());
        BookingDto pastBooking = bookingService.createBooking(pastBookingDto, bookerId);
        bookingService.approveBooking(pastBooking.getId(), true, ownerId);

        CreateBookingDto currentBookingDto = new CreateBookingDto();
        currentBookingDto.setItemId(createdCurrentItem.getId());
        currentBookingDto.setStart(LocalDateTime.now().minusDays(1).toString());
        currentBookingDto.setEnd(LocalDateTime.now().plusDays(1).toString());
        BookingDto currentBooking = bookingService.createBooking(currentBookingDto, bookerId);
        bookingService.approveBooking(currentBooking.getId(), true, ownerId);

        CreateBookingDto futureBookingDto = new CreateBookingDto();
        futureBookingDto.setItemId(createdFutureItem.getId());
        futureBookingDto.setStart(LocalDateTime.now().plusDays(1).toString());
        futureBookingDto.setEnd(LocalDateTime.now().plusDays(2).toString());
        BookingDto futureBooking = bookingService.createBooking(futureBookingDto, bookerId);
        bookingService.approveBooking(futureBooking.getId(), true, ownerId);

        List<BookingDto> pastBookings = bookingService.getUserBookings(bookerId, BookingState.PAST);
        assertEquals(1, pastBookings.size());

        List<BookingDto> futureBookings = bookingService.getUserBookings(bookerId, BookingState.FUTURE);
        assertEquals(1, futureBookings.size());

        List<BookingDto> currentBookings = bookingService.getUserBookings(bookerId, BookingState.CURRENT);
        assertEquals(1, currentBookings.size());
    }
}