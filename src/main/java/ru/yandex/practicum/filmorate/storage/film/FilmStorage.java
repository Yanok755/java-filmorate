package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface FilmStorage {
    Film createFilm(Film film);
    Film updateFilm(Film film);
    Collection<Film> findAllFilms();
    Optional<Film> getFilmById(Long id);
    boolean containsFilm(Long id);
    void addLike(Long filmId, Long userId);
    void removeLike(Long filmId, Long userId);
    Collection<Film> getMostPopularFilms(Integer limit);
    int getLikesCount(Long filmId);
    Set<Long> getFilmLikes(Long filmId);
    Mpa getMpaById(Integer id);
}
