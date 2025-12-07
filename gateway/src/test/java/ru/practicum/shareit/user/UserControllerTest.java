package ru.practicum.shareit.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.user.dto.UserDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserClient userClient;

    private UserDto userDto;

    @BeforeEach
    void setUp() {
        userDto = new UserDto();
        userDto.setId(1L);
        userDto.setName("John Doe");
        userDto.setEmail("john@example.com");
    }

    @Test
    void createUser_whenValidUser_thenReturnOk() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isOk());

        verify(userClient, times(1)).createUser(any(UserDto.class));
    }

    @Test
    void createUser_whenInvalidUser_thenReturnBadRequest() throws Exception {
        UserDto invalidUser = new UserDto();
        invalidUser.setName("");
        invalidUser.setEmail("invalid-email");

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUser)))
                .andExpect(status().isBadRequest());

        verify(userClient, never()).createUser(any(UserDto.class));
    }

    @Test
    void updateUser_whenValid_thenReturnOk() throws Exception {
        UserDto updateDto = new UserDto();
        updateDto.setName("Updated Name");

        mockMvc.perform(patch("/users/{userId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk());

        verify(userClient, times(1)).updateUser(eq(1L), any(UserDto.class));
    }

    @Test
    void getUser_whenValid_thenReturnOk() throws Exception {
        mockMvc.perform(get("/users/{userId}", 1L))
                .andExpect(status().isOk());

        verify(userClient, times(1)).getUser(1L);
    }

    @Test
    void getAllUsers_whenCalled_thenReturnOk() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk());

        verify(userClient, times(1)).getAllUsers();
    }

    @Test
    void deleteUser_whenValid_thenReturnOk() throws Exception {
        mockMvc.perform(delete("/users/{userId}", 1L))
                .andExpect(status().isOk());

        verify(userClient, times(1)).deleteUser(1L);
    }
}