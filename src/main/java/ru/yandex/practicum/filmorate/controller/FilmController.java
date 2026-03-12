package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.util.Comparator;
import java.util.List;

@Slf4j
@RestController
@Validated
@RequestMapping("/films")
public class FilmController {

    private final FilmService filmService;

    @Autowired
    public FilmController(FilmService filmService) {
        this.filmService = filmService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Film createFilm(@RequestBody Film film) {
        Film createdFilm = filmService.createFilm(film);
        log.info("Фильм {} успешно создан", createdFilm.getId());
        return enrichFilmWithDetails(createdFilm);
    }

    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    public Film updateFilm(@RequestBody Film film) {
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
}
