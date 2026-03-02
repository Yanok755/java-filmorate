package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserStorage userStorage;

    public User createUser(User user) {
        log.info("Запрос на создание пользователя: {}", user);

        return userStorage.createUser(user);
    }

    public User updateUser(User user) {
        log.info("Запрос на обновление пользователя: {}", user.getId());

        if (!userStorage.containsUser(user.getId())) {
            log.error("Пользователь с id {} не найден", user.getId());
            throw new NotFoundException("Пользователь с id " + user.getId() + " не найден");
        }

        return userStorage.updateUser(user);
    }

    public Collection<User> findAllUsers() {
        Collection<User> users = userStorage.findAllUsers();
        log.info("Запрос на получение всех пользователей. Всего пользователей: {}", users.size());

        return users;
    }

    public User getUserById(Long id) {
        log.info("Запрос на получение пользователя {}", id);

        return userStorage.getUserById(id)
                .orElseThrow(() -> {
                    log.error("Пользователь с id {} не найден", id);
                    throw new NotFoundException("Пользователь с id " + id + " не найден");
                });
    }

    public void addFriend(Long userId, Long friendId) {
        log.info("Запрос на добавление в друзья: пользователь {} хочет добавить пользователя {}", userId, friendId);

        User user = userStorage.getUserById(userId)
                .orElseThrow(() -> {
                    log.error("Пользователь с id {} не найден", userId);
                    return new NotFoundException("Пользователь с id " + userId + " не найден");
                });

        User friend = userStorage.getUserById(friendId)
                .orElseThrow(() -> {
                    log.error("Пользователь с id {} не найден", friendId);
                    return new NotFoundException("Пользователь с id " + friendId + " не найден");
                });

        if (userId.equals(friendId)) {
            log.error("Пользователь {} пытается добавить самого себя в друзья", userId);
            throw new ValidationException("Нельзя добавить самого себя в друзья");
        }

        user.getFriends().add(friendId);
        friend.getFriends().add(userId);

        log.info("Пользователи {} и {} теперь друзья", userId, friendId);
    }

    public void removeFriend(Long userId, Long friendId) {
        log.info("Запрос на удаление из друзей: пользователь {} хочет удалить пользователя {}", userId, friendId);

        User user = userStorage.getUserById(userId)
                .orElseThrow(() -> {
                    log.error("Пользователь с id {} не найден", userId);
                    return new NotFoundException("Пользователь с id " + userId + " не найден");
                });

        User friend = userStorage.getUserById(friendId)
                .orElseThrow(() -> {
                    log.error("Пользователь с id {} не найден", friendId);
                    return new NotFoundException("Пользователь с id " + friendId + " не найден");
                });

        user.getFriends().remove(friendId);
        friend.getFriends().remove(userId);

        log.info("Пользователи {} и {} больше не друзья", userId, friendId);
    }

    public Collection<User> getUserFriends(Long userId) {
        log.info("Запрос на получение друзей пользователя {}", userId);

        User user = userStorage.getUserById(userId)
                .orElseThrow(() -> {
                    log.error("Пользователь с id {} не найден", userId);
                    return new NotFoundException("Пользователь с id " + userId + " не найден");
                });

        return user.getFriends().stream()
                .map(friendId -> userStorage.getUserById(friendId)
                        .orElseThrow(() -> {
                            log.error("Друг с id {} не найден в хранилище", friendId);
                            return new NotFoundException("Друг с id " + friendId + " не найден");
                        }))
                .collect(Collectors.toList());
    }

    public Collection<User> getCommonFriends(Long userId, Long otherId) {
        log.info("Запрос на получение общих друзей пользователей {} и {}", userId, otherId);

        User user = userStorage.getUserById(userId)
                .orElseThrow(() -> {
                    log.error("Пользователь с id {} не найден", userId);
                    return new NotFoundException("Пользователь с id " + userId + " не найден");
                });

        User other = userStorage.getUserById(otherId)
                .orElseThrow(() -> {
                    log.error("Пользователь с id {} не найден", otherId);
                    return new NotFoundException("Пользователь с id " + otherId + " не найден");
                });

        Set<Long> commonFriendIds = user.getFriends().stream()
                .filter(other.getFriends()::contains)
                .collect(Collectors.toSet());

        log.info("Найдено {} общих друзей", commonFriendIds.size());

        return commonFriendIds.stream()
                .map(friendId -> userStorage.getUserById(friendId)
                        .orElseThrow(() -> {
                            log.error("Общий друг с id {} не найден в хранилище", friendId);
                            return new NotFoundException("Общий друг с id " + friendId + " не найден");
                        }))
                .collect(Collectors.toList());
    }
}
