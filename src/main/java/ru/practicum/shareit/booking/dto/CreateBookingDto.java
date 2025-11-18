package ru.practicum.shareit.booking.dto;

import lombok.Data;

@Data
public class CreateBookingDto {
    private Long itemId;
    private String start;
    private String end;
}