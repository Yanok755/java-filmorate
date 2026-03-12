package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.GenreStorage;
import ru.yandex.practicum.filmorate.storage.MpaStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final GenreStorage genreStorage;
    private final MpaStorage mpaStorage;
    private final LocalDate minReleaseDate = LocalDate.of(1895, 12, 28);

    @Autowired
    public FilmService(
            @Qualifier("filmDbStorage") FilmStorage filmStorage,
            @Qualifier("userDbStorage") UserStorage userStorage,
            GenreStorage genreStorage,
            MpaStorage mpaStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.genreStorage = genreStorage;
        this.mpaStorage = mpaStorage;
    }

    public Film createFilm(Film film) {
        log.debug("Создание фильма: {}", film.getName());
        validateFilm(film, "создании");
        validateMpaAndGenres(film);
        Film createdFilm = filmStorage.createFilm(film);
        log.info("Фильм {} успешно создан", createdFilm.getId());
        return enrichFilmWithDetails(createdFilm);
    }

    public Film updateFilm(Film film) {
        if (film.getId() == null) {
            log.error("ID фильма не может быть null при обновлении");
            throw new ValidationException("ID фильма должен быть указан");
        }

        log.debug("Обновление фильма с id: {}", film.getId());
        validateFilm(film, "обновлении");
        validateMpaAndGenres(film);

        Film updatedFilm = filmStorage.updateFilm(film);
        log.info("Фильм с id {} успешно обновлен", film.getId());
        return enrichFilmWithDetails(updatedFilm);
    }

    public List<Film> findAllFilms() {
        log.debug("Получение всех фильмов");
        return filmStorage.findAllFilms().stream()
                .map(this::enrichFilmWithDetails)
                .toList();
    }

    public Film getFilmById(Long id) {
        log.debug("Получение фильма с id: {}", id);
        Film film = filmStorage.getFilmById(id);
        return enrichFilmWithDetails(film);
    }

    public void addLike(Long filmId, Long userId) {
        log.debug("Добавление лайка фильму {} от пользователя {}", filmId, userId);

        // Проверка существования пользователя через интерфейс, без instanceof
        if (!userStorage.containsUser(userId)) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }

        filmStorage.addLike(filmId, userId);
        log.info("Лайк добавлен: фильм {}, пользователь {}", filmId, userId);
    }

    public void removeLike(Long filmId, Long userId) {
        log.debug("Удаление лайка у фильма {} от пользователя {}", filmId, userId);

        // Проверка существования пользователя через интерфейс, без instanceof
        if (!userStorage.containsUser(userId)) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }

        filmStorage.removeLike(filmId, userId);
        log.info("Лайк удален: фильм {}, пользователь {}", filmId, userId);
    }

    public List<Film> getMostPopularFilms(Integer count) {
        log.debug("Получение {} популярных фильмов", count);
        return filmStorage.getMostPopularFilms(count).stream()
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

    private void validateFilm(Film film, String operation) {
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

        if (film.getReleaseDate().isBefore(minReleaseDate)) {
            log.error("Ошибка валидации при {}: дата релиза {} раньше минимальной {}",
                     operation, film.getReleaseDate(), minReleaseDate);
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }

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
}
