package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/films")
public class FilmController {

    private final Map<Long, Film> films = new HashMap<>();
    LocalDate minReleaseDate = LocalDate.of(1895, 12, 28);

    @PostMapping
    public Film createFilm(@RequestBody Film film) {
        log.info("Запрос на создание фильма: {}",film);

        if (film.getName() == null || film.getName().isBlank()) {
            log.error("Ошибка валидации при создании: название фильма не должно быть пустым");
            throw new ValidationException("Имя фильма не должно быть пустым");
        }

        if (film.getDescription().length() > 200) {
            log.error("Ошибка валидации при создании: длина описания {} превышает 200 символов",
                    film.getDescription() != null ? film.getDescription().length() : 0);

            throw new ValidationException("Максимальная длина описания — 200 символов");
        }

        if (film.getReleaseDate() == null) {
            log.error("Ошибка валидации при создании: дата релиза не указана");
            throw new ValidationException("Дата релиза должна быть указана");
        }

        if (film.getReleaseDate().isBefore(minReleaseDate)) {
            log.error("Ошибка валидации при создании: дата релиза {} раньше минимальной {}",
                    film.getReleaseDate(), minReleaseDate);
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }

        if (film.getDuration() <= 0) {
            log.error("Ошибка валидации при создании: продолжительность фильма {} должна быть положительной",
                    film.getDuration());
            throw new ValidationException("Продолжительность фильма должна быть положительным числом");
        }

        film.setId(getNextId());
        films.put(film.getId(), film);

        log.info("Фильм {} успешно создан", film.getId());

        return film;
    }

    @PutMapping
    public Film updateFilm(@RequestBody Film newFilm) {
        log.info("Запрос на обновление фильма: {}", newFilm.getId());

        if (newFilm.getId() == null) {
            log.error("ID фильма не может быть null при обновлении");
            throw new ValidationException("ID фильма должен быть указан");
        }

        if (!films.containsKey(newFilm.getId())) {
            log.error("Фильм с id {} не найден", newFilm.getId());
            throw new ValidationException("Фильм с id " + newFilm.getId() + " не найден");
        }

        if (newFilm.getName() == null || newFilm.getName().isBlank()) {
            log.error("Ошибка валидации при обновлении: название фильма не должно быть пустым");
            throw new ValidationException("Название фильма не может быть пустым");
        }

        if (newFilm.getDescription() == null || newFilm.getDescription().length() > 200) {
            log.error("Ошибка валидации при обновлении: длина описания {} превышает 200 символов",
                    newFilm.getDescription() != null ? newFilm.getDescription().length() : 0);
            throw new ValidationException("Максимальная длина описания — 200 символов");
        }

        if (newFilm.getReleaseDate() == null) {
            log.error("Ошибка валидации при обновлении: дата релиза не указана");
            throw new ValidationException("Дата релиза должна быть указана");
        }

        if (newFilm.getReleaseDate().isBefore(minReleaseDate)) {
            log.error("Ошибка валидации при обновлении: дата релиза {} раньше минимальной {}",
                    newFilm.getReleaseDate(), minReleaseDate);
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }

        if (newFilm.getDuration() <= 0) {
            log.error("Ошибка валидации при обновлении: продолжительность фильма {} должна быть положительной",
                    newFilm.getDuration());
            throw new ValidationException("Продолжительность фильма должна быть положительным числом");
        }

        Film oldFilm = films.get(newFilm.getId());

        oldFilm.setName(newFilm.getName());
        oldFilm.setDescription(newFilm.getDescription());
        oldFilm.setReleaseDate(newFilm.getReleaseDate());
        oldFilm.setDuration(newFilm.getDuration());

        log.info("Фильм с id {} успешно обновлен", newFilm.getId());

        return oldFilm;
    }

    @GetMapping
    public Collection<Film> findAllFilms() {
        log.info("Получен запрос на получение всех фильмов. Всего фильмов: {}", films.size());
        return films.values();
    }

    private long getNextId() {
        long currentMaxId = films.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }
}
