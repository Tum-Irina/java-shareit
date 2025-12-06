package ru.practicum.shareit.booking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.CreateBookingDto;
import ru.practicum.shareit.booking.mapper.BookingMapper;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public BookingDto createBooking(CreateBookingDto bookingDto, Long userId) {
        User booker = findUserOrThrow(userId);
        Item item = findItemOrThrow(bookingDto.getItemId());
        LocalDateTime start = LocalDateTime.parse(bookingDto.getStart());
        LocalDateTime end = LocalDateTime.parse(bookingDto.getEnd());

        if (item.getOwner().getId().equals(userId)) {
            throw new NotFoundException("Нельзя бронировать свою вещь");
        }
        if (!item.getAvailable()) {
            throw new ValidationException("Вещь недоступна для бронирования");
        }

        Booking booking = new Booking();
        booking.setStart(start);
        booking.setEnd(end);
        booking.setItem(item);
        booking.setBooker(booker);
        booking.setStatus(BookingStatus.WAITING);

        Booking saved = bookingRepository.save(booking);
        return BookingMapper.toDto(saved);
    }

    @Override
    @Transactional
    public BookingDto approveBooking(Long bookingId, Boolean approved, Long userId) {
        Booking booking = findBookingOrThrow(bookingId);

        if (!booking.getItem().getOwner().getId().equals(userId)) {
            throw new ValidationException("Только владелец вещи может подтвердить бронирование");
        }
        if (booking.getStatus() != BookingStatus.WAITING) {
            throw new ValidationException("Бронирование уже обработано");
        }

        booking.setStatus(approved ? BookingStatus.APPROVED : BookingStatus.REJECTED);
        Booking updated = bookingRepository.save(booking);
        return BookingMapper.toDto(updated);
    }

    @Override
    public BookingDto getBookingById(Long bookingId, Long userId) {
        Booking booking = findBookingOrThrow(bookingId);

        boolean isBooker = booking.getBooker().getId().equals(userId);
        boolean isOwner = booking.getItem().getOwner().getId().equals(userId);

        if (!isBooker && !isOwner) {
            throw new NotFoundException("Доступ запрещен");
        }

        return BookingMapper.toDto(booking);
    }

    @Override
    public List<BookingDto> getUserBookings(Long userId, BookingState state) {
        findUserOrThrow(userId);
        LocalDateTime now = LocalDateTime.now();

        switch (state) {
            case ALL: return bookingRepository.findByBookerIdOrderByStartDesc(userId).stream()
                    .map(BookingMapper::toDto).collect(Collectors.toList());
            case CURRENT: return filterCurrentBookings(
                    bookingRepository.findByBookerIdOrderByStartDesc(userId), now);
            case PAST: return bookingRepository.findByBookerIdAndEndBeforeOrderByStartDesc(userId, now).stream()
                    .map(BookingMapper::toDto).collect(Collectors.toList());
            case FUTURE: return bookingRepository.findByBookerIdAndStartAfterOrderByStartDesc(userId, now).stream()
                    .map(BookingMapper::toDto).collect(Collectors.toList());
            case WAITING: return bookingRepository.findByBookerIdAndStatusOrderByStartDesc(userId, BookingStatus.WAITING).stream()
                    .map(BookingMapper::toDto).collect(Collectors.toList());
            case REJECTED: return bookingRepository.findByBookerIdAndStatusOrderByStartDesc(userId, BookingStatus.REJECTED).stream()
                    .map(BookingMapper::toDto).collect(Collectors.toList());
            default: throw new ValidationException("Unknown state: " + state);
        }
    }

    @Override
    public List<BookingDto> getOwnerBookings(Long userId, BookingState state) {
        findUserOrThrow(userId);
        LocalDateTime now = LocalDateTime.now();

        switch (state) {
            case ALL: return bookingRepository.findByItemOwnerIdOrderByStartDesc(userId).stream()
                    .map(BookingMapper::toDto).collect(Collectors.toList());
            case PAST: return bookingRepository.findByItemOwnerIdAndEndBeforeOrderByStartDesc(userId, now).stream()
                    .map(BookingMapper::toDto).collect(Collectors.toList());
            case FUTURE: return bookingRepository.findByItemOwnerIdAndStartAfterOrderByStartDesc(userId, now).stream()
                    .map(BookingMapper::toDto).collect(Collectors.toList());
            case WAITING: return bookingRepository.findByItemOwnerIdAndStatusOrderByStartDesc(userId, BookingStatus.WAITING).stream()
                    .map(BookingMapper::toDto).collect(Collectors.toList());
            case REJECTED: return bookingRepository.findByItemOwnerIdAndStatusOrderByStartDesc(userId, BookingStatus.REJECTED).stream()
                    .map(BookingMapper::toDto).collect(Collectors.toList());
            default: throw new ValidationException("Unknown state: " + state);
        }
    }

    private List<BookingDto> filterCurrentBookings(List<Booking> bookings, LocalDateTime now) {
        return bookings.stream()
                .filter(b -> b.getStart().isBefore(now) && b.getEnd().isAfter(now))
                .map(BookingMapper::toDto)
                .collect(Collectors.toList());
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
    }

    private Item findItemOrThrow(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь не найдена"));
    }

    private Booking findBookingOrThrow(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Бронирование не найдено"));
    }
}