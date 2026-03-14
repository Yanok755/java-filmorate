package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class InMemoryFilmStorage implements FilmStorage {

    private final Map<Long, Film> films = new HashMap<>();
    private final Map<Long, Set<Long>> filmLikes = new HashMap<>(); // filmId -> Set of userIds
    private long currentId = 0;

    @Override
    public Film createFilm(Film film) {
        film.setId(getNextId());
        films.put(film.getId(), film);
        filmLikes.put(film.getId(), new HashSet<>()); // Инициализируем пустое множество лайков

        log.debug("Фильм сохранен: id={}, название='{}'", film.getId(), film.getName());
        return film;
    }

    @Override
    public Film updateFilm(Film film) {
        Film existingFilm = films.get(film.getId());

        existingFilm.setName(film.getName());
        existingFilm.setDescription(film.getDescription());
        existingFilm.setReleaseDate(film.getReleaseDate());
        existingFilm.setDuration(film.getDuration());
        existingFilm.setMpa(film.getMpa());
        existingFilm.setGenres(film.getGenres());

        log.debug("Фильм обновлен: id={}", film.getId());
        return existingFilm;
    }

    @Override
    public Collection<Film> findAllFilms() {
        log.debug("Получены все фильмы. Всего: {} фильмов", films.size());
        return films.values();
    }

    @Override
    public Optional<Film> getFilmById(Long id) {
        log.trace("Поиск фильма по id: {}", id);
        return Optional.ofNullable(films.get(id));
    }

    @Override
    public boolean deleteFilm(Long id) {
        if (films.containsKey(id)) {
            films.remove(id);
            filmLikes.remove(id); // Удаляем и лайки фильма
            log.debug("Фильм удален: id={}", id);
            return true;
        }
        log.warn("Попытка удалить несуществующий фильм: id={}", id);
        return false;
    }

    @Override
    public boolean containsFilm(Long id) {
        boolean exists = films.containsKey(id);
        log.trace("Проверка существования фильма id={}: {}", id, exists);
        return exists;
    }

    @Override
    public int getFilmsCount() {
        int count = films.size();
        log.trace("Текущее количество фильмов: {}", count);
        return count;
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        if (!films.containsKey(filmId)) {
            throw new IllegalArgumentException("Фильм с id " + filmId + " не найден");
        }

        filmLikes.computeIfAbsent(filmId, k -> new HashSet<>()).add(userId);
        log.debug("Лайк добавлен: фильм={}, пользователь={}", filmId, userId);
    }

    @Override
    public void removeLike(Long filmId, Long userId) {
        if (!films.containsKey(filmId)) {
            throw new IllegalArgumentException("Фильм с id " + filmId + " не найден");
        }

        if (filmLikes.containsKey(filmId)) {
            filmLikes.get(filmId).remove(userId);
            log.debug("Лайк удален: фильм={}, пользователь={}", filmId, userId);
        }
    }

    @Override
    public Collection<Film> getMostPopularFilms(int count) {
        log.debug("Получение топ-{} популярных фильмов", count);

        return films.values().stream()
                .sorted((f1, f2) -> {
                    int likes1 = filmLikes.getOrDefault(f1.getId(), Collections.emptySet()).size();
                    int likes2 = filmLikes.getOrDefault(f2.getId(), Collections.emptySet()).size();
                    return Integer.compare(likes2, likes1); // Сортировка по убыванию
                })
                .limit(count)
                .collect(Collectors.toList());
    }

    private long getNextId() {
        return ++currentId;
    }
}
