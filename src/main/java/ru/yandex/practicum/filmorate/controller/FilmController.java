package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.time.LocalDate;
import java.util.Collection;

@Slf4j
@RestController
@Validated
@RequestMapping("/films")
public class FilmController {

    private final FilmService filmService;
    LocalDate minReleaseDate = LocalDate.of(1895, 12, 28);

    @Autowired
    public FilmController(FilmService filmService) {
        this.filmService = filmService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Film createFilm(@RequestBody Film film) {

        validate(film, "создании");

        Film createdFilm = filmService.createFilm(film);
        log.info("Фильм {} успешно создан", createdFilm.getId());

        return createdFilm;
    }

    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    public Film updateFilm(@RequestBody Film newFilm) {
        if (newFilm.getId() == null) {
            log.error("ID фильма не может быть null при обновлении");
            throw new ValidationException("ID фильма должен быть указан");
        }

        validate(newFilm, "обновлении");

       Film updateFilm = filmService.updateFilm(newFilm);

        log.info("Фильм с id {} успешно обновлен", newFilm.getId());

        return updateFilm;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Collection<Film> findAllFilms() {
        return filmService.findAllFilms();
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Film getFilmById(@PathVariable Long id) {
        return filmService.getFilmById(id);
    }

    @PutMapping("/{id}/like/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addLike(@PathVariable Long id, @PathVariable Long userId) {
        filmService.addLike(id, userId);
    }

    @DeleteMapping("/{id}/like/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeLike(@PathVariable Long id, @PathVariable Long userId) {
        filmService.removeLike(id, userId);
    }

    @GetMapping("/popular")
    @ResponseStatus(HttpStatus.OK)
    public Collection<Film> getMostPopularFilms(@RequestParam(defaultValue = "10")
                                                    @Positive(message = "Количество фильмов должно быть положительным") Integer count) {
        if (count == null || count <= 0) {
            log.error("Некорректное количество: {}", count);
            throw new ValidationException("Количество фильмов должно быть положительным");
        }
        return filmService.getMostPopularFilms(count);
    }

    private void validate(Film film, String info) {
        if (film.getName() == null || film.getName().isBlank()) {
            log.error("Ошибка валидации при {}: название фильма не должно быть пустым", info);
            throw new ValidationException("Имя фильма не должно быть пустым");
        }

        if (film.getDescription() != null && film.getDescription().length() > 200) {
            log.error("Ошибка валидации при {}: длина описания {} превышает 200 символов", info,
                    film.getDescription().length());

            throw new ValidationException("Максимальная длина описания — 200 символов");
        }

        if (film.getReleaseDate() == null) {
            log.error("Ошибка валидации при {}: дата релиза не указана", info);
            throw new ValidationException("Дата релиза должна быть указана");
        }

        if (film.getReleaseDate().isBefore(minReleaseDate)) {
            log.error("Ошибка валидации при {}: дата релиза {} раньше минимальной {}", info,
                    film.getReleaseDate(), minReleaseDate);
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }

        if (film.getDuration() != null && film.getDuration() <= 0) {
            log.error("Ошибка валидации при {}: продолжительность фильма {} должна быть положительной", info, film.getDuration());
            throw new ValidationException("Продолжительность фильма должна быть положительным числом");
        }
    }
}
