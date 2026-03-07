package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class InMemoryFilmStorage implements FilmStorage {

    private final Map<Long, Film> films = new HashMap<>();

    private long currentId = 0;

    @Override
    public Film createFilm(Film film) {
        film.setId(getNextId());

        films.put(film.getId(), film);

        log.debug("Фильм сохранен: id={}, название='{}'",
                film.getId(), film.getName());

        return film;
    }

    @Override
    public Film updateFilm(Film film) {
        Film existingFilm = films.get(film.getId());

        existingFilm.setName(film.getName());
        existingFilm.setDescription(film.getDescription());
        existingFilm.setReleaseDate(film.getReleaseDate());
        existingFilm.setDuration(film.getDuration());

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
        log.debug("Поиск фильма по id: {}", id);  // ИСПРАВЛЕНО: trace -> debug

        return Optional.ofNullable(films.get(id));
    }

    @Override
    public boolean deleteFilm(Long id) {
        if (films.containsKey(id)) {
            films.remove(id);
            log.debug("Фильм удален: id={}", id);
            return true;
        }

        log.warn("Попытка удалить несуществующий фильм: id={}", id);
        return false;
    }

    @Override
    public boolean containsFilm(Long id) {
        boolean exists = films.containsKey(id);
        log.debug("Проверка существования фильма id={}: {}", id, exists);  // ИСПРАВЛЕНО: trace -> debug
        return exists;
    }

    @Override
    public int getFilmsCount() {
        int count = films.size();
        log.debug("Текущее количество фильмов: {}", count);  // ИСПРАВЛЕНО: trace -> debug
        return count;
    }

    private long getNextId() {
        return ++currentId;
    }
}
