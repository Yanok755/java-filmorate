package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.ErrorResponse;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/films")
public class FilmController {
    private static final LocalDate CINEMA_BIRTHDAY = LocalDate.of(1895, 12, 28);
    private final Map<Long, Film> films = new HashMap<>();

    @GetMapping
    public ResponseEntity<List<Film>> getFilms() {
        log.info("Вызван эндпоинт на получение всех фильмов");
        return ResponseEntity.ok(new ArrayList<>(films.values()));
    }

    @PostMapping
    public ResponseEntity<Film> addFilm(@Valid @RequestBody Film film) {
        log.info("Вызван эндпоинт на создание нового фильма");

        if (film == null) {
            log.error("Пустой запрос");
            throw new ValidationException("Запрос некорректен");
        }

        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(CINEMA_BIRTHDAY)) {
            log.warn("Проблема с полем 'Дата релиза'");
            throw new ValidationException("Дата релиза не может быть раньше " + CINEMA_BIRTHDAY);
        }

        Long id = generateNextId();
        log.debug("Сгенерирован новый id - {}", id);
        film.setId(id);

        films.put(film.getId(), film);
        log.info("Успешно добавлен новый фильм с id = {}", film.getId());

        return ResponseEntity.ok(film);
    }

    @PutMapping
    public ResponseEntity<?> updateFilm(@Valid @RequestBody Film film) {
        log.info("Вызван эндпоинт на обновление данных фильма");

        validateRequestBody(film);
        log.debug("Валидация запроса прошла успешно");

        Film oldFilm = films.get(film.getId());

        if (oldFilm == null) {
            log.warn("Не найдено фильмов с указанным id - {}", film.getId());
            // Используем ErrorResponse вместо NotFoundResponse
            ErrorResponse error = ErrorResponse.notFound("Не найдено фильмов с указанным id");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        if (film.getDescription() != null) {
            oldFilm.setDescription(film.getDescription());
            log.debug("Обновили описание фильма - {}", oldFilm.getDescription());
        }

        if (film.getName() != null) {
            oldFilm.setName(film.getName());
            log.debug("Обновили название фильма - {}", oldFilm.getName());
        }

        if (film.getReleaseDate() != null) {
            if (film.getReleaseDate().isBefore(CINEMA_BIRTHDAY)) {
                log.warn("Проблема с полем 'Дата релиза' при обновлении");
                throw new ValidationException("Дата релиза не может быть раньше " + CINEMA_BIRTHDAY);
            }
            oldFilm.setReleaseDate(film.getReleaseDate());
            log.debug("Обновили дату релиза фильма - {}", oldFilm.getReleaseDate());
        }

        if (film.getDuration() > 0) {
            oldFilm.setDuration(film.getDuration());
            log.debug("Обновили продолжительность фильма - {}", oldFilm.getDuration());
        }

        log.info("Данные фильма с id = {} успешно обновлены", film.getId());

        return ResponseEntity.ok(oldFilm);
    }

    private void validateRequestBody(Film film) {
        if (film.getId() == null) {
            log.error("Отсутствует id");
            throw new ValidationException("Укажите id для обновления фильма");
        }
    }

    private Long generateNextId() {
        log.trace("Генерация нового id");
        long currentId = films
                .keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0L);

        return currentId + 1;
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException e) {
        log.error("Validation error: {}", e.getMessage());
        ErrorResponse error = ErrorResponse.badRequest(e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception e) {
        log.error("Unexpected error: {}", e.getMessage());
        ErrorResponse error = ErrorResponse.internalError("Internal server error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
