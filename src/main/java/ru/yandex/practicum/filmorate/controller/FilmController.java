package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.Exceptions.ValidationException;
import ru.yandex.practicum.filmorate.NotFoundResponse;
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
            log.warn("Пустой запрос");
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
        log.trace("Валидация запроса прошла успешно");

        Film oldFilm = films.get(film.getId());

        if (oldFilm == null) {
            log.info("Не найдено фильмов с указанным id - {}", film.getId());
            NotFoundResponse error = new NotFoundResponse(HttpStatusCode.valueOf(404), "Не найдено фильмов с указанным id", System.currentTimeMillis());
            return ResponseEntity.status(404).body(error);
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
            log.warn("Отсутствует id");
            throw new ValidationException("Укажите id для обновления фильма");
        }

        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(CINEMA_BIRTHDAY)) {
            log.warn("Проблема с полем 'Дата релиза'");
            throw new ValidationException("Дата релиза не может быть раньше " + CINEMA_BIRTHDAY);
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
}
