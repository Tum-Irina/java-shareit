package ru.practicum.shareit.integration.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.service.UserService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Test
    void createUser_whenValidUser_thenUserSaved() {
        UserDto userDto = new UserDto();
        userDto.setName("Test User");
        userDto.setEmail("test@example.com");

        UserDto createdUser = userService.createUser(userDto);

        assertNotNull(createdUser.getId());
        assertEquals("Test User", createdUser.getName());
        assertEquals("test@example.com", createdUser.getEmail());
    }

    @Test
    void createUser_whenDuplicateEmail_thenThrowConflictException() {
        UserDto user1 = new UserDto();
        user1.setName("User 1");
        user1.setEmail("same@email.com");
        userService.createUser(user1);

        UserDto user2 = new UserDto();
        user2.setName("User 2");
        user2.setEmail("same@email.com");

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> userService.createUser(user2)
        );

        assertTrue(exception.getMessage().contains("уже существует"));
    }

    @Test
    void getUserById_whenUserExists_thenReturnUser() {
        UserDto createDto = new UserDto();
        createDto.setName("Test User");
        createDto.setEmail("test@example.com");
        UserDto createdUser = userService.createUser(createDto);

        UserDto foundUser = userService.getUserById(createdUser.getId());

        assertEquals(createdUser.getId(), foundUser.getId());
        assertEquals(createdUser.getName(), foundUser.getName());
        assertEquals(createdUser.getEmail(), foundUser.getEmail());
    }

    @Test
    void getUserById_whenUserNotFound_thenThrowNotFoundException() {
        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> userService.getUserById(999L)
        );

        assertTrue(exception.getMessage().contains("не найден"));
    }

    @Test
    void updateUser_whenValidUpdate_thenUserUpdated() {
        UserDto createDto = new UserDto();
        createDto.setName("Original Name");
        createDto.setEmail("original@example.com");
        UserDto createdUser = userService.createUser(createDto);

        UserDto updateDto = new UserDto();
        updateDto.setName("Updated Name");
        updateDto.setEmail("updated@example.com");

        UserDto updatedUser = userService.updateUser(createdUser.getId(), updateDto);

        assertEquals(createdUser.getId(), updatedUser.getId());
        assertEquals("Updated Name", updatedUser.getName());
        assertEquals("updated@example.com", updatedUser.getEmail());
    }

    @Test
    void updateUser_whenPartialUpdate_thenOnlyUpdatedFieldsChanged() {
        UserDto createDto = new UserDto();
        createDto.setName("Original Name");
        createDto.setEmail("original@example.com");
        UserDto createdUser = userService.createUser(createDto);

        UserDto updateDto = new UserDto();
        updateDto.setName("Updated Name");

        UserDto updatedUser = userService.updateUser(createdUser.getId(), updateDto);

        assertEquals("Updated Name", updatedUser.getName());
        assertEquals("original@example.com", updatedUser.getEmail());
    }

    @Test
    void updateUser_whenEmailToExisting_thenThrowConflictException() {
        UserDto user1 = new UserDto();
        user1.setName("User 1");
        user1.setEmail("user1@example.com");
        UserDto createdUser1 = userService.createUser(user1);

        UserDto user2 = new UserDto();
        user2.setName("User 2");
        user2.setEmail("user2@example.com");
        userService.createUser(user2);

        UserDto updateDto = new UserDto();
        updateDto.setEmail("user2@example.com");

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> userService.updateUser(createdUser1.getId(), updateDto)
        );

        assertTrue(exception.getMessage().contains("уже используется"));
    }

    @Test
    void getAllUsers_whenMultipleUsers_thenReturnAll() {
        UserDto user1 = new UserDto();
        user1.setName("User 1");
        user1.setEmail("user1@example.com");
        userService.createUser(user1);

        UserDto user2 = new UserDto();
        user2.setName("User 2");
        user2.setEmail("user2@example.com");
        userService.createUser(user2);

        List<UserDto> allUsers = userService.getAllUsers();

        assertNotNull(allUsers);
        assertTrue(allUsers.size() >= 2);
        assertTrue(allUsers.stream().anyMatch(u -> u.getEmail().equals("user1@example.com")));
        assertTrue(allUsers.stream().anyMatch(u -> u.getEmail().equals("user2@example.com")));
    }

    @Test
    void deleteUser_whenUserExists_thenUserDeleted() {
        UserDto createDto = new UserDto();
        createDto.setName("To Delete");
        createDto.setEmail("delete@example.com");
        UserDto createdUser = userService.createUser(createDto);

        userService.deleteUser(createdUser.getId());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> userService.getUserById(createdUser.getId())
        );

        assertTrue(exception.getMessage().contains("не найден"));
    }

    @Test
    void deleteUser_whenUserNotFound_thenThrowNotFoundException() {
        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> userService.deleteUser(999L)
        );

        assertTrue(exception.getMessage().contains("не найден"));
    }

    @Test
    void testFullUserLifecycle() {
        UserDto createDto = new UserDto();
        createDto.setName("Lifecycle User");
        createDto.setEmail("lifecycle@example.com");
        UserDto created = userService.createUser(createDto);
        assertNotNull(created.getId());

        UserDto found = userService.getUserById(created.getId());
        assertEquals(created.getId(), found.getId());

        UserDto updateDto = new UserDto();
        updateDto.setName("Updated Lifecycle");
        updateDto.setEmail("updated@example.com");
        UserDto updated = userService.updateUser(created.getId(), updateDto);
        assertEquals("Updated Lifecycle", updated.getName());

        List<UserDto> allUsers = userService.getAllUsers();
        assertFalse(allUsers.isEmpty());

        userService.deleteUser(created.getId());

        assertThrows(NotFoundException.class,
                () -> userService.getUserById(created.getId()));
    }
}