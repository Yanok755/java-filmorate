package ru.yandex.practicum.filmorate.storage.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.mapper.UserRowMapper;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDbStorage implements UserStorage {

    private final JdbcTemplate jdbcTemplate;
    private final UserRowMapper userRowMapper;

    @Override
    public User createUser(User user) {
        String sql = "INSERT INTO users (email, login, user_name, birthday) VALUES (?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"user_id"});
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getLogin());
            ps.setString(3, user.getName());
            ps.setDate(4, Date.valueOf(user.getBirthday()));
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            user.setId(key.longValue());
        }

        log.debug("Создан пользователь с id: {}", user.getId());
        return user;
    }

    @Override
    public User updateUser(User user) {
        String sql = "UPDATE users SET email = ?, login = ?, user_name = ?, birthday = ? WHERE user_id = ?";

        int updated = jdbcTemplate.update(sql,
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                Date.valueOf(user.getBirthday()),
                user.getId());

        if (updated == 0) {
            log.error("Пользователь с id {} не найден при обновлении", user.getId());
            throw new NotFoundException("Пользователь с id " + user.getId() + " не найден");
        }

        log.debug("Обновлен пользователь с id: {}", user.getId());
        return user;
    }

    @Override
    public Collection<User> findAllUsers() {
        String sql = "SELECT * FROM users";
        return jdbcTemplate.query(sql, userRowMapper);
    }

    @Override
    public Optional<User> getUserById(Long id) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, id);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean deleteUser(Long id) {
        String sql = "DELETE FROM users WHERE user_id = ?";
        int deleted = jdbcTemplate.update(sql, id);
        return deleted > 0;
    }

    @Override
    public boolean containsUser(Long id) {
        String sql = "SELECT COUNT(*) FROM users WHERE user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count > 0;
    }

    @Override
    public int getUsersCount() {
        String sql = "SELECT COUNT(*) FROM users";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
    }

    @Override
    public void addFriend(Long userId, Long friendId) {
        String checkSql = "SELECT COUNT(*) FROM friends WHERE user_id = ? AND friend_id = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, userId, friendId);

        if (count == 0) {
            String sql = "INSERT INTO friends (user_id, friend_id) VALUES (?, ?)";
            jdbcTemplate.update(sql, userId, friendId);
            log.debug("Дружба добавлена: {} и {}", userId, friendId);
        }
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        String sql = "DELETE FROM friends WHERE user_id = ? AND friend_id = ?";
        jdbcTemplate.update(sql, userId, friendId);
        log.debug("Дружба удалена: {} и {}", userId, friendId);
    }

    @Override
    public Collection<User> getUserFriends(Long userId) {
        String sql = "SELECT u.* FROM users u " +
                     "JOIN friends f ON u.user_id = f.friend_id " +
                     "WHERE f.user_id = ?";
        return jdbcTemplate.query(sql, userRowMapper, userId);
    }

    @Override
    public Collection<User> getCommonFriends(Long userId, Long otherId) {
        String sql = "SELECT u.* FROM users u " +
                     "JOIN friends f1 ON u.user_id = f1.friend_id " +
                     "JOIN friends f2 ON u.user_id = f2.friend_id " +
                     "WHERE f1.user_id = ? AND f2.user_id = ?";

        return jdbcTemplate.query(sql, userRowMapper, userId, otherId);
    }
}
