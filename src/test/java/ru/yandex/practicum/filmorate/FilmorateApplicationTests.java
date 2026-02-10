package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.controller.UserController;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FilmorateApplicationTests {

    private FilmController filmController;
    private UserController userController;

    @BeforeEach
    void setUp() {
        filmController = new FilmController();
        userController = new UserController();
    }

    @Test
    void contextLoads() {
        // Проверка загрузки контекста Spring
    }

    // ========== ТЕСТЫ ДЛЯ FILM ==========

    @Test
    void shouldCreateValidFilm() {
        Film film = createValidFilm();
        Film createdFilm = filmController.createFilm(film);

        assertNotNull(createdFilm);
        assertNotNull(createdFilm.getId());
        assertEquals("Test Film", createdFilm.getName());
        assertEquals("Test Description", createdFilm.getDescription());
        assertEquals(LocalDate.of(2000, 1, 1), createdFilm.getReleaseDate());
        assertEquals(120, createdFilm.getDuration());
    }

    @Test
    void shouldThrowExceptionWhenFilmNameIsEmpty() {
        Film film = createValidFilm();
        film.setName("");

        assertThrows(ValidationException.class, () -> filmController.createFilm(film));
    }

    @Test
    void shouldThrowExceptionWhenFilmReleaseDateTooEarly() {
        Film film = createValidFilm();
        film.setReleaseDate(LocalDate.of(1895, 12, 27));

        assertThrows(ValidationException.class, () -> filmController.createFilm(film));
    }

    @Test
    void shouldAcceptFilmReleaseDateExactlyMinDate() {
        Film film = createValidFilm();
        film.setReleaseDate(LocalDate.of(1895, 12, 28));

        Film createdFilm = filmController.createFilm(film);
        assertEquals(LocalDate.of(1895, 12, 28), createdFilm.getReleaseDate());
    }

    @Test
    void shouldThrowExceptionWhenFilmDurationIsZero() {
        Film film = createValidFilm();
        film.setDuration(0);

        assertThrows(ValidationException.class, () -> filmController.createFilm(film));
    }

    @Test
    void shouldThrowExceptionWhenFilmDurationIsNegative() {
        Film film = createValidFilm();
        film.setDuration(-10);

        assertThrows(ValidationException.class, () -> filmController.createFilm(film));
    }

    @Test
    void shouldUpdateFilmSuccessfully() {
        Film film = createValidFilm();
        Film createdFilm = filmController.createFilm(film);

        createdFilm.setName("Updated Film");
        createdFilm.setDescription("Updated Description");

        Film updatedFilm = filmController.updateFilm(createdFilm);
        assertEquals("Updated Film", updatedFilm.getName());
        assertEquals("Updated Description", updatedFilm.getDescription());
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentFilm() {
        Film film = createValidFilm();
        film.setId(999);

        assertThrows(ValidationException.class, () -> filmController.updateFilm(film));
    }

    @Test
    void shouldGetAllFilms() {
        Film film1 = createValidFilm();
        film1.setName("Film 1");
        Film film2 = createValidFilm();
        film2.setName("Film 2");

        filmController.createFilm(film1);
        filmController.createFilm(film2);

        assertEquals(2, filmController.getAllFilms().size());
    }

    // ========== ТЕСТЫ ДЛЯ USER ==========

    @Test
    void shouldCreateValidUser() {
        User user = createValidUser();
        User createdUser = userController.createUser(user);

        assertNotNull(createdUser);
        assertNotNull(createdUser.getId());
        assertEquals("test@example.com", createdUser.getEmail());
        assertEquals("testlogin", createdUser.getLogin());
        assertEquals("Test User", createdUser.getName());
        assertEquals(LocalDate.of(1990, 1, 1), createdUser.getBirthday());
    }

    @Test
    void shouldUseLoginWhenUserNameIsEmpty() {
        User user = createValidUser();
        user.setName("");

        User createdUser = userController.createUser(user);
        assertEquals("testlogin", createdUser.getName());
    }

    @Test
    void shouldThrowExceptionWhenUserEmailIsEmpty() {
        User user = createValidUser();
        user.setEmail("");

        assertThrows(ValidationException.class, () -> userController.createUser(user));
    }

    @Test
    void shouldThrowExceptionWhenUserEmailHasNoAtSymbol() {
        User user = createValidUser();
        user.setEmail("invalid-email.com");

        assertThrows(ValidationException.class, () -> userController.createUser(user));
    }

    @Test
    void shouldThrowExceptionWhenUserLoginIsEmpty() {
        User user = createValidUser();
        user.setLogin("");

        assertThrows(ValidationException.class, () -> userController.createUser(user));
    }

    @Test
    void shouldThrowExceptionWhenUserLoginHasSpaces() {
        User user = createValidUser();
        user.setLogin("test login");

        assertThrows(ValidationException.class, () -> userController.createUser(user));
    }

    @Test
    void shouldUpdateUserSuccessfully() {
        User user = createValidUser();
        User createdUser = userController.createUser(user);

        createdUser.setName("Updated User");
        createdUser.setEmail("updated@example.com");

        User updatedUser = userController.updateUser(createdUser);
        assertEquals("Updated User", updatedUser.getName());
        assertEquals("updated@example.com", updatedUser.getEmail());
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentUser() {
        User user = createValidUser();
        user.setId(999);

        assertThrows(ValidationException.class, () -> userController.updateUser(user));
    }

    @Test
    void shouldGetAllUsers() {
        User user1 = createValidUser();
        user1.setEmail("user1@example.com");
        User user2 = createValidUser();
        user2.setEmail("user2@example.com");

        userController.createUser(user1);
        userController.createUser(user2);

        assertEquals(2, userController.getAllUsers().size());
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    private Film createValidFilm() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("Test Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        return film;
    }

    private User createValidUser() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setLogin("testlogin");
        user.setName("Test User");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return user;
    }
}
