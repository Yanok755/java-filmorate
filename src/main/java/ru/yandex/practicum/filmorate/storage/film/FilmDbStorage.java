package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.mapper.FilmRowMapper;
import ru.yandex.practicum.filmorate.storage.mapper.GenreRowMapper;
import ru.yandex.practicum.filmorate.storage.mapper.MpaRowMapper;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbcTemplate;
    private final FilmRowMapper filmRowMapper;
    private final GenreRowMapper genreRowMapper;
    private final MpaRowMapper mpaRowMapper;

    @Override
    public Film createFilm(Film film) {
        String sql = "INSERT INTO films (name, description, release_date, duration, mpa_id) VALUES (?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"film_id"});
            ps.setString(1, film.getName());
            ps.setString(2, film.getDescription());
            ps.setDate(3, Date.valueOf(film.getReleaseDate()));
            ps.setInt(4, film.getDuration());
            ps.setInt(5, film.getMpa().getId());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            film.setId(key.longValue());
        }

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            saveFilmGenres(film.getId(), film.getGenres());
        }

        loadMpaNameForFilm(film);

        log.debug("Создан фильм с id: {}", film.getId());
        return film;
    }

    @Override
    public Film updateFilm(Film film) {
        String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_id = ? WHERE film_id = ?";

        int updated = jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                Date.valueOf(film.getReleaseDate()),
                film.getDuration(),
                film.getMpa().getId(),
                film.getId());

        if (updated == 0) {
            log.error("Фильм с id {} не найден при обновлении", film.getId());
            throw new NotFoundException("Фильм с id " + film.getId() + " не найден");
        }

        updateFilmGenres(film.getId(), film.getGenres());
        loadMpaNameForFilm(film);

        log.debug("Обновлен фильм с id: {}", film.getId());
        return film;
    }

    @Override
    public Collection<Film> findAllFilms() {
        String sql = "SELECT f.*, m.mpa_name FROM films f " +
                     "JOIN mpa_ratings m ON f.mpa_id = m.mpa_id";

        List<Film> films = jdbcTemplate.query(sql, filmRowMapper);

        Map<Long, Set<Genre>> genresMap = loadGenresForFilms(films);
        films.forEach(film -> film.setGenres(genresMap.getOrDefault(film.getId(), new LinkedHashSet<>())));

        return films;
    }

    @Override
    public Optional<Film> getFilmById(Long id) {
        String sql = "SELECT f.*, m.mpa_name FROM films f " +
                     "JOIN mpa_ratings m ON f.mpa_id = m.mpa_id " +
                     "WHERE f.film_id = ?";

        try {
            Film film = jdbcTemplate.queryForObject(sql, filmRowMapper, id);
            if (film != null) {
                loadGenresForFilm(film);
            }
            return Optional.ofNullable(film);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean containsFilm(Long id) {
        String sql = "SELECT COUNT(*) FROM films WHERE film_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        String checkSql = "SELECT COUNT(*) FROM likes WHERE film_id = ? AND user_id = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, filmId, userId);

        if (count == null || count == 0) {
            String sql = "INSERT INTO likes (film_id, user_id) VALUES (?, ?)";
            jdbcTemplate.update(sql, filmId, userId);
            log.debug("Лайк добавлен: фильм {}, пользователь {}", filmId, userId);
        }
    }

    @Override
    public void removeLike(Long filmId, Long userId) {
        String sql = "DELETE FROM likes WHERE film_id = ? AND user_id = ?";
        jdbcTemplate.update(sql, filmId, userId);
        log.debug("Лайк удален: фильм {}, пользователь {}", filmId, userId);
    }

    @Override
    public Collection<Film> getMostPopularFilms(Integer limit) {
        String sql = "SELECT f.*, m.mpa_name, COUNT(l.user_id) as likes_count " +
                     "FROM films f " +
                     "JOIN mpa_ratings m ON f.mpa_id = m.mpa_id " +
                     "LEFT JOIN likes l ON f.film_id = l.film_id " +
                     "GROUP BY f.film_id " +
                     "ORDER BY likes_count DESC " +
                     "LIMIT ?";

        List<Film> films = jdbcTemplate.query(sql, filmRowMapper, limit);

        Map<Long, Set<Genre>> genresMap = loadGenresForFilms(films);
        films.forEach(film -> film.setGenres(genresMap.getOrDefault(film.getId(), new LinkedHashSet<>())));

        return films;
    }

    @Override
    public int getLikesCount(Long filmId) {
        String sql = "SELECT COUNT(*) FROM likes WHERE film_id = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, filmId);
    }

    @Override
    public Set<Long> getFilmLikes(Long filmId) {
        String sql = "SELECT user_id FROM likes WHERE film_id = ?";
        return new HashSet<>(jdbcTemplate.queryForList(sql, Long.class, filmId));
    }

    @Override
    public Mpa getMpaById(Integer id) {
        String sql = "SELECT * FROM mpa_ratings WHERE mpa_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, mpaRowMapper, id);
        } catch (EmptyResultDataAccessException e) {
            log.error("MPA с id {} не найден", id);
            throw new NotFoundException("Рейтинг MPA с id " + id + " не найден");
        }
    }

    private void saveFilmGenres(Long filmId, Set<Genre> genres) {
        String sql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";

        for (Genre genre : genres) {
            jdbcTemplate.update(sql, filmId, genre.getId());
        }
    }

    private void updateFilmGenres(Long filmId, Set<Genre> newGenres) {
        String deleteSql = "DELETE FROM film_genres WHERE film_id = ?";
        jdbcTemplate.update(deleteSql, filmId);

        if (newGenres != null && !newGenres.isEmpty()) {
            saveFilmGenres(filmId, newGenres);
        }
    }

    private void loadGenresForFilm(Film film) {
        String sql = "SELECT g.* FROM genres g " +
                     "JOIN film_genres fg ON g.genre_id = fg.genre_id " +
                     "WHERE fg.film_id = ? " +
                     "ORDER BY g.genre_id";

        List<Genre> genres = jdbcTemplate.query(sql, genreRowMapper, film.getId());
        film.setGenres(new LinkedHashSet<>(genres));
    }

    private Map<Long, Set<Genre>> loadGenresForFilms(List<Film> films) {
        if (films.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> filmIds = films.stream()
                .map(Film::getId)
                .collect(Collectors.toList());

        String placeholders = String.join(",", Collections.nCopies(filmIds.size(), "?"));
        String sql = "SELECT fg.film_id, g.* FROM genres g " +
                     "JOIN film_genres fg ON g.genre_id = fg.genre_id " +
                     "WHERE fg.film_id IN (" + placeholders + ") " +
                     "ORDER BY g.genre_id";

        Map<Long, Set<Genre>> genresMap = new HashMap<>();

        jdbcTemplate.query(sql, rs -> {
            Long filmId = rs.getLong("film_id");
            Genre genre = genreRowMapper.mapRow(rs, rs.getRow());

            genresMap.computeIfAbsent(filmId, k -> new LinkedHashSet<>()).add(genre);
        }, filmIds.toArray());

        return genresMap;
    }

    private void loadMpaNameForFilm(Film film) {
        if (film.getMpa() != null && film.getMpa().getId() != null) {
            String sql = "SELECT mpa_name FROM mpa_ratings WHERE mpa_id = ?";
            try {
                String mpaName = jdbcTemplate.queryForObject(sql, String.class, film.getMpa().getId());
                film.getMpa().setName(mpaName);
            } catch (EmptyResultDataAccessException e) {
                log.warn("MPA с id {} не найден", film.getMpa().getId());
            }
        }
    }
}
