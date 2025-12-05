package ru.practicum.shareit.booking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.CreateBookingDto;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingState;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.model.Item;
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
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private User owner;
    private User booker;
    private Item item;
    private Booking booking;
    private CreateBookingDto createBookingDto;

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

        booking = new Booking();
        booking.setId(1L);
        booking.setStart(LocalDateTime.now().plusDays(1));
        booking.setEnd(LocalDateTime.now().plusDays(2));
        booking.setItem(item);
        booking.setBooker(booker);
        booking.setStatus(BookingStatus.WAITING);

        createBookingDto = new CreateBookingDto();
        createBookingDto.setItemId(item.getId());
        createBookingDto.setStart(LocalDateTime.now().plusDays(1).toString());
        createBookingDto.setEnd(LocalDateTime.now().plusDays(2).toString());
    }

    @Test
    void createBooking_whenValid_thenBookingCreated() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(booker));
        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        BookingDto result = bookingService.createBooking(createBookingDto, booker.getId());

        assertNotNull(result);
        assertEquals(booking.getId(), result.getId());
        assertEquals(booking.getStatus(), result.getStatus());
        verify(userRepository).findById(booker.getId());
        verify(itemRepository).findById(item.getId());
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void createBooking_whenBookOwnItem_thenThrowNotFoundException() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(owner));
        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> bookingService.createBooking(createBookingDto, owner.getId())
        );

        assertEquals("Нельзя бронировать свою вещь", exception.getMessage());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void createBooking_whenItemNotAvailable_thenThrowValidationException() {
        item.setAvailable(false);
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(booker));
        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> bookingService.createBooking(createBookingDto, booker.getId())
        );

        assertEquals("Вещь недоступна для бронирования", exception.getMessage());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void createBooking_whenUserNotFound_thenThrowNotFoundException() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> bookingService.createBooking(createBookingDto, 999L)
        );

        assertEquals("Пользователь не найден", exception.getMessage());
        verify(userRepository).findById(999L);
        verify(itemRepository, never()).findById(anyLong());
    }

    @Test
    void createBooking_whenItemNotFound_thenThrowNotFoundException() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(booker));
        when(itemRepository.findById(anyLong())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> bookingService.createBooking(createBookingDto, booker.getId())
        );

        assertEquals("Вещь не найдена", exception.getMessage());
        verify(itemRepository).findById(item.getId());
    }

    @Test
    void approveBooking_whenValidApprove_thenBookingApproved() {
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        BookingDto result = bookingService.approveBooking(booking.getId(), true, owner.getId());

        assertNotNull(result);
        assertEquals(BookingStatus.APPROVED, result.getStatus());
        verify(bookingRepository).findById(booking.getId());
        verify(bookingRepository).save(booking);
    }

    @Test
    void approveBooking_whenValidReject_thenBookingRejected() {
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        BookingDto result = bookingService.approveBooking(booking.getId(), false, owner.getId());

        assertNotNull(result);
        assertEquals(BookingStatus.REJECTED, result.getStatus());
        verify(bookingRepository).findById(booking.getId());
        verify(bookingRepository).save(booking);
    }

    @Test
    void approveBooking_whenNotOwner_thenThrowValidationException() {
        Long notOwnerId = 999L;
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.of(booking));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> bookingService.approveBooking(booking.getId(), true, notOwnerId)
        );

        assertEquals("Только владелец вещи может подтвердить бронирование", exception.getMessage());
        verify(bookingRepository).findById(booking.getId());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void approveBooking_whenNotWaitingStatus_thenThrowValidationException() {
        booking.setStatus(BookingStatus.APPROVED);
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.of(booking));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> bookingService.approveBooking(booking.getId(), true, owner.getId())
        );

        assertEquals("Бронирование уже обработано", exception.getMessage());
        verify(bookingRepository).findById(booking.getId());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void approveBooking_whenBookingNotFound_thenThrowNotFoundException() {
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> bookingService.approveBooking(999L, true, owner.getId())
        );

        assertEquals("Бронирование не найдено", exception.getMessage());
        verify(bookingRepository).findById(999L);
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void getBookingById_whenBookerRequest_thenReturnBooking() {
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.of(booking));

        BookingDto result = bookingService.getBookingById(booking.getId(), booker.getId());

        assertNotNull(result);
        assertEquals(booking.getId(), result.getId());
        verify(bookingRepository).findById(booking.getId());
    }

    @Test
    void getBookingById_whenOwnerRequest_thenReturnBooking() {
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.of(booking));

        BookingDto result = bookingService.getBookingById(booking.getId(), owner.getId());

        assertNotNull(result);
        assertEquals(booking.getId(), result.getId());
        verify(bookingRepository).findById(booking.getId());
    }

    @Test
    void getBookingById_whenNotBookerOrOwner_thenThrowNotFoundException() {
        Long strangerId = 999L;
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.of(booking));

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> bookingService.getBookingById(booking.getId(), strangerId)
        );

        assertEquals("Доступ запрещен", exception.getMessage());
        verify(bookingRepository).findById(booking.getId());
    }

    @Test
    void getBookingById_whenBookingNotFound_thenThrowNotFoundException() {
        when(bookingRepository.findById(anyLong())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> bookingService.getBookingById(999L, booker.getId())
        );

        assertEquals("Бронирование не найдено", exception.getMessage());
        verify(bookingRepository).findById(999L);
    }

    @Test
    void getUserBookings_whenAllState_thenReturnAllBookings() {
        List<Booking> bookings = List.of(booking);
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(booker));
        when(bookingRepository.findByBookerIdOrderByStartDesc(anyLong())).thenReturn(bookings);

        List<BookingDto> result = bookingService.getUserBookings(booker.getId(), BookingState.ALL);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userRepository).findById(booker.getId());
        verify(bookingRepository).findByBookerIdOrderByStartDesc(booker.getId());
    }

    @Test
    void getUserBookings_whenCurrentState_thenReturnCurrentBookings() {
        Booking currentBooking = new Booking();
        currentBooking.setId(2L);
        currentBooking.setStart(LocalDateTime.now().minusDays(1));
        currentBooking.setEnd(LocalDateTime.now().plusDays(1));
        currentBooking.setItem(item);
        currentBooking.setBooker(booker);
        currentBooking.setStatus(BookingStatus.APPROVED);

        List<Booking> allBookings = List.of(currentBooking, booking);

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(booker));
        when(bookingRepository.findByBookerIdOrderByStartDesc(anyLong())).thenReturn(allBookings);

        List<BookingDto> result = bookingService.getUserBookings(booker.getId(), BookingState.CURRENT);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(currentBooking.getId(), result.get(0).getId());
    }

    @Test
    void getUserBookings_whenFutureState_thenReturnFutureBookings() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(booker));
        when(bookingRepository.findByBookerIdAndStartAfterOrderByStartDesc(anyLong(), any(LocalDateTime.class)))
                .thenReturn(List.of(booking));

        List<BookingDto> result = bookingService.getUserBookings(booker.getId(), BookingState.FUTURE);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(booking.getId(), result.get(0).getId());
    }

    @Test
    void getUserBookings_whenWaitingState_thenReturnWaitingBookings() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(booker));
        when(bookingRepository.findByBookerIdAndStatusOrderByStartDesc(anyLong(), eq(BookingStatus.WAITING)))
                .thenReturn(List.of(booking));

        List<BookingDto> result = bookingService.getUserBookings(booker.getId(), BookingState.WAITING);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(booking.getId(), result.get(0).getId());
    }

    @Test
    void getUserBookings_whenRejectedState_thenReturnRejectedBookings() {
        booking.setStatus(BookingStatus.REJECTED);
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(booker));
        when(bookingRepository.findByBookerIdAndStatusOrderByStartDesc(anyLong(), eq(BookingStatus.REJECTED)))
                .thenReturn(List.of(booking));

        List<BookingDto> result = bookingService.getUserBookings(booker.getId(), BookingState.REJECTED);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(booking.getId(), result.get(0).getId());
    }

    @Test
    void getUserBookings_whenUserNotFound_thenThrowNotFoundException() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> bookingService.getUserBookings(999L, BookingState.ALL)
        );

        assertEquals("Пользователь не найден", exception.getMessage());
        verify(userRepository).findById(999L);
    }

    @Test
    void getOwnerBookings_whenAllState_thenReturnAllBookings() {
        List<Booking> bookings = List.of(booking);
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(owner));
        when(bookingRepository.findByItemOwnerIdOrderByStartDesc(anyLong())).thenReturn(bookings);

        List<BookingDto> result = bookingService.getOwnerBookings(owner.getId(), BookingState.ALL);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userRepository).findById(owner.getId());
        verify(bookingRepository).findByItemOwnerIdOrderByStartDesc(owner.getId());
    }

    @Test
    void getOwnerBookings_whenUserNotFound_thenThrowNotFoundException() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> bookingService.getOwnerBookings(999L, BookingState.ALL)
        );

        assertEquals("Пользователь не найден", exception.getMessage());
        verify(userRepository).findById(999L);
    }
}