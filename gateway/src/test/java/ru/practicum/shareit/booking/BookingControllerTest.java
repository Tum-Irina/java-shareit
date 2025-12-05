package ru.practicum.shareit.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.booking.dto.CreateBookingDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BookingController.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingClient bookingClient;

    private CreateBookingDto createBookingDto;

    @BeforeEach
    void setUp() {
        createBookingDto = new CreateBookingDto();
        createBookingDto.setItemId(1L);
        createBookingDto.setStart("2024-12-01T10:00:00");
        createBookingDto.setEnd("2024-12-02T10:00:00");
    }

    @Test
    void createBooking_whenValid_thenReturnOk() throws Exception {
        mockMvc.perform(post("/bookings")
                        .header("X-Sharer-User-Id", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBookingDto)))
                .andExpect(status().isOk());

        verify(bookingClient, times(1)).bookItem(eq(2L), any(CreateBookingDto.class));
    }

    @Test
    void createBooking_whenMissingUserIdHeader_thenReturnBadRequest() throws Exception {
        mockMvc.perform(post("/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBookingDto)))
                .andExpect(status().isBadRequest());

        verify(bookingClient, never()).bookItem(anyLong(), any(CreateBookingDto.class));
    }

    @Test
    void createBooking_whenInvalidBooking_thenReturnBadRequest() throws Exception {
        CreateBookingDto invalidDto = new CreateBookingDto();
        invalidDto.setItemId(null);
        invalidDto.setStart(null);
        invalidDto.setEnd(null);

        mockMvc.perform(post("/bookings")
                        .header("X-Sharer-User-Id", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());

        verify(bookingClient, never()).bookItem(anyLong(), any(CreateBookingDto.class));
    }

    @Test
    void approveBooking_whenValid_thenReturnOk() throws Exception {
        mockMvc.perform(patch("/bookings/{bookingId}", 1L)
                        .header("X-Sharer-User-Id", 1L)
                        .param("approved", "true"))
                .andExpect(status().isOk());

        verify(bookingClient, times(1)).approveBooking(eq(1L), eq(1L), eq(true));
    }

    @Test
    void approveBooking_whenMissingApprovedParam_thenReturnBadRequest() throws Exception {
        mockMvc.perform(patch("/bookings/{bookingId}", 1L)
                        .header("X-Sharer-User-Id", 1L))
                .andExpect(status().isBadRequest());

        verify(bookingClient, never()).approveBooking(anyLong(), anyLong(), anyBoolean());
    }

    @Test
    void getBooking_whenValid_thenReturnOk() throws Exception {
        mockMvc.perform(get("/bookings/{bookingId}", 1L)
                        .header("X-Sharer-User-Id", 2L))
                .andExpect(status().isOk());

        verify(bookingClient, times(1)).getBooking(eq(2L), eq(1L));
    }

    @Test
    void getUserBookings_whenValid_thenReturnOk() throws Exception {
        mockMvc.perform(get("/bookings")
                        .header("X-Sharer-User-Id", 2L)
                        .param("state", "ALL"))
                .andExpect(status().isOk());

        verify(bookingClient, times(1)).getBookings(eq(2L), any());
    }

    @Test
    void getUserBookings_whenDefaultState_thenReturnOk() throws Exception {
        mockMvc.perform(get("/bookings")
                        .header("X-Sharer-User-Id", 2L))
                .andExpect(status().isOk());

        verify(bookingClient, times(1)).getBookings(eq(2L), any());
    }

    @Test
    void getOwnerBookings_whenValid_thenReturnOk() throws Exception {
        mockMvc.perform(get("/bookings/owner")
                        .header("X-Sharer-User-Id", 1L)
                        .param("state", "ALL"))
                .andExpect(status().isOk());

        verify(bookingClient, times(1)).getOwnerBookings(eq(1L), any());
    }

    @Test
    void getOwnerBookings_whenMissingUserIdHeader_thenReturnBadRequest() throws Exception {
        mockMvc.perform(get("/bookings/owner")
                        .param("state", "ALL"))
                .andExpect(status().isBadRequest());

        verify(bookingClient, never()).getOwnerBookings(anyLong(), any());
    }
}