package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
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
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Date;
import java.util.*;

@Slf4j
@Repository
@Qualifier("filmDbStorage")
@Primary
@RequiredArgsConstructor
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Film> filmRowMapper = (rs, rowNum) -> {
        Film film = new Film();
        film.setId(rs.getLong("id"));
        film.setName(rs.getString("name"));
        film.setDescription(rs.getString("description"));
        film.setReleaseDate(rs.getDate("release_date").toLocalDate());
        film.setDuration(rs.getInt("duration"));

        int mpaId = rs.getInt("mpa_rating_id");
        film.setMpaRating(MpaRating.values()[mpaId - 1]);

        film.setLikes(getLikes(film.getId()));
        film.setGenres(getGenres(film.getId()));

        return film;
    };

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
            ps.setInt(5, film.getMpaRating().ordinal() + 1);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            film.setId(key.longValue());
        }

        saveGenres(film.getId(), film.getGenres());

        log.debug("Фильм сохранен в БД: id={}", film.getId());
        return film;
    }

    @Override
    public Film updateFilm(Film film) {
        String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_rating_id = ? WHERE id = ?";

        jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                Date.valueOf(film.getReleaseDate()),
                film.getDuration(),
                film.getMpaRating().ordinal() + 1,
                film.getId()
        );

        deleteGenres(film.getId());
        saveGenres(film.getId(), film.getGenres());

        log.debug("Фильм обновлен в БД: id={}", film.getId());
        return film;
    }

    @Override
    public Collection<Film> findAllFilms() {
        String sql = "SELECT * FROM films";
        return jdbcTemplate.query(sql, filmRowMapper);
    }

    @Override
    public Optional<Film> getFilmById(Long id) {
        String sql = "SELECT * FROM films WHERE id = ?";
        List<Film> films = jdbcTemplate.query(sql, filmRowMapper, id);
        return films.isEmpty() ? Optional.empty() : Optional.of(films.get(0));
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

    private Set<Long> getLikes(Long filmId) {
        String sql = "SELECT user_id FROM likes WHERE film_id = ?";
        return new HashSet<>(jdbcTemplate.queryForList(sql, Long.class, filmId));
    }

    private Set<Genre> getGenres(Long filmId) {
        String sql = "SELECT genre_id FROM film_genres WHERE film_id = ? ORDER BY genre_id";
        Set<Genre> genres = new HashSet<>();

        List<Integer> genreIds = jdbcTemplate.queryForList(sql, Integer.class, filmId);
        for (Integer genreId : genreIds) {
            genres.add(Genre.values()[genreId - 1]);
        }

        return genres;
    }

    private void saveGenres(Long filmId, Set<Genre> genres) {
        if (genres == null || genres.isEmpty()) return;

        String sql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";

        for (Genre genre : genres) {
            jdbcTemplate.update(sql, filmId, genre.ordinal() + 1);
        }
    }

    private void deleteGenres(Long filmId) {
        String sql = "DELETE FROM film_genres WHERE film_id = ?";
        jdbcTemplate.update(sql, filmId);
    }
}
