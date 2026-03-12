package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Repository
@Qualifier("filmInMemoryStorage")
public class InMemoryFilmStorage implements FilmStorage {

    private final Map<Long, Film> films = new ConcurrentHashMap<>();
    private final Map<Long, Set<Long>> likes = new ConcurrentHashMap<>();
    private long currentId = 0;

    @Override
    public Film createFilm(Film film) {
        film.setId(++currentId);
        films.put(film.getId(), film);
        likes.put(film.getId(), ConcurrentHashMap.newKeySet());
        log.debug("Фильм сохранен: id={}, name={}", film.getId(), film.getName());
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
            likes.remove(id);
            log.debug("Фильм удален: id={}", id);
            return true;
        }
        log.warn("Попытка удалить несуществующий фильм: id={}", id);
        return false;
    }

    @Override
    public boolean containsFilm(Long id) {
        return films.containsKey(id);
    }

    @Override
    public int getFilmsCount() {
        return films.size();
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        likes.get(filmId).add(userId);
        log.debug("Лайк добавлен: фильм {}, пользователь {}", filmId, userId);
    }

    @Override
    public void removeLike(Long filmId, Long userId) {
        likes.get(filmId).remove(userId);
        log.debug("Лайк удален: фильм {}, пользователь {}", filmId, userId);
    }

    @Override
    public Collection<Film> getMostPopularFilms(int count) {
        log.debug("Получение {} популярных фильмов", count);
        return films.values().stream()
                .sorted((f1, f2) -> {
                    int likes1 = likes.getOrDefault(f1.getId(), Collections.emptySet()).size();
                    int likes2 = likes.getOrDefault(f2.getId(), Collections.emptySet()).size();
                    return Integer.compare(likes2, likes1);
                })
                .limit(count)
                .collect(Collectors.toList());
    }

    @Override
    public int getLikesCount(Long filmId) {
        return likes.getOrDefault(filmId, Collections.emptySet()).size();
    }

    @Override
    public Set<Long> getLikesForFilm(Long filmId) {
        return new HashSet<>(likes.getOrDefault(filmId, Collections.emptySet()));
    }
}
