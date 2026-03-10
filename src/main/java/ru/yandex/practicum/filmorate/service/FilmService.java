package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    private void validate(Film film, String operation) {
        if (film.getName() == null || film.getName().isBlank()) {
            log.error("Ошибка валидации при {}: название фильма не должно быть пустым", operation);
            throw new ValidationException("Имя фильма не должно быть пустым");
        }

        if (film.getDescription() != null && film.getDescription().length() > 200) {
            log.error("Ошибка валидации при {}: длина описания {} превышает 200 символов",
                    operation, film.getDescription().length());
            throw new ValidationException("Максимальная длина описания — 200 символов");
        }

        if (film.getReleaseDate() == null) {
            log.error("Ошибка валидации при {}: дата релиза не указана", operation);
            throw new ValidationException("Дата релиза должна быть указана");
        }

        if (film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            log.error("Ошибка валидации при {}: дата релиза {} раньше минимальной {}",
                    operation, film.getReleaseDate(), MIN_RELEASE_DATE);
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }

        if (film.getDuration() == null || film.getDuration() <= 0) {
            log.error("Ошибка валидации при {}: продолжительность фильма {} должна быть положительной",
                    operation, film.getDuration());
            throw new ValidationException("Продолжительность фильма должна быть положительным числом");
        }
    }

    public Film createFilm(Film film) {
        log.info("Запрос на создание фильма: {}", film);
        validate(film, "создании");
        return filmStorage.createFilm(film);
    }

    public Film updateFilm(Film film) {
        log.info("Запрос на обновление фильма: {}", film.getId());

        if (film.getId() == null) {
            log.error("ID фильма не может быть null при обновлении");
            throw new ValidationException("ID фильма должен быть указан");
        }

        if (!filmStorage.containsFilm(film.getId())) {
            log.error("Фильм с id {} не найден", film.getId());
            throw new NotFoundException("Фильм с id " + film.getId() + " не найден");
        }

        validate(film, "обновлении");
        return filmStorage.updateFilm(film);
    }

    public Collection<Film> findAllFilms() {
        Collection<Film> films = filmStorage.findAllFilms();
        log.info("Запрос на получение всех фильмов. Всего фильмов: {}", films.size());
        return films;
    }

    public Film getFilmById(Long id) {
        log.info("Запрос на получение фильма по id {}", id);

        return filmStorage.getFilmById(id)
                .orElseThrow(() -> {
                    log.error("Фильм с id {} не найден", id);
                    return new NotFoundException("Фильм с id " + id + " не найден");
                });
    }

    public void addLike(Long filmId, Long userId) {
        log.info("Запрос на добавление лайка: пользователь {} ставит лайк фильму {}", userId, filmId);

        Film film = getFilmById(filmId);

        userStorage.getUserById(userId)
                .orElseThrow(() -> {
                    log.error("Пользователь с id {} не найден", userId);
                    return new NotFoundException("Пользователь с id " + userId + " не найден");
                });

        film.getLikes().add(userId);
        log.info("Лайк добавлен. У фильма {} теперь {} лайков", filmId, film.getLikes().size());
    }

    public void removeLike(Long filmId, Long userId) {
        log.info("Запрос на удаление лайка: пользователь {} убирает лайк с фильма {}", userId, filmId);

        Film film = getFilmById(filmId);

        userStorage.getUserById(userId)
                .orElseThrow(() -> {
                    log.error("Пользователь с id {} не найден", userId);
                    return new NotFoundException("Пользователь с id " + userId + " не найден");
                });

        if (!film.getLikes().contains(userId)) {
            log.error("Пользователь {} не ставил лайк фильму {}", userId, filmId);
            throw new ValidationException("Пользователь с id " + userId + " не ставил лайк этому фильму");
        }

        film.getLikes().remove(userId);
        log.info("Лайк удален. У фильма {} теперь {} лайков", filmId, film.getLikes().size());
    }

    public Collection<Film> getMostPopularFilms(Integer count) {
        int limit = count != null ? count : 10;
        log.info("Запрос на получение {} самых популярных фильмов", limit);

        return filmStorage.findAllFilms().stream()
                .sorted(Comparator.comparingInt((Film film) -> film.getLikes().size()).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
}
