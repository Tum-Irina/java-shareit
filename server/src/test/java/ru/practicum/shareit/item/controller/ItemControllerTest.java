package ru.practicum.shareit.item.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemWithBookingsDto;
import ru.practicum.shareit.item.service.ItemService;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ItemController.class)
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ItemService itemService;

    private ItemDto itemDto;
    private ItemWithBookingsDto itemWithBookingsDto;
    private CommentDto commentDto;

    @BeforeEach
    void setUp() {
        itemDto = new ItemDto();
        itemDto.setId(1L);
        itemDto.setName("Дрель");
        itemDto.setDescription("Мощная дрель");
        itemDto.setAvailable(true);
        itemDto.setOwnerId(1L);

        itemWithBookingsDto = new ItemWithBookingsDto();
        itemWithBookingsDto.setId(1L);
        itemWithBookingsDto.setName("Дрель");
        itemWithBookingsDto.setDescription("Мощная дрель");
        itemWithBookingsDto.setAvailable(true);

        commentDto = new CommentDto();
        commentDto.setId(1L);
        commentDto.setText("Отличная дрель!");
        commentDto.setAuthorName("Booker");
        commentDto.setCreated(LocalDateTime.now());
    }

    @Test
    void createItem_whenValid_thenReturnCreatedItem() throws Exception {
        when(itemService.createItem(any(ItemDto.class), anyLong())).thenReturn(itemDto);

        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(itemDto.getId().intValue())))
                .andExpect(jsonPath("$.name", is(itemDto.getName())))
                .andExpect(jsonPath("$.available", is(itemDto.getAvailable())));

        verify(itemService, times(1)).createItem(any(ItemDto.class), eq(1L));
    }

    @Test
    void createItem_whenMissingUserIdHeader_thenReturnBadRequest() throws Exception {
        mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemDto)))
                .andExpect(status().isBadRequest());

        verify(itemService, never()).createItem(any(ItemDto.class), anyLong());
    }

    @Test
    void updateItem_whenValid_thenReturnUpdatedItem() throws Exception {
        when(itemService.updateItem(anyLong(), any(ItemDto.class), anyLong())).thenReturn(itemDto);

        mockMvc.perform(patch("/items/{itemId}", 1L)
                        .header("X-Sharer-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemDto)))
                .andExpect(status().isOk());

        verify(itemService, times(1)).updateItem(eq(1L), any(ItemDto.class), eq(1L));
    }

    @Test
    void updateItem_whenItemNotFound_thenReturnNotFound() throws Exception {
        when(itemService.updateItem(anyLong(), any(ItemDto.class), anyLong()))
                .thenThrow(new NotFoundException("Вещь не найдена"));

        mockMvc.perform(patch("/items/{itemId}", 999L)
                        .header("X-Sharer-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemDto)))
                .andExpect(status().isNotFound());

        verify(itemService, times(1)).updateItem(eq(999L), any(ItemDto.class), eq(1L));
    }

    @Test
    void getItem_whenExists_thenReturnItem() throws Exception {
        when(itemService.getItemById(anyLong(), anyLong())).thenReturn(itemWithBookingsDto);

        mockMvc.perform(get("/items/{itemId}", 1L)
                        .header("X-Sharer-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(itemWithBookingsDto.getId().intValue())))
                .andExpect(jsonPath("$.name", is(itemWithBookingsDto.getName())));

        verify(itemService, times(1)).getItemById(eq(1L), eq(1L));
    }

    @Test
    void getItem_whenNotFound_thenReturnNotFound() throws Exception {
        when(itemService.getItemById(anyLong(), anyLong()))
                .thenThrow(new NotFoundException("Вещь не найдена"));

        mockMvc.perform(get("/items/{itemId}", 999L)
                        .header("X-Sharer-User-Id", 1L))
                .andExpect(status().isNotFound());

        verify(itemService, times(1)).getItemById(eq(999L), eq(1L));
    }

    @Test
    void getAllUserItems_whenUserExists_thenReturnItems() throws Exception {
        List<ItemWithBookingsDto> items = List.of(itemWithBookingsDto);
        when(itemService.getAllUserItems(anyLong())).thenReturn(items);

        mockMvc.perform(get("/items")
                        .header("X-Sharer-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(itemService, times(1)).getAllUserItems(eq(1L));
    }

    @Test
    void getAllUserItems_whenUserNotFound_thenReturnNotFound() throws Exception {
        when(itemService.getAllUserItems(anyLong()))
                .thenThrow(new NotFoundException("Пользователь не найден"));

        mockMvc.perform(get("/items")
                        .header("X-Sharer-User-Id", 999L))
                .andExpect(status().isNotFound());

        verify(itemService, times(1)).getAllUserItems(eq(999L));
    }

    @Test
    void searchItems_whenValidText_thenReturnItems() throws Exception {
        List<ItemDto> items = List.of(itemDto);
        when(itemService.searchItems(anyString())).thenReturn(items);

        mockMvc.perform(get("/items/search")
                        .param("text", "дрель"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(itemService, times(1)).searchItems(eq("дрель"));
    }

    @Test
    void searchItems_whenEmptyText_thenReturnEmptyList() throws Exception {
        when(itemService.searchItems(anyString())).thenReturn(List.of());

        mockMvc.perform(get("/items/search")
                        .param("text", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(itemService, times(1)).searchItems(eq(""));
    }

    @Test
    void deleteItem_whenExists_thenReturnOk() throws Exception {
        doNothing().when(itemService).deleteItem(anyLong(), anyLong());

        mockMvc.perform(delete("/items/{itemId}", 1L)
                        .header("X-Sharer-User-Id", 1L))
                .andExpect(status().isOk());

        verify(itemService, times(1)).deleteItem(eq(1L), eq(1L));
    }

    @Test
    void deleteItem_whenNotFound_thenReturnNotFound() throws Exception {
        doThrow(new NotFoundException("Вещь не найдена"))
                .when(itemService).deleteItem(anyLong(), anyLong());

        mockMvc.perform(delete("/items/{itemId}", 999L)
                        .header("X-Sharer-User-Id", 1L))
                .andExpect(status().isNotFound());

        verify(itemService, times(1)).deleteItem(eq(999L), eq(1L));
    }

    @Test
    void addComment_whenValid_thenReturnComment() throws Exception {
        when(itemService.addComment(anyLong(), any(CommentDto.class), anyLong())).thenReturn(commentDto);

        mockMvc.perform(post("/items/{itemId}/comment", 1L)
                        .header("X-Sharer-User-Id", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text", is(commentDto.getText())))
                .andExpect(jsonPath("$.authorName", is(commentDto.getAuthorName())));

        verify(itemService, times(1)).addComment(eq(1L), any(CommentDto.class), eq(2L));
    }

    @Test
    void addComment_whenUserDidNotBookItem_thenReturnBadRequest() throws Exception {
        when(itemService.addComment(anyLong(), any(CommentDto.class), anyLong()))
                .thenThrow(new ValidationException("Пользователь не брал эту вещь"));

        mockMvc.perform(post("/items/{itemId}/comment", 1L)
                        .header("X-Sharer-User-Id", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentDto)))
                .andExpect(status().isBadRequest());

        verify(itemService, times(1)).addComment(eq(1L), any(CommentDto.class), eq(2L));
    }

    @Test
    void addComment_whenItemNotFound_thenReturnNotFound() throws Exception {
        when(itemService.addComment(anyLong(), any(CommentDto.class), anyLong()))
                .thenThrow(new NotFoundException("Вещь не найдена"));

        mockMvc.perform(post("/items/{itemId}/comment", 999L)
                        .header("X-Sharer-User-Id", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentDto)))
                .andExpect(status().isNotFound());

        verify(itemService, times(1)).addComment(eq(999L), any(CommentDto.class), eq(2L));
    }

    @Test
    void addComment_whenMissingUserIdHeader_thenReturnBadRequest() throws Exception {
        mockMvc.perform(post("/items/{itemId}/comment", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentDto)))
                .andExpect(status().isBadRequest());

        verify(itemService, never()).addComment(anyLong(), any(CommentDto.class), anyLong());
    }
}