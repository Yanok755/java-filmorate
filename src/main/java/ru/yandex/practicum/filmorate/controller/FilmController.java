package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/films")
@Slf4j
public class FilmController {
    private final Map<Integer, Film> films = new HashMap<>();
    private Integer idCounter = 1;

    private static final LocalDate CINEMA_BIRTHDAY = LocalDate.of(1895, 12, 28);

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Film createFilm(@Valid @RequestBody Film film) {
        validateFilm(film);

        film.setId(idCounter++);
        films.put(film.getId(), film);

        log.info("Добавлен фильм: {}", film);
        return film;
    }

    @PutMapping
    public Film updateFilm(@Valid @RequestBody Film film) {
        if (film.getId() == null) {
            log.error("ID фильма не указан");
            throw new ValidationException("ID фильма не указан");
        }

        if (!films.containsKey(film.getId())) {
            log.error("Фильм с id {} не найден", film.getId());
            throw new NotFoundException("Фильм с указанным id не найден");
        }

        validateFilm(film);
        films.put(film.getId(), film);

        log.info("Обновлен фильм: {}", film);
        return film;
    }

    @GetMapping
    public List<Film> getAllFilms() {
        log.info("Получен список всех фильмов, количество: {}", films.size());
        return new ArrayList<>(films.values());
    }

    private void validateFilm(Film film) {
        if (film.getName() == null || film.getName().isBlank()) {
            log.error("Название фильма не может быть пустым");
            throw new ValidationException("Название фильма не может быть пустым");
        }

        if (film.getDescription() != null && film.getDescription().length() > 200) {
            log.error("Максимальная длина описания — 200 символов");
            throw new ValidationException("Максимальная длина описания — 200 символов");
        }

        if (film.getReleaseDate() == null) {
            log.error("Дата релиза обязательна");
            throw new ValidationException("Дата релиза обязательна");
        }

        if (film.getReleaseDate().isBefore(CINEMA_BIRTHDAY)) {
            log.error("Дата релиза фильма '{}' ({}) раньше 28 декабря 1895 года",
                     film.getName(), film.getReleaseDate());
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }

        if (film.getDuration() == null || film.getDuration() <= 0) {
            log.error("Продолжительность фильма должна быть положительным числом");
            throw new ValidationException("Продолжительность фильма должна быть положительным числом");
        }
    }
}
