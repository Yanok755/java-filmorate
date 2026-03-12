package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;
import ru.yandex.practicum.filmorate.storage.GenreStorage;
import ru.yandex.practicum.filmorate.storage.MpaStorage;

import java.time.LocalDate;
import java.util.Collection;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final GenreStorage genreStorage;
    private final MpaStorage mpaStorage;

    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    public Film createFilm(Film film) {
        log.debug("Создание нового фильма: {}", film);

        // Полная валидация фильма
        validateFilm(film, "создании");
        validateMpaAndGenres(film);

        Film createdFilm = filmStorage.createFilm(film);
        log.info("Фильм успешно создан с id: {}", createdFilm.getId());
        return createdFilm;
    }

    public Film updateFilm(Film film) {
        log.debug("Обновление фильма с id: {}", film.getId());

        // Проверка наличия ID
        if (film.getId() == null) {
            log.error("ID фильма не может быть null при обновлении");
            throw new ValidationException("ID фильма должен быть указан");
        }

        // Проверка существования фильма
        if (!filmStorage.containsFilm(film.getId())) {
            log.error("Фильм с id {} не найден", film.getId());
            throw new NotFoundException("Фильм с id " + film.getId() + " не найден");
        }

        // Полная валидация фильма
        validateFilm(film, "обновлении");
        validateMpaAndGenres(film);

        Film updatedFilm = filmStorage.updateFilm(film);
        log.info("Фильм с id {} успешно обновлен", film.getId());
        return updatedFilm;
    }

    public Collection<Film> findAllFilms() {
        return filmStorage.findAllFilms();
    }

    public Film getFilmById(Long id) {
        return filmStorage.getFilmById(id)
                .orElseThrow(() -> {
                    log.error("Фильм с id {} не найден", id);
                    return new NotFoundException("Фильм с id " + id + " не найден");
                });
    }

    public void addLike(Long filmId, Long userId) {
        log.debug("Добавление лайка к фильму {} от пользователя {}", filmId, userId);

        // Проверка существования фильма
        getFilmById(filmId);

        // Проверка существования пользователя
        if (!userStorage.containsUser(userId)) {
            log.error("Пользователь с id {} не найден", userId);
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }

        if (filmStorage instanceof FilmDbStorage) {
            ((FilmDbStorage) filmStorage).addLike(filmId, userId);
            log.info("Лайк успешно добавлен: фильм {}, пользователь {}", filmId, userId);
        }
    }

    public void removeLike(Long filmId, Long userId) {
        log.debug("Удаление лайка с фильма {} от пользователя {}", filmId, userId);

        // Проверка существования фильма
        getFilmById(filmId);

        // Проверка существования пользователя
        if (!userStorage.containsUser(userId)) {
            log.error("Пользователь с id {} не найден", userId);
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }

        if (filmStorage instanceof FilmDbStorage) {
            ((FilmDbStorage) filmStorage).removeLike(filmId, userId);
            log.info("Лайк успешно удален: фильм {}, пользователь {}", filmId, userId);
        }
    }

    public Collection<Film> getMostPopularFilms(Integer count) {
        int limit = count != null ? count : 10;
        log.debug("Получение топ-{} популярных фильмов", limit);

        if (filmStorage instanceof FilmDbStorage) {
            return ((FilmDbStorage) filmStorage).getMostPopularFilms(limit);
        }

        // Fallback для in-memory хранилища
        return filmStorage.findAllFilms().stream()
                .sorted((f1, f2) -> {
                    int likes1 = filmStorage instanceof FilmDbStorage ?
                            ((FilmDbStorage) filmStorage).getLikesCount(f1.getId()) : 0;
                    int likes2 = filmStorage instanceof FilmDbStorage ?
                            ((FilmDbStorage) filmStorage).getLikesCount(f2.getId()) : 0;
                    return Integer.compare(likes2, likes1);
                })
                .limit(limit)
                .toList();
    }

    /**
     * Валидация полей фильма
     */
    private void validateFilm(Film film, String operation) {
        // Проверка названия
        if (film.getName() == null || film.getName().isBlank()) {
            log.error("Ошибка валидации при {}: название фильма не должно быть пустым", operation);
            throw new ValidationException("Название фильма не должно быть пустым");
        }

        // Проверка описания
        if (film.getDescription() != null && film.getDescription().length() > 200) {
            log.error("Ошибка валидации при {}: длина описания {} превышает 200 символов",
                     operation, film.getDescription().length());
            throw new ValidationException("Максимальная длина описания — 200 символов");
        }

        // Проверка даты релиза
        if (film.getReleaseDate() == null) {
            log.error("Ошибка валидации при {}: дата релиза не указана", operation);
            throw new ValidationException("Дата релиза должна быть указана");
        }

        if (film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            log.error("Ошибка валидации при {}: дата релиза {} раньше минимальной {}",
                     operation, film.getReleaseDate(), MIN_RELEASE_DATE);
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }

        // Проверка продолжительности
        if (film.getDuration() == null) {
            log.error("Ошибка валидации при {}: продолжительность фильма не указана", operation);
            throw new ValidationException("Продолжительность фильма должна быть указана");
        }

        if (film.getDuration() <= 0) {
            log.error("Ошибка валидации при {}: продолжительность фильма {} должна быть положительной",
                     operation, film.getDuration());
            throw new ValidationException("Продолжительность фильма должна быть положительным числом");
        }
    }

    /**
     * Валидация MPA и жанров
     */
    private void validateMpaAndGenres(Film film) {
        // Проверка MPA
        if (film.getMpa() != null) {
            if (!mpaStorage.existsById(film.getMpa().getId())) {
                log.error("Неверный id MPA: {}", film.getMpa().getId());
                throw new NotFoundException("Рейтинг MPA с id " + film.getMpa().getId() + " не найден");
            }
        }

        // Проверка жанров
        if (film.getGenres() != null) {
            for (Genre genre : film.getGenres()) {
                if (!genreStorage.existsById(genre.getId())) {
                    log.error("Неверный id жанра: {}", genre.getId());
                    throw new NotFoundException("Жанр с id " + genre.getId() + " не найден");
                }
            }
        }
    }
}
