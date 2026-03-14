package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.storage.GenreStorage;
import ru.yandex.practicum.filmorate.storage.MpaStorage;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@RestController
@Validated
@RequestMapping("/films")
public class FilmController {

    private final FilmService filmService;
    private final GenreStorage genreStorage;
    private final MpaStorage mpaStorage;
    private final LocalDate minReleaseDate = LocalDate.of(1895, 12, 28);

    @Autowired
    public FilmController(FilmService filmService, GenreStorage genreStorage, MpaStorage mpaStorage) {
        this.filmService = filmService;
        this.genreStorage = genreStorage;
        this.mpaStorage = mpaStorage;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Film createFilm(@RequestBody Film film) {
        validateMpaAndGenres(film);
        validate(film, "создании");
        Film createdFilm = filmService.createFilm(film);
        log.info("Фильм {} успешно создан", createdFilm.getId());
        return enrichFilmWithDetails(createdFilm);
    }

    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    public Film updateFilm(@RequestBody Film film) {
        if (film.getId() == null) {
            log.error("ID фильма не может быть null при обновлении");
            throw new ValidationException("ID фильма должен быть указан");
        }
        validateMpaAndGenres(film);
        validate(film, "обновлении");
        Film updatedFilm = filmService.updateFilm(film);
        log.info("Фильм с id {} успешно обновлен", film.getId());
        return enrichFilmWithDetails(updatedFilm);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<Film> findAllFilms() {
        return filmService.findAllFilms().stream()
                .map(this::enrichFilmWithDetails)
                .toList();
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Film getFilmById(@PathVariable Long id) {
        return enrichFilmWithDetails(filmService.getFilmById(id));
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
    public List<Film> getMostPopularFilms(@RequestParam(defaultValue = "10")
                                          @Positive(message = "Количество фильмов должно быть положительным") Integer count) {
        return filmService.getMostPopularFilms(count).stream()
                .map(this::enrichFilmWithDetails)
                .toList();
    }

    private Film enrichFilmWithDetails(Film film) {
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            List<Genre> sortedGenres = new ArrayList<>(film.getGenres());
            sortedGenres.sort(Comparator.comparingInt(Genre::getId));
            film.setGenres(new LinkedHashSet<>(sortedGenres));
        }
        return film;
    }

    private void validateMpaAndGenres(Film film) {
        if (film.getMpa() != null) {
            if (!mpaStorage.existsById(film.getMpa().getId())) {
                log.error("Неверный id MPA: {}", film.getMpa().getId());
                throw new NotFoundException("Рейтинг mpa с id " + film.getMpa().getId() + " не найден");
            }
        }

        if (film.getGenres() != null) {
            for (Genre genre : film.getGenres()) {
                if (!genreStorage.existsById(genre.getId())) {
                    log.error("Неверный id жанра: {}", genre.getId());
                    throw new NotFoundException("Жанр с id " + genre.getId() + " не найден");
                }
            }
        }
    }

    private void validate(Film film, String info) {
        if (film.getName() == null || film.getName().isBlank()) {
            log.error("Ошибка валидации при {}: название фильма не должно быть пустым", info);
            throw new ValidationException("Имя фильма не должно быть пустым");
        }
        if (film.getDescription() != null && film.getDescription().length() > 200) {
            log.error("Ошибка валидации при {}: длина описания {} превышает 200 символов", info, film.getDescription().length());
            throw new ValidationException("Максимальная длина описания — 200 символов");
        }
        if (film.getReleaseDate() == null) {
            log.error("Ошибка валидации при {}: дата релиза не указана", info);
            throw new ValidationException("Дата релиза должна быть указана");
        }
        if (film.getReleaseDate().isBefore(minReleaseDate)) {
            log.error("Ошибка валидации при {}: дата релиза {} раньше минимальной {}", info, film.getReleaseDate(), minReleaseDate);
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }
        if (film.getDuration() == null) {
            log.error("Ошибка валидации при {}: продолжительность фильма не указана", info);
            throw new ValidationException("Продолжительность фильма должна быть указана");
        }
        if (film.getDuration() <= 0) {
            log.error("Ошибка валидации при {}: продолжительность фильма {} должна быть положительной", info, film.getDuration());
            throw new ValidationException("Продолжительность фильма должна быть положительным числом");
        }
    }
}
