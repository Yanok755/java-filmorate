package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserService userService;
    private final GenreService genreService;
    private final MpaService mpaService;

    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    public Film createFilm(Film film) {
        log.debug("Создание нового фильма: {}", film.getName());

        validateFilm(film, "создании");
        validateMpaAndGenres(film);

        Film createdFilm = filmStorage.createFilm(film);
        log.info("Фильм успешно создан с id: {}", createdFilm.getId());

        return enrichFilmWithDetails(createdFilm);
    }

    public Film updateFilm(Film film) {
        log.debug("Обновление фильма с id: {}", film.getId());

        if (film.getId() == null) {
            log.error("ID фильма не может быть null при обновлении");
            throw new ValidationException("ID фильма должен быть указан");
        }

        if (!filmStorage.containsFilm(film.getId())) {
            log.error("Фильм с id {} не найден", film.getId());
            throw new NotFoundException("Фильм с id " + film.getId() + " не найден");
        }

        validateFilm(film, "обновлении");
        validateMpaAndGenres(film);

        Film updatedFilm = filmStorage.updateFilm(film);
        log.info("Фильм с id {} успешно обновлен", film.getId());

        return enrichFilmWithDetails(updatedFilm);
    }

    public List<Film> findAllFilms() {
        return new ArrayList<>(filmStorage.findAllFilms());
    }

    public Film getFilmById(Long id) {
        Film film = filmStorage.getFilmById(id)
                .orElseThrow(() -> {
                    log.error("Фильм с id {} не найден", id);
                    return new NotFoundException("Фильм с id " + id + " не найден");
                });

        return enrichFilmWithDetails(film);
    }

    public void addLike(Long filmId, Long userId) {
        log.debug("Добавление лайка к фильму {} от пользователя {}", filmId, userId);

        getFilmById(filmId);
        userService.getUserById(userId);

        filmStorage.addLike(filmId, userId);
        log.info("Лайк успешно добавлен: фильм {}, пользователь {}", filmId, userId);
    }

    public void removeLike(Long filmId, Long userId) {
        log.debug("Удаление лайка с фильма {} от пользователя {}", filmId, userId);

        getFilmById(filmId);
        userService.getUserById(userId);

        filmStorage.removeLike(filmId, userId);
        log.info("Лайк успешно удален: фильм {}, пользователь {}", filmId, userId);
    }

    public List<Film> getMostPopularFilms(Integer count) {
        int limit = count != null ? count : 10;
        log.debug("Получение топ-{} популярных фильмов", limit);

        Collection<Film> films = filmStorage.getMostPopularFilms(limit);

        return films.stream()
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

    private void validateFilm(Film film, String operation) {
        if (film.getName() == null || film.getName().isBlank()) {
            log.error("Ошибка валидации при {}: название фильма не должно быть пустым", operation);
            throw new ValidationException("Название фильма не должно быть пустым");
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

    private void validateMpaAndGenres(Film film) {
        if (film.getMpa() != null) {
            try {
                mpaService.getMpaById(film.getMpa().getId());
            } catch (NotFoundException e) {
                log.error("Неверный id MPA: {}", film.getMpa().getId());
                throw new NotFoundException("Рейтинг MPA с id " + film.getMpa().getId() + " не найден");
            }
        }

        if (film.getGenres() != null) {
            for (Genre genre : film.getGenres()) {
                try {
                    genreService.getGenreById(genre.getId());
                } catch (NotFoundException e) {
                    log.error("Неверный id жанра: {}", genre.getId());
                    throw new NotFoundException("Жанр с id " + genre.getId() + " не найден");
                }
            }
        }
    }
}
