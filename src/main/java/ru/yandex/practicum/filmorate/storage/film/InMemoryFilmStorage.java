package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class InMemoryFilmStorage implements FilmStorage {

    private final Map<Long, Film> films = new HashMap<>();
    private final Map<Long, Set<Long>> likes = new HashMap<>();
    private long nextId = 1;

    @Override
    public Film createFilm(Film film) {
        film.setId(nextId++);
        films.put(film.getId(), film);
        likes.put(film.getId(), new HashSet<>());
        return film;
    }

    @Override
    public Film updateFilm(Film film) {
        if (!films.containsKey(film.getId())) {
            throw new NotFoundException("Фильм с id " + film.getId() + " не найден");
        }
        films.put(film.getId(), film);
        return film;
    }

    @Override
    public Collection<Film> findAllFilms() {
        return films.values();
    }

    @Override
    public Optional<Film> getFilmById(Long id) {
        return Optional.ofNullable(films.get(id));
    }

    @Override
    public boolean deleteFilm(Long id) {
        if (films.containsKey(id)) {
            films.remove(id);
            likes.remove(id);
            return true;
        }
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
        Set<Long> filmLikes = likes.computeIfAbsent(filmId, k -> new HashSet<>());
        filmLikes.add(userId);
    }

    @Override
    public void removeLike(Long filmId, Long userId) {
        Set<Long> filmLikes = likes.get(filmId);
        if (filmLikes != null) {
            filmLikes.remove(userId);
        }
    }

    @Override
    public int getLikesCount(Long filmId) {
        return likes.getOrDefault(filmId, new HashSet<>()).size();
    }

    @Override
    public Set<Long> getFilmLikes(Long filmId) {
        return likes.getOrDefault(filmId, new HashSet<>());
    }

    @Override
    public Collection<Film> getMostPopularFilms(Integer limit) {
        return films.values().stream()
                .sorted((f1, f2) -> {
                    int likes1 = getLikesCount(f1.getId());
                    int likes2 = getLikesCount(f2.getId());
                    return Integer.compare(likes2, likes1);
                })
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public Mpa getMpaById(Integer id) {
        throw new UnsupportedOperationException("InMemoryFilmStorage не поддерживает получение MPA по ID");
    }
}
