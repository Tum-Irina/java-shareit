package ru.practicum.shareit.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.CreateBookingDto;
import ru.practicum.shareit.booking.model.BookingState;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BookingController.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;

    private CreateBookingDto createBookingDto;
    private BookingDto bookingDto;

    @BeforeEach
    void setUp() {
        createBookingDto = new CreateBookingDto();
        createBookingDto.setItemId(1L);
        createBookingDto.setStart(LocalDateTime.now().plusDays(1).toString());
        createBookingDto.setEnd(LocalDateTime.now().plusDays(2).toString());

        bookingDto = new BookingDto();
        bookingDto.setId(1L);
        bookingDto.setStart(createBookingDto.getStart());
        bookingDto.setEnd(createBookingDto.getEnd());
        bookingDto.setStatus(BookingStatus.WAITING);

        BookingDto.ItemInfo itemInfo = new BookingDto.ItemInfo();
        itemInfo.setId(1L);
        itemInfo.setName("Дрель");
        bookingDto.setItem(itemInfo);

        BookingDto.BookerInfo bookerInfo = new BookingDto.BookerInfo();
        bookerInfo.setId(2L);
        bookerInfo.setName("Booker");
        bookingDto.setBooker(bookerInfo);
    }

    @Test
    void createBooking_whenValid_thenReturnCreatedBooking() throws Exception {
        when(bookingService.createBooking(any(CreateBookingDto.class), anyLong())).thenReturn(bookingDto);

        mockMvc.perform(post("/bookings")
                        .header("X-Sharer-User-Id", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBookingDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(bookingDto.getId().intValue())))
                .andExpect(jsonPath("$.status", is(bookingDto.getStatus().toString())))
                .andExpect(jsonPath("$.item.id", is(bookingDto.getItem().getId().intValue())))
                .andExpect(jsonPath("$.booker.id", is(bookingDto.getBooker().getId().intValue())));

        verify(bookingService, times(1)).createBooking(any(CreateBookingDto.class), eq(2L));
    }

    @Test
    void createBooking_whenMissingUserIdHeader_thenReturnBadRequest() throws Exception {
        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBookingDto)))
                .andExpect(status().isBadRequest());

        verify(bookingService, never()).createBooking(any(CreateBookingDto.class), anyLong());
    }

    @Test
    void createBooking_whenItemNotFound_thenReturnNotFound() throws Exception {
        when(bookingService.createBooking(any(CreateBookingDto.class), anyLong()))
                .thenThrow(new NotFoundException("Вещь не найдена"));

        mockMvc.perform(post("/bookings")
                        .header("X-Sharer-User-Id", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBookingDto)))
                .andExpect(status().isNotFound());

        verify(bookingService, times(1)).createBooking(any(CreateBookingDto.class), eq(2L));
    }

    @Test
    void createBooking_whenValidationError_thenReturnBadRequest() throws Exception {
        when(bookingService.createBooking(any(CreateBookingDto.class), anyLong()))
                .thenThrow(new ValidationException("Вещь недоступна для бронирования"));

        mockMvc.perform(post("/bookings")
                        .header("X-Sharer-User-Id", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBookingDto)))
                .andExpect(status().isBadRequest());

        verify(bookingService, times(1)).createBooking(any(CreateBookingDto.class), eq(2L));
    }

    @Test
    void approveBooking_whenApprove_thenReturnApprovedBooking() throws Exception {
        bookingDto.setStatus(BookingStatus.APPROVED);
        when(bookingService.approveBooking(anyLong(), anyBoolean(), anyLong())).thenReturn(bookingDto);

        mockMvc.perform(patch("/bookings/{bookingId}", 1L)
                        .header("X-Sharer-User-Id", 1L)
                        .param("approved", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")));

        verify(bookingService, times(1)).approveBooking(eq(1L), eq(true), eq(1L));
    }

    @Test
    void approveBooking_whenReject_thenReturnRejectedBooking() throws Exception {
        bookingDto.setStatus(BookingStatus.REJECTED);
        when(bookingService.approveBooking(anyLong(), anyBoolean(), anyLong())).thenReturn(bookingDto);

        mockMvc.perform(patch("/bookings/{bookingId}", 1L)
                        .header("X-Sharer-User-Id", 1L)
                        .param("approved", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REJECTED")));

        verify(bookingService, times(1)).approveBooking(eq(1L), eq(false), eq(1L));
    }

    @Test
    void approveBooking_whenMissingUserIdHeader_thenReturnBadRequest() throws Exception {
        mockMvc.perform(patch("/bookings/{bookingId}", 1L)
                        .param("approved", "true"))
                .andExpect(status().isBadRequest());

        verify(bookingService, never()).approveBooking(anyLong(), anyBoolean(), anyLong());
    }

    @Test
    void approveBooking_whenMissingApprovedParam_thenReturnBadRequest() throws Exception {
        mockMvc.perform(patch("/bookings/{bookingId}", 1L)
                        .header("X-Sharer-User-Id", 1L))
                .andExpect(status().isBadRequest());

        verify(bookingService, never()).approveBooking(anyLong(), anyBoolean(), anyLong());
    }

    @Test
    void getBooking_whenExists_thenReturnBooking() throws Exception {
        when(bookingService.getBookingById(anyLong(), anyLong())).thenReturn(bookingDto);

        mockMvc.perform(get("/bookings/{bookingId}", 1L)
                        .header("X-Sharer-User-Id", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(bookingDto.getId().intValue())));

        verify(bookingService, times(1)).getBookingById(eq(1L), eq(2L));
    }

    @Test
    void getBooking_whenNotFound_thenReturnNotFound() throws Exception {
        when(bookingService.getBookingById(anyLong(), anyLong()))
                .thenThrow(new NotFoundException("Бронирование не найдено"));

        mockMvc.perform(get("/bookings/{bookingId}", 999L)
                        .header("X-Sharer-User-Id", 2L))
                .andExpect(status().isNotFound());

        verify(bookingService, times(1)).getBookingById(eq(999L), eq(2L));
    }

    @Test
    void getBooking_whenAccessDenied_thenReturnNotFound() throws Exception {
        when(bookingService.getBookingById(anyLong(), anyLong()))
                .thenThrow(new NotFoundException("Доступ запрещен"));

        mockMvc.perform(get("/bookings/{bookingId}", 1L)
                        .header("X-Sharer-User-Id", 999L))
                .andExpect(status().isNotFound());

        verify(bookingService, times(1)).getBookingById(eq(1L), eq(999L));
    }

    @Test
    void getUserBookings_whenAllState_thenReturnBookings() throws Exception {
        List<BookingDto> bookings = List.of(bookingDto);
        when(bookingService.getUserBookings(anyLong(), any(BookingState.class))).thenReturn(bookings);

        mockMvc.perform(get("/bookings")
                        .header("X-Sharer-User-Id", 2L)
                        .param("state", "ALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(bookingService, times(1)).getUserBookings(eq(2L), eq(BookingState.ALL));
    }

    @Test
    void getUserBookings_whenDefaultState_thenReturnAllBookings() throws Exception {
        List<BookingDto> bookings = List.of(bookingDto);
        when(bookingService.getUserBookings(anyLong(), eq(BookingState.ALL))).thenReturn(bookings);

        mockMvc.perform(get("/bookings")
                        .header("X-Sharer-User-Id", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(bookingService, times(1)).getUserBookings(eq(2L), eq(BookingState.ALL));
    }

    @Test
    void getUserBookings_whenCurrentState_thenReturnCurrentBookings() throws Exception {
        List<BookingDto> bookings = List.of(bookingDto);
        when(bookingService.getUserBookings(anyLong(), eq(BookingState.CURRENT))).thenReturn(bookings);

        mockMvc.perform(get("/bookings")
                        .header("X-Sharer-User-Id", 2L)
                        .param("state", "CURRENT"))
                .andExpect(status().isOk());

        verify(bookingService, times(1)).getUserBookings(eq(2L), eq(BookingState.CURRENT));
    }

    @Test
    void getOwnerBookings_whenAllState_thenReturnBookings() throws Exception {
        List<BookingDto> bookings = List.of(bookingDto);
        when(bookingService.getOwnerBookings(anyLong(), any(BookingState.class))).thenReturn(bookings);

        mockMvc.perform(get("/bookings/owner")
                        .header("X-Sharer-User-Id", 1L)
                        .param("state", "ALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(bookingService, times(1)).getOwnerBookings(eq(1L), eq(BookingState.ALL));
    }

    @Test
    void getOwnerBookings_whenDefaultState_thenReturnAllBookings() throws Exception {
        List<BookingDto> bookings = List.of(bookingDto);
        when(bookingService.getOwnerBookings(anyLong(), eq(BookingState.ALL))).thenReturn(bookings);

        mockMvc.perform(get("/bookings/owner")
                        .header("X-Sharer-User-Id", 1L))
                .andExpect(status().isOk());

        verify(bookingService, times(1)).getOwnerBookings(eq(1L), eq(BookingState.ALL));
    }

    @Test
    void getUserBookings_whenMissingUserIdHeader_thenReturnBadRequest() throws Exception {
        mockMvc.perform(get("/bookings")
                        .param("state", "ALL"))
                .andExpect(status().isBadRequest());

        verify(bookingService, never()).getUserBookings(anyLong(), any(BookingState.class));
    }

    @Test
    void getOwnerBookings_whenMissingUserIdHeader_thenReturnBadRequest() throws Exception {
        mockMvc.perform(get("/bookings/owner")
                        .param("state", "ALL"))
                .andExpect(status().isBadRequest());

        verify(bookingService, never()).getOwnerBookings(anyLong(), any(BookingState.class));
    }
}