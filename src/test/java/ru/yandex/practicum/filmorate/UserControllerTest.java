package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.controller.UserController;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class UserControllerTest {

    private UserController userController;
    private User validUser;
    private UserStorage userStorage;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userStorage = new InMemoryUserStorage();
        userService = new UserService(userStorage);
        userController = new UserController(userService);

        validUser = new User();
        validUser.setEmail("user@example.com");
        validUser.setLogin("user123");
        validUser.setName("Иван Иванов");
        validUser.setBirthday(LocalDate.of(1990, 1, 1));
    }

    @Test
    void createUser_ShouldThrowException_WhenEmailIsEmpty() {
        validUser.setEmail("");

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userController.createUser(validUser));
        assertEquals("Электронная почта не должна быть пустой", exception.getMessage());
    }

    @Test
    void createUser_ShouldThrowException_WhenEmailHasNoAtSymbol() {
        validUser.setEmail("userexample.com");

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userController.createUser(validUser));
        assertEquals("Электронная почта должна содержать символ @", exception.getMessage());
    }

    @Test
    void createUser_ShouldThrowException_WhenLoginIsEmpty() {
        validUser.setLogin("");

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userController.createUser(validUser));
        assertEquals("Логин не может быть пустым", exception.getMessage());
    }

    @Test
    void createUser_ShouldThrowException_WhenLoginContainsSpaces() {
        validUser.setLogin("user 123");

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userController.createUser(validUser));
        assertEquals("Логин не может содержать пробелы", exception.getMessage());
    }

    @Test
    void createUser_ShouldUseLoginAsName_WhenNameIsEmpty() {
        validUser.setName("");

        User createdUser = userController.createUser(validUser);
        assertEquals(validUser.getLogin(), createdUser.getName());
    }

    @Test
    void createUser_ShouldThrowException_WhenBirthdayIsInFuture() {
        validUser.setBirthday(LocalDate.now().plusDays(1));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userController.createUser(validUser));
        assertEquals("Дата рождения не может быть в будущем", exception.getMessage());
    }

    @Test
    void createUser_ShouldCreateUser_WhenAllFieldsAreValid() {
        User createdUser = userController.createUser(validUser);

        assertNotNull(createdUser.getId());
        assertEquals(1, createdUser.getId());
        assertEquals(validUser.getEmail(), createdUser.getEmail());
    }
}
