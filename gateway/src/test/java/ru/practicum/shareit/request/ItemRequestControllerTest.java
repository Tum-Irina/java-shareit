package ru.practicum.shareit.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.request.dto.CreateItemRequestDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ItemRequestController.class)
class ItemRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ItemRequestClient requestClient;

    private CreateItemRequestDto createRequestDto;

    @BeforeEach
    void setUp() {
        createRequestDto = new CreateItemRequestDto();
        createRequestDto.setDescription("Need a drill for repairs");
    }

    @Test
    void createRequest_whenValid_thenReturnOk() throws Exception {
        mockMvc.perform(post("/requests")
                        .header("X-Sharer-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequestDto)))
                .andExpect(status().isOk());

        verify(requestClient, times(1)).createRequest(eq(1L), any(CreateItemRequestDto.class));
    }

    @Test
    void createRequest_whenMissingUserIdHeader_thenReturnBadRequest() throws Exception {
        mockMvc.perform(post("/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequestDto)))
                .andExpect(status().isBadRequest());

        verify(requestClient, never()).createRequest(anyLong(), any(CreateItemRequestDto.class));
    }

    @Test
    void createRequest_whenInvalidRequest_thenReturnBadRequest() throws Exception {
        CreateItemRequestDto invalidDto = new CreateItemRequestDto();
        invalidDto.setDescription("");

        mockMvc.perform(post("/requests")
                        .header("X-Sharer-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());

        verify(requestClient, never()).createRequest(anyLong(), any(CreateItemRequestDto.class));
    }

    @Test
    void getUserRequests_whenValid_thenReturnOk() throws Exception {
        mockMvc.perform(get("/requests")
                        .header("X-Sharer-User-Id", 1L))
                .andExpect(status().isOk());

        verify(requestClient, times(1)).getUserRequests(1L);
    }

    @Test
    void getAllRequests_whenValidPagination_thenReturnOk() throws Exception {
        mockMvc.perform(get("/requests/all")
                        .header("X-Sharer-User-Id", 1L)
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());

        verify(requestClient, times(1)).getAllRequests(eq(1L), eq(0), eq(10));
    }

    @Test
    void getAllRequests_whenDefaultPagination_thenReturnOk() throws Exception {
        mockMvc.perform(get("/requests/all")
                        .header("X-Sharer-User-Id", 1L))
                .andExpect(status().isOk());

        verify(requestClient, times(1)).getAllRequests(eq(1L), eq(0), eq(10));
    }

    @Test
    void getRequestById_whenValid_thenReturnOk() throws Exception {
        mockMvc.perform(get("/requests/{requestId}", 1L)
                        .header("X-Sharer-User-Id", 1L))
                .andExpect(status().isOk());

        verify(requestClient, times(1)).getRequestById(eq(1L), eq(1L));
    }
}