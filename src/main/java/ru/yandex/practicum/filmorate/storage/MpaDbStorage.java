package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.mapper.MpaRowMapper;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MpaDbStorage implements MpaStorage {

    private static final String SQL_SELECT_ALL = "SELECT * FROM mpa_ratings ORDER BY id";
    private static final String SQL_SELECT_BY_ID = "SELECT * FROM mpa_ratings WHERE id = ?";
    private static final String SQL_EXISTS_BY_ID = "SELECT COUNT(*) FROM mpa_ratings WHERE id = ?";

    private final JdbcTemplate jdbcTemplate;
    private final MpaRowMapper mpaRowMapper;

    @Override
    public List<Mpa> findAll() {
        return jdbcTemplate.query(SQL_SELECT_ALL, mpaRowMapper);
    }

    @Override
    public Optional<Mpa> findById(Integer id) {
        return jdbcTemplate.query(SQL_SELECT_BY_ID, mpaRowMapper, id)
                .stream()
                .findFirst();
    }

    @Override
    public boolean existsById(Integer id) {
        Integer count = jdbcTemplate.queryForObject(SQL_EXISTS_BY_ID, Integer.class, id);
        return count != null && count > 0;
    }
}
