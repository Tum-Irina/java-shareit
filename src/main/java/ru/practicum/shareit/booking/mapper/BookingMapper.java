package ru.practicum.shareit.booking.mapper;

import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.model.Booking;

import java.time.format.DateTimeFormatter;

public class BookingMapper {

    public static BookingDto toDto(Booking booking) {
        BookingDto dto = new BookingDto();
        dto.setId(booking.getId());
        dto.setStart(booking.getStart().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        dto.setEnd(booking.getEnd().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        dto.setStatus(booking.getStatus());

        BookingDto.ItemInfo itemInfo = new BookingDto.ItemInfo();
        itemInfo.setId(booking.getItem().getId());
        itemInfo.setName(booking.getItem().getName());
        dto.setItem(itemInfo);

        BookingDto.BookerInfo bookerInfo = new BookingDto.BookerInfo();
        bookerInfo.setId(booking.getBooker().getId());
        bookerInfo.setName(booking.getBooker().getName());
        dto.setBooker(bookerInfo);

        return dto;
    }
}