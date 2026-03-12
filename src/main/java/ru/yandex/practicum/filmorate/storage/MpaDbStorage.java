package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
public class MpaDbStorage implements MpaStorage {

    // SQL константы
    private static final String SQL_SELECT_ALL_MPA = "SELECT * FROM mpa_ratings ORDER BY id";
    private static final String SQL_SELECT_MPA_BY_ID = "SELECT * FROM mpa_ratings WHERE id = ?";
    private static final String SQL_COUNT_MPA_BY_ID = "SELECT COUNT(*) FROM mpa_ratings WHERE id = ?";

    private final JdbcTemplate jdbcTemplate;

    // RowMapper для преобразования ResultSet в объект Mpa
    private final RowMapper<Mpa> mpaRowMapper = (rs, rowNum) -> {
        Mpa mpa = new Mpa();
        mpa.setId(rs.getInt("id"));
        mpa.setName(rs.getString("name"));
        return mpa;
    };

    public MpaDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Mpa> findAll() {
        log.debug("Получение списка всех MPA рейтингов");
        return jdbcTemplate.query(SQL_SELECT_ALL_MPA, mpaRowMapper);
    }

    @Override
    public Optional<Mpa> findById(int id) {
        log.debug("Поиск MPA рейтинга по id: {}", id);

        try {
            Mpa mpa = jdbcTemplate.queryForObject(SQL_SELECT_MPA_BY_ID, mpaRowMapper, id);
            return Optional.ofNullable(mpa);
        } catch (EmptyResultDataAccessException e) {
            log.debug("MPA рейтинг с id {} не найден", id);
            return Optional.empty();
        }
    }

    @Override
    public boolean existsById(int id) {
        log.debug("Проверка существования MPA рейтинга с id: {}", id);

        Integer count = jdbcTemplate.queryForObject(SQL_COUNT_MPA_BY_ID, Integer.class, id);
        return count != null && count > 0;
    }
}
