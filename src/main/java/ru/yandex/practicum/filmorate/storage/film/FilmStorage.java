package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface FilmStorage {
    // Основные CRUD операции
    Film createFilm(Film film);

    Film updateFilm(Film film);

    Collection<Film> findAllFilms();

    Optional<Film> getFilmById(Long id);

    boolean deleteFilm(Long id);

    boolean containsFilm(Long id);

    int getFilmsCount();

    // Операции с лайками
    void addLike(Long filmId, Long userId);

    void removeLike(Long filmId, Long userId);

    int getLikesCount(Long filmId);

    Set<Long> getFilmLikes(Long filmId);

    // Операции для получения популярных фильмов
    Collection<Film> getMostPopularFilms(Integer limit);

    // Операции с MPA
    Mpa getMpaById(Integer id);
}
