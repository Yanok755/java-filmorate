package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.genre.mapper.GenreRowMapper;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Qualifier("genreDbStorage")
public class GenreDbStorage implements GenreStorage {

    private static final String SQL_SELECT_ALL = "SELECT * FROM genres ORDER BY id";
    private static final String SQL_SELECT_BY_ID = "SELECT * FROM genres WHERE id = ?";
    private static final String SQL_EXISTS_BY_ID = "SELECT COUNT(*) FROM genres WHERE id = ?";

    private final JdbcTemplate jdbcTemplate;
    private final GenreRowMapper genreRowMapper;

    @Override
    public List<Genre> findAll() {
        return jdbcTemplate.query(SQL_SELECT_ALL, genreRowMapper);
    }

    @Override
    public Optional<Genre> findById(int id) {
        return jdbcTemplate.query(SQL_SELECT_BY_ID, genreRowMapper, id)
                .stream()
                .findFirst();
    }

    @Override
    public boolean existsById(int id) {
        Integer count = jdbcTemplate.queryForObject(SQL_EXISTS_BY_ID, Integer.class, id);
        return count != null && count > 0;
    }
}
