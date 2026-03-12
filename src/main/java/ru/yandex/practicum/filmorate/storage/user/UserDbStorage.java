package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.mapper.UserRowMapper;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Date;
import java.util.*;

@Slf4j
@Repository
@Qualifier("userDbStorage")
@Primary
public class UserDbStorage implements UserStorage {

    private final JdbcTemplate jdbcTemplate;
    private final UserRowMapper userRowMapper;

    public UserDbStorage(JdbcTemplate jdbcTemplate, UserRowMapper userRowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRowMapper = userRowMapper;
    }

    @Override
    public User createUser(User user) {
        String sql = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getLogin());
            ps.setString(3, user.getName() != null ? user.getName() : user.getLogin());
            ps.setDate(4, Date.valueOf(user.getBirthday()));
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            user.setId(key.longValue());
        }

        log.debug("Пользователь создан в БД: id={}", user.getId());
        return user;
    }

    @Override
    public User updateUser(User user) {
        String sql = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE id = ?";

        int updated = jdbcTemplate.update(sql,
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                Date.valueOf(user.getBirthday()),
                user.getId()
        );

        if (updated == 0) {
            throw new NotFoundException("Пользователь с id " + user.getId() + " не найден");
        }

        log.debug("Пользователь обновлен в БД: id={}", user.getId());
        return user;
    }

    @Override
    public Collection<User> findAllUsers() {
        String sql = "SELECT * FROM users ORDER BY id";
        return jdbcTemplate.query(sql, userRowMapper);
    }

    @Override
    public Optional<User> getUserById(Long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        List<User> users = jdbcTemplate.query(sql, userRowMapper, id);
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }

    @Override
    public boolean deleteUser(Long id) {
        // Сначала удаляем связи дружбы
        String deleteFriendshipsSql = "DELETE FROM friendships WHERE user_id = ? OR friend_id = ?";
        jdbcTemplate.update(deleteFriendshipsSql, id, id);

        // Затем удаляем пользователя
        String deleteUserSql = "DELETE FROM users WHERE id = ?";
        int deleted = jdbcTemplate.update(deleteUserSql, id);

        if (deleted > 0) {
            log.debug("Пользователь удален из БД: id={}", id);
        }
        return deleted > 0;
    }

    @Override
    public boolean containsUser(Long id) {
        String sql = "SELECT COUNT(*) FROM users WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    @Override
    public int getUsersCount() {
        String sql = "SELECT COUNT(*) FROM users";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }

    @Override
    public void addFriend(Long userId, Long friendId) {
        String sql = "INSERT INTO friendships (user_id, friend_id, status) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, userId, friendId, FriendshipStatus.PENDING.name());
        jdbcTemplate.update(sql, friendId, userId, FriendshipStatus.PENDING.name());

        log.debug("Дружба добавлена в БД: пользователь {} добавил пользователя {}", userId, friendId);
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        String sql = "DELETE FROM friendships WHERE (user_id = ? AND friend_id = ?) OR (user_id = ? AND friend_id = ?)";
        jdbcTemplate.update(sql, userId, friendId, friendId, userId);

        log.debug("Дружба удалена из БД: пользователь {} удалил пользователя {}", userId, friendId);
    }

    @Override
    public Collection<Long> getFriendIds(Long userId) {
        String sql = "SELECT friend_id FROM friendships WHERE user_id = ?";
        return jdbcTemplate.queryForList(sql, Long.class, userId);
    }

    @Override
    public Map<Long, FriendshipStatus> getFriendsWithStatus(Long userId) {
        String sql = "SELECT friend_id, status FROM friendships WHERE user_id = ?";
        Map<Long, FriendshipStatus> friends = new HashMap<>();

        jdbcTemplate.query(sql, (rs) -> {
            friends.put(rs.getLong("friend_id"),
                    FriendshipStatus.valueOf(rs.getString("status")));
        }, userId);

        return friends;
    }

    @Override
    public Collection<User> getFriendsAsUsers(Long userId) {
        String sql = """
            SELECT u.* FROM users u
            INNER JOIN friendships f ON u.id = f.friend_id
            WHERE f.user_id = ?
            ORDER BY u.id
        """;

        return jdbcTemplate.query(sql, userRowMapper, userId);
    }

    @Override
    public Collection<User> getCommonFriends(Long userId, Long otherId) {
        String sql = """
            SELECT u.* FROM users u
            WHERE u.id IN (
                SELECT f1.friend_id
                FROM friendships f1
                INNER JOIN friendships f2 ON f1.friend_id = f2.friend_id
                WHERE f1.user_id = ? AND f2.user_id = ?
            )
            ORDER BY u.id
        """;

        return jdbcTemplate.query(sql, userRowMapper, userId, otherId);
    }

    @Override
    public boolean areFriends(Long userId, Long otherId) {
        String sql = "SELECT COUNT(*) FROM friendships WHERE user_id = ? AND friend_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId, otherId);
        return count != null && count > 0;
    }

    @Override
    public int getFriendsCount(Long userId) {
        String sql = "SELECT COUNT(*) FROM friendships WHERE user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
        return count != null ? count : 0;
    }

    @Override
    public void updateFriendshipStatus(Long userId, Long friendId, FriendshipStatus status) {
        String sql = "UPDATE friendships SET status = ? WHERE user_id = ? AND friend_id = ?";
        jdbcTemplate.update(sql, status.name(), userId, friendId);
        jdbcTemplate.update(sql, status.name(), friendId, userId);

        log.debug("Статус дружбы обновлен в БД: пользователи {} и {}, статус {}", userId, friendId, status);
    }
}
