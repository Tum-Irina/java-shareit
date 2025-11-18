package ru.practicum.shareit.booking.dto;

import lombok.Data;
import ru.practicum.shareit.booking.model.BookingStatus;

/**
 * TODO Sprint add-bookings.
 */

@Data
public class BookingDto {
    private Long id;
    private String start;
    private String end;
    private ItemInfo item;
    private BookerInfo booker;
    private BookingStatus status;

    @Data
    public static class ItemInfo {
        private Long id;
        private String name;
    }

    @Data
    public static class BookerInfo {
        private Long id;
        private String name;
    }
}