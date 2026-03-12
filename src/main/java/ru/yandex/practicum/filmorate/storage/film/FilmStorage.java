package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.model.Film;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface FilmStorage {
    // Базовые CRUD операции
    Film createFilm(Film film);

    Film updateFilm(Film film);

    Collection<Film> findAllFilms();

    Optional<Film> getFilmById(Long id);

    boolean deleteFilm(Long id);

    boolean containsFilm(Long id);

    int getFilmsCount();

    // Методы для работы с лайками
    void addLike(Long filmId, Long userId);

    void removeLike(Long filmId, Long userId);

    int getLikesCount(Long filmId);

    Set<Long> getLikesForFilm(Long filmId);

    // Метод для получения популярных фильмов
    Collection<Film> getMostPopularFilms(int count);
}
