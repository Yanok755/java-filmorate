package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.EmptyResultDataAccessException;
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

    // SQL константы
    private static final String SQL_INSERT_FILM =
        "INSERT INTO films (name, description, release_date, duration, mpa_rating_id) VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE_FILM =
        "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_rating_id = ? WHERE id = ?";

    private static final String SQL_SELECT_ALL_FILMS = "SELECT * FROM films";

    private static final String SQL_SELECT_FILM_BY_ID = "SELECT * FROM films WHERE id = ?";

    private static final String SQL_DELETE_FILM = "DELETE FROM films WHERE id = ?";

    private static final String SQL_COUNT_FILMS = "SELECT COUNT(*) FROM films";

    private static final String SQL_COUNT_FILM_BY_ID = "SELECT COUNT(*) FROM films WHERE id = ?";

    private static final String SQL_SELECT_FILM_GENRES =
        "SELECT g.id, g.name FROM genres g " +
        "JOIN film_genres fg ON g.id = fg.genre_id " +
        "WHERE fg.film_id = ? ORDER BY g.id";

    private static final String SQL_SELECT_GENRES_FOR_FILMS =
        "SELECT fg.film_id, g.id, g.name FROM genres g " +
        "JOIN film_genres fg ON g.id = fg.genre_id " +
        "WHERE fg.film_id IN (%s) " +
        "ORDER BY fg.film_id, g.id";

    private static final String SQL_INSERT_FILM_GENRE =
        "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";

    private static final String SQL_DELETE_FILM_GENRES =
        "DELETE FROM film_genres WHERE film_id = ?";

    private static final String SQL_SELECT_MPA_BY_ID =
        "SELECT id, name FROM mpa_ratings WHERE id = ?";

    private final JdbcTemplate jdbcTemplate;

    public FilmDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Film createFilm(Film film) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(SQL_INSERT_FILM, Statement.RETURN_GENERATED_KEYS);
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
        int mpaId = film.getMpa() != null ? film.getMpa().getId() : 1;

        jdbcTemplate.update(SQL_UPDATE_FILM,
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
        List<Film> films = jdbcTemplate.query(SQL_SELECT_ALL_FILMS, filmRowMapper);

        if (films.isEmpty()) {
            return films;
        }

        Map<Long, Set<Genre>> genresMap = loadGenresForFilms(films);

        for (Film film : films) {
            film.setGenres(genresMap.getOrDefault(film.getId(), new LinkedHashSet<>()));

            if (film.getMpa() != null && film.getMpa().getId() > 0) {
                Mpa mpa = getMpaById(film.getMpa().getId());
                if (mpa != null) {
                    film.getMpa().setName(mpa.getName());
                }
            }
        }

        return films;
    }

    @Override
    public Optional<Film> getFilmById(Long id) {
        try {
            Film film = jdbcTemplate.queryForObject(SQL_SELECT_FILM_BY_ID, filmRowMapper, id);

            if (film != null) {
                film.setGenres(getGenresForFilm(id));

                if (film.getMpa() != null && film.getMpa().getId() > 0) {
                    Mpa mpa = getMpaById(film.getMpa().getId());
                    if (mpa != null) {
                        film.getMpa().setName(mpa.getName());
                    }
                }

                return Optional.of(film);
            }
        } catch (EmptyResultDataAccessException e) {
            log.debug("Фильм с id {} не найден", id);
        }

        return Optional.empty();
    }

    @Override
    public boolean deleteFilm(Long id) {
        deleteGenres(id);
        return jdbcTemplate.update(SQL_DELETE_FILM, id) > 0;
    }

    @Override
    public boolean containsFilm(Long id) {
        Integer count = jdbcTemplate.queryForObject(SQL_COUNT_FILM_BY_ID, Integer.class, id);
        return count != null && count > 0;
    }

    @Override
    public int getFilmsCount() {
        Integer count = jdbcTemplate.queryForObject(SQL_COUNT_FILMS, Integer.class);
        return count != null ? count : 0;
    }

    // Дополнительные методы для работы с лайками и MPA (не входят в интерфейс)
    public void addLike(Long filmId, Long userId) {
        String sql = "INSERT INTO likes (film_id, user_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, filmId, userId);
    }

    public void removeLike(Long filmId, Long userId) {
        String sql = "DELETE FROM likes WHERE film_id = ? AND user_id = ?";
        jdbcTemplate.update(sql, filmId, userId);
    }

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

        for (Film film : films) {
            film.setGenres(genresMap.getOrDefault(film.getId(), new LinkedHashSet<>()));

            if (film.getMpa() != null && film.getMpa().getId() > 0) {
                Mpa mpa = getMpaById(film.getMpa().getId());
                if (mpa != null) {
                    film.getMpa().setName(mpa.getName());
                }
            }
        }

        return films;
    }

    public int getLikesCount(Long filmId) {
        String sql = "SELECT COUNT(*) FROM likes WHERE film_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, filmId);
        return count != null ? count : 0;
    }

    public Set<Long> getFilmLikes(Long filmId) {
        String sql = "SELECT user_id FROM likes WHERE film_id = ?";
        return new HashSet<>(jdbcTemplate.queryForList(sql, Long.class, filmId));
    }

    public Mpa getMpaById(Integer id) {
        if (id == null) {
            return null;
        }

        try {
            return jdbcTemplate.queryForObject(SQL_SELECT_MPA_BY_ID, (rs, rowNum) -> {
                Mpa mpa = new Mpa();
                mpa.setId(rs.getInt("id"));
                mpa.setName(rs.getString("name"));
                return mpa;
            }, id);
        } catch (EmptyResultDataAccessException e) {
            log.warn("MPA с id {} не найден", id);
            return null;
        }
    }

    private final RowMapper<Film> filmRowMapper = (rs, rowNum) -> {
        Film film = new Film();
        film.setId(rs.getLong("id"));
        film.setName(rs.getString("name"));
        film.setDescription(rs.getString("description"));
        film.setReleaseDate(rs.getDate("release_date").toLocalDate());
        film.setDuration(rs.getInt("duration"));

        int mpaId = rs.getInt("mpa_rating_id");
        if (mpaId > 0) {
            Mpa mpa = new Mpa();
            mpa.setId(mpaId);
            film.setMpa(mpa);
        }

        return film;
    };

    private Map<Long, Set<Genre>> loadGenresForFilms(List<Film> films) {
        if (films.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> filmIds = films.stream()
                .map(Film::getId)
                .toList();

        String inSql = String.join(",", Collections.nCopies(filmIds.size(), "?"));
        String sql = String.format(SQL_SELECT_GENRES_FOR_FILMS, inSql);

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
        return new LinkedHashSet<>(jdbcTemplate.query(SQL_SELECT_FILM_GENRES, (rs, rowNum) -> {
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

        for (Genre genre : genres) {
            if (genre != null && genre.getId() >= 1 && genre.getId() <= 6) {
                try {
                    jdbcTemplate.update(SQL_INSERT_FILM_GENRE, filmId, genre.getId());
                } catch (Exception e) {
                    log.error("Ошибка при сохранении жанра {} для фильма {}: {}", genre, filmId, e.getMessage());
                }
            }
        }
    }

    private void deleteGenres(Long filmId) {
        jdbcTemplate.update(SQL_DELETE_FILM_GENRES, filmId);
    }
}
