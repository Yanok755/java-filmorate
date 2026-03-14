package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    public Film createFilm(Film film) {
        return filmStorage.createFilm(film);
    }

    public Film updateFilm(Film film) {
        if (!filmStorage.containsFilm(film.getId())) {
            throw new NotFoundException("Фильм с id " + film.getId() + " не найден");
        }
        return filmStorage.updateFilm(film);
    }

    public Collection<Film> findAllFilms() {
        return filmStorage.findAllFilms();
    }

    public Film getFilmById(Long id) {
        return filmStorage.getFilmById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с id " + id + " не найден"));
    }

    public void addLike(Long filmId, Long userId) {
        // Проверяем существование фильма
        getFilmById(filmId);

        // Проверяем существование пользователя
        if (!userStorage.containsUser(userId)) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }

        // Вызываем метод интерфейса - никаких instanceof!
        filmStorage.addLike(filmId, userId);
        log.info("Лайк добавлен к фильму {} от пользователя {}", filmId, userId);
    }

    public void removeLike(Long filmId, Long userId) {
        // Проверяем существование фильма
        getFilmById(filmId);

        // Проверяем существование пользователя
        if (!userStorage.containsUser(userId)) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }

        // Вызываем метод интерфейса
        filmStorage.removeLike(filmId, userId);
        log.info("Лайк удален у фильма {} от пользователя {}", filmId, userId);
    }

    public Collection<Film> getMostPopularFilms(Integer count) {
        int limit = count != null ? count : 10;
        log.info("Запрос топ-{} популярных фильмов", limit);

        // Вызываем метод интерфейса
        return filmStorage.getMostPopularFilms(limit);
    }
}
