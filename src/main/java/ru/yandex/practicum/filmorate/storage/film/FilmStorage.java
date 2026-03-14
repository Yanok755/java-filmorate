package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.model.Film;
import java.util.Collection;
import java.util.Optional;

public interface FilmStorage {
    Film createFilm(Film film);

    Film updateFilm(Film film);

    Collection<Film> findAllFilms();

    Optional<Film> getFilmById(Long id);

    boolean deleteFilm(Long id);

    boolean containsFilm(Long id);

    int getFilmsCount();
}
