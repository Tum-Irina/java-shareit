package ru.practicum.shareit.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.mapper.UserMapper;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User user1;
    private User user2;
    private UserDto userDto1;
    private UserDto userDto2;

    @BeforeEach
    void setUp() {
        user1 = new User();
        user1.setId(1L);
        user1.setName("John Doe");
        user1.setEmail("john@example.com");

        user2 = new User();
        user2.setId(2L);
        user2.setName("Jane Smith");
        user2.setEmail("jane@example.com");

        userDto1 = UserMapper.toUserDto(user1);
        userDto2 = UserMapper.toUserDto(user2);
    }

    @Test
    void createUser_whenValidUser_thenUserCreated() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user1);

        UserDto result = userService.createUser(userDto1);

        assertNotNull(result);
        assertEquals(user1.getId(), result.getId());
        verify(userRepository, times(1)).existsByEmail(user1.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void createUser_whenEmailExists_thenThrowConflictException() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> userService.createUser(userDto1)
        );

        assertEquals("Пользователь с email john@example.com уже существует", exception.getMessage());
        verify(userRepository, times(1)).existsByEmail(user1.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_whenUserExistsAndEmailNotTaken_thenUserUpdated() {
        UserDto updateDto = new UserDto();
        updateDto.setName("John Updated");
        updateDto.setEmail("john.updated@example.com");

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user1));
        when(userRepository.existsByEmailAndIdNot(anyString(), anyLong())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            user1.setName(savedUser.getName());
            user1.setEmail(savedUser.getEmail());
            return user1;
        });

        UserDto result = userService.updateUser(user1.getId(), updateDto);

        assertEquals("John Updated", result.getName());
        assertEquals("john.updated@example.com", result.getEmail());
    }

    @Test
    void updateUser_whenOnlyNameUpdated_thenEmailRemainsSame() {
        UserDto updateDto = new UserDto();
        updateDto.setName("John Updated");

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user1));
        when(userRepository.save(any(User.class))).thenReturn(user1);

        UserDto result = userService.updateUser(user1.getId(), updateDto);

        assertEquals("John Updated", result.getName());
        assertEquals("john@example.com", result.getEmail());
        verify(userRepository, never()).existsByEmailAndIdNot(anyString(), anyLong());
    }

    @Test
    void updateUser_whenUserNotFound_thenThrowNotFoundException() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> userService.updateUser(999L, userDto1)
        );

        assertEquals("Пользователь с ID 999 не найден", exception.getMessage());
    }

    @Test
    void updateUser_whenEmailTakenByOtherUser_thenThrowConflictException() {
        UserDto updateDto = new UserDto();
        updateDto.setEmail("taken@example.com");

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user1));
        when(userRepository.existsByEmailAndIdNot(anyString(), anyLong())).thenReturn(true);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> userService.updateUser(user1.getId(), updateDto)
        );

        assertEquals("Email taken@example.com уже используется другим пользователем", exception.getMessage());
    }

    @Test
    void getUserById_whenUserExists_thenReturnUser() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user1));

        UserDto result = userService.getUserById(user1.getId());

        assertEquals(user1.getId(), result.getId());
        assertEquals(user1.getName(), result.getName());
        verify(userRepository, times(1)).findById(user1.getId());
    }

    @Test
    void getUserById_whenUserNotFound_thenThrowNotFoundException() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> userService.getUserById(999L)
        );

        assertEquals("Пользователь с ID 999 не найден", exception.getMessage());
    }

    @Test
    void getAllUsers_whenUsersExist_thenReturnAllUsers() {
        List<User> users = List.of(user1, user2);
        when(userRepository.findAll()).thenReturn(users);

        List<UserDto> result = userService.getAllUsers();

        assertEquals(2, result.size());
        assertEquals(user1.getId(), result.get(0).getId());
        assertEquals(user2.getId(), result.get(1).getId());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void getAllUsers_whenNoUsers_thenReturnEmptyList() {
        when(userRepository.findAll()).thenReturn(List.of());

        List<UserDto> result = userService.getAllUsers();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void deleteUser_whenUserExists_thenUserDeleted() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user1));
        doNothing().when(userRepository).deleteById(anyLong());

        userService.deleteUser(user1.getId());

        verify(userRepository, times(1)).findById(user1.getId());
        verify(userRepository, times(1)).deleteById(user1.getId());
    }

    @Test
    void deleteUser_whenUserNotFound_thenThrowNotFoundException() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> userService.deleteUser(999L)
        );

        assertEquals("Пользователь с ID 999 не найден", exception.getMessage());
        verify(userRepository, never()).deleteById(anyLong());
    }
}