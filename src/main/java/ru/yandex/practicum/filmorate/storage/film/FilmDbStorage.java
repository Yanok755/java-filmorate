package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Date;
import java.util.*;

@Slf4j
@Repository
@Qualifier("filmDbStorage")
@Primary
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbcTemplate;

    public FilmDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Film createFilm(Film film) {
        String sql = "INSERT INTO films (name, description, release_date, duration, mpa_rating_id) VALUES (?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, film.getName());
            ps.setString(2, film.getDescription());
            ps.setDate(3, Date.valueOf(film.getReleaseDate()));
            ps.setInt(4, film.getDuration());

            int mpaId = film.getMpa() != null ? film.getMpa().getId() : 1;
            ps.setInt(5, mpaId);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            film.setId(key.longValue());
        }

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            saveGenres(film.getId(), film.getGenres());
        }

        return film;
    }

    @Override
    public Film updateFilm(Film film) {
        String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_rating_id = ? WHERE id = ?";

        int mpaId = film.getMpa() != null ? film.getMpa().getId() : 1;

        jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                Date.valueOf(film.getReleaseDate()),
                film.getDuration(),
                mpaId,
                film.getId()
        );

        deleteGenres(film.getId());
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            saveGenres(film.getId(), film.getGenres());
        }

        return film;
    }

    @Override
    public Collection<Film> findAllFilms() {
        String sql = "SELECT * FROM films";
        List<Film> films = jdbcTemplate.query(sql, filmRowMapper);

        if (films.isEmpty()) {
            return films;
        }

        Map<Long, Set<Genre>> genresMap = loadGenresForFilms(films);
        Map<Integer, String> mpaNamesMap = loadAllMpaNames();

        for (Film film : films) {
            film.setGenres(genresMap.getOrDefault(film.getId(), new LinkedHashSet<>()));

            if (film.getMpa() != null) {
                film.getMpa().setName(mpaNamesMap.get(film.getMpa().getId()));
            }
        }

        return films;
    }

    @Override
    public Optional<Film> getFilmById(Long id) {
        String sql = "SELECT * FROM films WHERE id = ?";
        List<Film> films = jdbcTemplate.query(sql, filmRowMapper, id);

        if (films.isEmpty()) {
            return Optional.empty();
        }

        Film film = films.get(0);
        film.setGenres(getGenresForFilm(id));

        if (film.getMpa() != null) {
            film.getMpa().setName(getMpaName(film.getMpa().getId()));
        }

        return Optional.of(film);
    }

    @Override
    public boolean deleteFilm(Long id) {
        String sql = "DELETE FROM films WHERE id = ?";
        return jdbcTemplate.update(sql, id) > 0;
    }

    @Override
    public boolean containsFilm(Long id) {
        String sql = "SELECT COUNT(*) FROM films WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    @Override
    public int getFilmsCount() {
        String sql = "SELECT COUNT(*) FROM films";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        String sql = "INSERT INTO likes (film_id, user_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, filmId, userId);
    }

    @Override
    public void removeLike(Long filmId, Long userId) {
        String sql = "DELETE FROM likes WHERE film_id = ? AND user_id = ?";
        jdbcTemplate.update(sql, filmId, userId);
    }

    @Override
    public Collection<Film> getMostPopularFilms(int count) {
        String sql = "SELECT f.*, COUNT(l.user_id) as likes_count " +
                "FROM films f " +
                "LEFT JOIN likes l ON f.id = l.film_id " +
                "GROUP BY f.id " +
                "ORDER BY likes_count DESC " +
                "LIMIT ?";

        List<Film> films = jdbcTemplate.query(sql, filmRowMapper, count);

        if (films.isEmpty()) {
            return films;
        }

        Map<Long, Set<Genre>> genresMap = loadGenresForFilms(films);
        Map<Integer, String> mpaNamesMap = loadAllMpaNames();

        for (Film film : films) {
            film.setGenres(genresMap.getOrDefault(film.getId(), new LinkedHashSet<>()));

            if (film.getMpa() != null) {
                film.getMpa().setName(mpaNamesMap.get(film.getMpa().getId()));
            }
        }

        return films;
    }

    public int getLikesCount(Long filmId) {
        String sql = "SELECT COUNT(*) FROM likes WHERE film_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, filmId);
        return count != null ? count : 0;
    }

    public Set<Long> getLikesForFilm(Long filmId) {
        String sql = "SELECT user_id FROM likes WHERE film_id = ?";
        return new HashSet<>(jdbcTemplate.queryForList(sql, Long.class, filmId));
    }

    private final RowMapper<Film> filmRowMapper = (rs, rowNum) -> {
        Film film = new Film();
        film.setId(rs.getLong("id"));
        film.setName(rs.getString("name"));
        film.setDescription(rs.getString("description"));
        film.setReleaseDate(rs.getDate("release_date").toLocalDate());
        film.setDuration(rs.getInt("duration"));

        int mpaId = rs.getInt("mpa_rating_id");
        Mpa mpa = new Mpa();
        mpa.setId(mpaId);
        film.setMpa(mpa);

        return film;
    };

    private String getMpaName(int id) {
        String sql = "SELECT name FROM mpa_ratings WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, String.class, id);
    }

    private Map<Integer, String> loadAllMpaNames() {
        String sql = "SELECT id, name FROM mpa_ratings";
        Map<Integer, String> mpaNamesMap = new HashMap<>();

        jdbcTemplate.query(sql, (rs) -> {
            mpaNamesMap.put(rs.getInt("id"), rs.getString("name"));
        });

        return mpaNamesMap;
    }

    private Map<Long, Set<Genre>> loadGenresForFilms(List<Film> films) {
        if (films.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> filmIds = films.stream()
                .map(Film::getId)
                .toList();

        String inSql = String.join(",", Collections.nCopies(filmIds.size(), "?"));

        String sql = "SELECT fg.film_id, g.id, g.name FROM genres g " +
                "JOIN film_genres fg ON g.id = fg.genre_id " +
                "WHERE fg.film_id IN (" + inSql + ") " +
                "ORDER BY fg.film_id, g.id";

        Map<Long, Set<Genre>> genresMap = new HashMap<>();

        jdbcTemplate.query(sql, (rs) -> {
            Long filmId = rs.getLong("film_id");
            Genre genre = new Genre();
            genre.setId(rs.getInt("id"));
            genre.setName(rs.getString("name"));

            genresMap.computeIfAbsent(filmId, k -> new LinkedHashSet<>()).add(genre);
        }, filmIds.toArray());

        return genresMap;
    }

    private Set<Genre> getGenresForFilm(Long filmId) {
        String sql = "SELECT g.id, g.name FROM genres g " +
                "JOIN film_genres fg ON g.id = fg.genre_id " +
                "WHERE fg.film_id = ? ORDER BY g.id";

        return new LinkedHashSet<>(jdbcTemplate.query(sql, (rs, rowNum) -> {
            Genre genre = new Genre();
            genre.setId(rs.getInt("id"));
            genre.setName(rs.getString("name"));
            return genre;
        }, filmId));
    }

    private void saveGenres(Long filmId, Set<Genre> genres) {
        if (genres == null || genres.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";

        for (Genre genre : genres) {
            if (genre != null && genre.getId() >= 1 && genre.getId() <= 6) {
                try {
                    jdbcTemplate.update(sql, filmId, genre.getId());
                } catch (Exception e) {
                    log.error("Ошибка при сохранении жанра {} для фильма {}: {}", genre, filmId, e.getMessage());
                }
            }
        }
    }

    private void deleteGenres(Long filmId) {
        String sql = "DELETE FROM film_genres WHERE film_id = ?";
        jdbcTemplate.update(sql, filmId);
    }
}
