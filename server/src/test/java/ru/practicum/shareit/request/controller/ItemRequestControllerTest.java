package ru.practicum.shareit.request.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.request.dto.CreateItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestWithItemsDto;
import ru.practicum.shareit.request.service.ItemRequestService;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ItemRequestController.class)
class ItemRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ItemRequestService requestService;

    private CreateItemRequestDto createRequestDto;
    private ItemRequestDto itemRequestDto;
    private ItemRequestWithItemsDto requestWithItemsDto;

    @BeforeEach
    void setUp() {
        createRequestDto = new CreateItemRequestDto();
        createRequestDto.setDescription("Нужна дрель для ремонта");

        itemRequestDto = new ItemRequestDto();
        itemRequestDto.setId(1L);
        itemRequestDto.setDescription("Нужна дрель для ремонта");
        itemRequestDto.setCreated(LocalDateTime.now());

        requestWithItemsDto = new ItemRequestWithItemsDto();
        requestWithItemsDto.setId(1L);
        requestWithItemsDto.setDescription("Нужна дрель для ремонта");
        requestWithItemsDto.setCreated(LocalDateTime.now());
    }

    @Test
    void createRequest_whenValid_thenReturnCreatedRequest() throws Exception {
        when(requestService.createRequest(any(CreateItemRequestDto.class), anyLong())).thenReturn(itemRequestDto);

        mockMvc.perform(post("/requests")
                        .header("X-Sharer-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(itemRequestDto.getId().intValue())))
                .andExpect(jsonPath("$.description", is(itemRequestDto.getDescription())));

        verify(requestService, times(1)).createRequest(any(CreateItemRequestDto.class), eq(1L));
    }

    @Test
    void createRequest_whenMissingUserIdHeader_thenReturnBadRequest() throws Exception {
        mockMvc.perform(post("/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequestDto)))
                .andExpect(status().isBadRequest());

        verify(requestService, never()).createRequest(any(CreateItemRequestDto.class), anyLong());
    }

    @Test
    void createRequest_whenUserNotFound_thenReturnNotFound() throws Exception {
        when(requestService.createRequest(any(CreateItemRequestDto.class), anyLong()))
                .thenThrow(new NotFoundException("Пользователь не найден"));

        mockMvc.perform(post("/requests")
                        .header("X-Sharer-User-Id", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequestDto)))
                .andExpect(status().isNotFound());

        verify(requestService, times(1)).createRequest(any(CreateItemRequestDto.class), eq(999L));
    }

    @Test
    void getUserRequests_whenUserExists_thenReturnRequests() throws Exception {
        List<ItemRequestWithItemsDto> requests = List.of(requestWithItemsDto);
        when(requestService.getUserRequests(anyLong())).thenReturn(requests);

        mockMvc.perform(get("/requests")
                        .header("X-Sharer-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(requestService, times(1)).getUserRequests(eq(1L));
    }

    @Test
    void getUserRequests_whenUserNotFound_thenReturnNotFound() throws Exception {
        when(requestService.getUserRequests(anyLong()))
                .thenThrow(new NotFoundException("Пользователь не найден"));

        mockMvc.perform(get("/requests")
                        .header("X-Sharer-User-Id", 999L))
                .andExpect(status().isNotFound());

        verify(requestService, times(1)).getUserRequests(eq(999L));
    }

    @Test
    void getUserRequests_whenMissingUserIdHeader_thenReturnBadRequest() throws Exception {
        mockMvc.perform(get("/requests"))
                .andExpect(status().isBadRequest());

        verify(requestService, never()).getUserRequests(anyLong());
    }

    @Test
    void getAllRequests_whenValidPagination_thenReturnRequests() throws Exception {
        List<ItemRequestWithItemsDto> requests = List.of(requestWithItemsDto);
        when(requestService.getAllRequests(anyLong(), anyInt(), anyInt())).thenReturn(requests);

        mockMvc.perform(get("/requests/all")
                        .header("X-Sharer-User-Id", 1L)
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(requestService, times(1)).getAllRequests(eq(1L), eq(0), eq(10));
    }

    @Test
    void getAllRequests_whenDefaultPagination_thenReturnRequests() throws Exception {
        List<ItemRequestWithItemsDto> requests = List.of(requestWithItemsDto);
        when(requestService.getAllRequests(anyLong(), eq(0), eq(10))).thenReturn(requests);

        mockMvc.perform(get("/requests/all")
                        .header("X-Sharer-User-Id", 1L))
                .andExpect(status().isOk());

        verify(requestService, times(1)).getAllRequests(eq(1L), eq(0), eq(10));
    }

    @Test
    void getAllRequests_whenUserNotFound_thenReturnNotFound() throws Exception {
        when(requestService.getAllRequests(anyLong(), anyInt(), anyInt()))
                .thenThrow(new NotFoundException("Пользователь не найден"));

        mockMvc.perform(get("/requests/all")
                        .header("X-Sharer-User-Id", 999L)
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isNotFound());

        verify(requestService, times(1)).getAllRequests(eq(999L), eq(0), eq(10));
    }

    @Test
    void getAllRequests_whenMissingUserIdHeader_thenReturnBadRequest() throws Exception {
        mockMvc.perform(get("/requests/all")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());

        verify(requestService, never()).getAllRequests(anyLong(), anyInt(), anyInt());
    }

    @Test
    void getRequestById_whenRequestExists_thenReturnRequest() throws Exception {
        when(requestService.getRequestById(anyLong(), anyLong())).thenReturn(requestWithItemsDto);

        mockMvc.perform(get("/requests/{requestId}", 1L)
                        .header("X-Sharer-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(requestWithItemsDto.getId().intValue())))
                .andExpect(jsonPath("$.description", is(requestWithItemsDto.getDescription())));

        verify(requestService, times(1)).getRequestById(eq(1L), eq(1L));
    }

    @Test
    void getRequestById_whenRequestNotFound_thenReturnNotFound() throws Exception {
        when(requestService.getRequestById(anyLong(), anyLong()))
                .thenThrow(new NotFoundException("Запрос не найден"));

        mockMvc.perform(get("/requests/{requestId}", 999L)
                        .header("X-Sharer-User-Id", 1L))
                .andExpect(status().isNotFound());

        verify(requestService, times(1)).getRequestById(eq(999L), eq(1L));
    }

    @Test
    void getRequestById_whenUserNotFound_thenReturnNotFound() throws Exception {
        when(requestService.getRequestById(anyLong(), anyLong()))
                .thenThrow(new NotFoundException("Пользователь не найден"));

        mockMvc.perform(get("/requests/{requestId}", 1L)
                        .header("X-Sharer-User-Id", 999L))
                .andExpect(status().isNotFound());

        verify(requestService, times(1)).getRequestById(eq(1L), eq(999L));
    }

    @Test
    void getRequestById_whenMissingUserIdHeader_thenReturnBadRequest() throws Exception {
        mockMvc.perform(get("/requests/{requestId}", 1L))
                .andExpect(status().isBadRequest());

        verify(requestService, never()).getRequestById(anyLong(), anyLong());
    }
}