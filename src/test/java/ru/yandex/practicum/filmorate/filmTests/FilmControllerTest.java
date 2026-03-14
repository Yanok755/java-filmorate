package ru.yandex.practicum.filmorate.filmTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.storage.film.InMemoryFilmStorage;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;
import ru.yandex.practicum.filmorate.storage.GenreStorage;
import ru.yandex.practicum.filmorate.storage.MpaStorage;

import java.time.LocalDate;
import java.util.Set;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FilmControllerTest {

    private FilmController filmController;
    private Film validFilm;
    private InMemoryFilmStorage filmStorage;
    private InMemoryUserStorage userStorage;
    private FilmService filmService;
    private GenreStorage genreStorage;
    private MpaStorage mpaStorage;

    @BeforeEach
    void setUp() {
        filmStorage = new InMemoryFilmStorage();
        userStorage = new InMemoryUserStorage();
        filmService = new FilmService(filmStorage, userStorage);

        genreStorage = mock(GenreStorage.class);
        mpaStorage = mock(MpaStorage.class);

        when(mpaStorage.existsById(3)).thenReturn(true);
        when(genreStorage.existsById(1)).thenReturn(true);
        when(genreStorage.existsById(6)).thenReturn(true);

        filmController = new FilmController(filmService, genreStorage, mpaStorage);

        Mpa mpa = new Mpa();
        mpa.setId(3);
        mpa.setName("PG-13");

        Genre comedy = new Genre();
        comedy.setId(1);
        comedy.setName("Комедия");

        Genre action = new Genre();
        action.setId(6);
        action.setName("Боевик");

        Set<Genre> genres = new HashSet<>();
        genres.add(comedy);
        genres.add(action);

        validFilm = new Film();
        validFilm.setName("Тестовый фильм");
        validFilm.setDescription("Описание");
        validFilm.setReleaseDate(LocalDate.of(2020, 1, 4));
        validFilm.setDuration(120);
        validFilm.setMpa(mpa);
        validFilm.setGenres(genres);
    }

    @Test
    void createFilm_ShouldThrowException_WhenNameIsEmpty() {
        validFilm.setName("");
        ValidationException exception = assertThrows(ValidationException.class,
                () -> filmController.createFilm(validFilm));
        assertEquals("Имя фильма не должно быть пустым", exception.getMessage());
    }

    @Test
    void createFilm_ShouldThrowException_WhenNameIsNull() {
        validFilm.setName(null);
        ValidationException exception = assertThrows(ValidationException.class,
                () -> filmController.createFilm(validFilm));
        assertEquals("Имя фильма не должно быть пустым", exception.getMessage());
    }

    @Test
    void createFilm_ShouldThrowException_WhenDescriptionIsTooLong() {
        validFilm.setDescription("a".repeat(201));
        ValidationException exception = assertThrows(ValidationException.class,
                () -> filmController.createFilm(validFilm));
        assertEquals("Максимальная длина описания — 200 символов", exception.getMessage());
    }

    @Test
    void createFilm_ShouldCreateFilm_WhenDescriptionIsExactly200Chars() {
        validFilm.setDescription("a".repeat(200));
        Film createdFilm = filmController.createFilm(validFilm);
        assertNotNull(createdFilm.getId());
        assertEquals(200, createdFilm.getDescription().length());
    }

    @Test
    void createFilm_ShouldThrowException_WhenReleaseDateIsNull() {
        validFilm.setReleaseDate(null);
        ValidationException exception = assertThrows(ValidationException.class,
                () -> filmController.createFilm(validFilm));
        assertEquals("Дата релиза должна быть указана", exception.getMessage());
    }

    @Test
    void createFilm_ShouldThrowException_WhenReleaseDateIsTooEarly() {
        validFilm.setReleaseDate(LocalDate.of(1895, 12, 27));
        ValidationException exception = assertThrows(ValidationException.class,
                () -> filmController.createFilm(validFilm));
        assertEquals("Дата релиза не может быть раньше 28 декабря 1895 года", exception.getMessage());
    }

    @Test
    void createFilm_ShouldAllowReleaseDateExactly1895_12_28() {
        validFilm.setReleaseDate(LocalDate.of(1895, 12, 28));
        Film createdFilm = filmController.createFilm(validFilm);
        assertNotNull(createdFilm.getId());
        assertEquals(LocalDate.of(1895, 12, 28), createdFilm.getReleaseDate());
    }

    @Test
    void createFilm_ShouldThrowException_WhenDurationIsNull() {
        validFilm.setDuration(null);
        ValidationException exception = assertThrows(ValidationException.class,
                () -> filmController.createFilm(validFilm));
        assertEquals("Продолжительность фильма должна быть указана", exception.getMessage());
    }

    @Test
    void createFilm_ShouldThrowException_WhenDurationIsNegative() {
        validFilm.setDuration(-10);
        ValidationException exception = assertThrows(ValidationException.class,
                () -> filmController.createFilm(validFilm));
        assertEquals("Продолжительность фильма должна быть положительным числом", exception.getMessage());
    }

    @Test
    void createFilm_ShouldThrowException_WhenDurationIsZero() {
        validFilm.setDuration(0);
        ValidationException exception = assertThrows(ValidationException.class,
                () -> filmController.createFilm(validFilm));
        assertEquals("Продолжительность фильма должна быть положительным числом", exception.getMessage());
    }

    @Test
    void createFilm_ShouldCreateFilm_WhenMpaIsNull() {
        validFilm.setMpa(null);
        Film createdFilm = filmController.createFilm(validFilm);
        assertNotNull(createdFilm.getId());
        assertNull(createdFilm.getMpa());
    }

    @Test
    void createFilm_ShouldCreateFilm_WhenGenresIsNull() {
        validFilm.setGenres(null);
        Film createdFilm = filmController.createFilm(validFilm);
        assertNotNull(createdFilm.getId());
        assertNull(createdFilm.getGenres());
    }

    @Test
    void createFilm_ShouldCreateFilm_WhenGenresIsEmpty() {
        validFilm.setGenres(new HashSet<>());
        Film createdFilm = filmController.createFilm(validFilm);
        assertNotNull(createdFilm.getId());
        assertTrue(createdFilm.getGenres().isEmpty());
    }

    @Test
    void createFilm_ShouldCreateFilm_WhenAllFieldsAreValid() {
        Film createdFilm = filmController.createFilm(validFilm);
        assertNotNull(createdFilm.getId());
        assertEquals(1L, createdFilm.getId());
        assertEquals(validFilm.getName(), createdFilm.getName());
        assertEquals(validFilm.getDescription(), createdFilm.getDescription());
        assertEquals(validFilm.getReleaseDate(), createdFilm.getReleaseDate());
        assertEquals(validFilm.getDuration(), createdFilm.getDuration());
        assertEquals(validFilm.getMpa().getId(), createdFilm.getMpa().getId());
        assertEquals(validFilm.getMpa().getName(), createdFilm.getMpa().getName());
        assertEquals(2, createdFilm.getGenres().size());

        verify(mpaStorage, times(1)).existsById(3);
        verify(genreStorage, times(1)).existsById(1);
        verify(genreStorage, times(1)).existsById(6);
    }

    @Test
    void createFilm_ShouldThrowException_WhenMpaIdIsInvalid() {
        when(mpaStorage.existsById(99)).thenReturn(false);

        Mpa invalidMpa = new Mpa();
        invalidMpa.setId(99);
        invalidMpa.setName("Invalid");
        validFilm.setMpa(invalidMpa);

        ru.yandex.practicum.filmorate.exception.NotFoundException exception =
                assertThrows(ru.yandex.practicum.filmorate.exception.NotFoundException.class,
                        () -> filmController.createFilm(validFilm));
        assertEquals("Рейтинг mpa с id 99 не найден", exception.getMessage());
    }

    @Test
    void createFilm_ShouldThrowException_WhenGenreIdIsInvalid() {
        when(genreStorage.existsById(99)).thenReturn(false);

        Genre invalidGenre = new Genre();
        invalidGenre.setId(99);
        invalidGenre.setName("Invalid");

        Set<Genre> genres = new HashSet<>(validFilm.getGenres());
        genres.add(invalidGenre);
        validFilm.setGenres(genres);

        ru.yandex.practicum.filmorate.exception.NotFoundException exception =
                assertThrows(ru.yandex.practicum.filmorate.exception.NotFoundException.class,
                        () -> filmController.createFilm(validFilm));
        assertEquals("Жанр с id 99 не найден", exception.getMessage());
    }

    @Test
    void createFilm_ShouldGenerateNewId_ForMultipleFilms() {
        Film firstFilm = filmController.createFilm(validFilm);

        Mpa mpa = new Mpa();
        mpa.setId(4);
        mpa.setName("R");

        when(mpaStorage.existsById(4)).thenReturn(true);

        Film secondFilm = new Film();
        secondFilm.setName("Второй фильм");
        secondFilm.setDescription("Описание второго");
        secondFilm.setReleaseDate(LocalDate.of(2021, 1, 1));
        secondFilm.setDuration(90);
        secondFilm.setMpa(mpa);

        Film createdSecond = filmController.createFilm(secondFilm);

        assertEquals(1L, firstFilm.getId());
        assertEquals(2L, createdSecond.getId());
    }
}
