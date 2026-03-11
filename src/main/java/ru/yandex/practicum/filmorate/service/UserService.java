package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserStorage userStorage;

    private void validate(User user, String operation) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            log.error("Ошибка валидации при {}: электронная почта не должна быть пустой", operation);
            throw new ValidationException("Электронная почта не должна быть пустой");
        }

        if (!user.getEmail().contains("@")) {
            log.error("Ошибка валидации при {}: email {} не содержит символ @", operation, user.getEmail());
            throw new ValidationException("Электронная почта должна содержать символ @");
        }

        if (user.getLogin() == null || user.getLogin().isBlank()) {
            log.error("Ошибка валидации при {}: логин не должен быть пустым", operation);
            throw new ValidationException("Логин не может быть пустым");
        }

        if (user.getLogin().contains(" ")) {
            log.error("Ошибка валидации при {}: логин {} содержит пробелы", operation, user.getLogin());
            throw new ValidationException("Логин не может содержать пробелы");
        }

        if (user.getName() == null || user.getName().isBlank()) {
            log.info("Имя пользователя не указано при {}, используем логин: {}", operation, user.getLogin());
            user.setName(user.getLogin());
        }

        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            log.error("Ошибка валидации при {}: дата рождения {} в будущем", operation, user.getBirthday());
            throw new ValidationException("Дата рождения не может быть в будущем");
        }
    }

    public User createUser(User user) {
        log.info("Запрос на создание пользователя: {}", user);
        validate(user, "создании");
        return userStorage.createUser(user);
    }

    public User updateUser(User user) {
        log.info("Запрос на обновление пользователя: {}", user.getId());

        if (user.getId() <= 0) {
            log.error("ID пользователя должен быть положительным числом");
            throw new ValidationException("ID пользователя должен быть указан");
        }

        if (!userStorage.containsUser(user.getId())) {
            log.error("Пользователь с id {} не найден", user.getId());
            throw new NotFoundException("Пользователь с id " + user.getId() + " не найден");
        }

        validate(user, "обновлении");
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

        if (userId.equals(friendId)) {
            log.error("Пользователь {} пытается добавить самого себя в друзья", userId);
            throw new ValidationException("Нельзя добавить самого себя в друзья");
        }

        User user = getUserById(userId);
        User friend = getUserById(friendId);

        user.getFriends().add(friendId);
        friend.getFriends().add(userId);

        log.info("Пользователи {} и {} теперь друзья", userId, friendId);
    }

    public void removeFriend(Long userId, Long friendId) {
        log.info("Запрос на удаление из друзей: пользователь {} хочет удалить пользователя {}", userId, friendId);

        User user = getUserById(userId);
        User friend = getUserById(friendId);

        user.getFriends().remove(friendId);
        friend.getFriends().remove(userId);

        log.info("Пользователи {} и {} больше не друзья", userId, friendId);
    }

    public Collection<User> getUserFriends(Long userId) {
        log.info("Запрос на получение друзей пользователя {}", userId);

        User user = getUserById(userId);

        return user.getFriends().stream()
                .map(this::getUserById)
                .collect(Collectors.toList());
    }

    public Collection<User> getCommonFriends(Long userId, Long otherId) {
        log.info("Запрос на получение общих друзей пользователей {} и {}", userId, otherId);

        User user = getUserById(userId);
        User other = getUserById(otherId);

        Set<Long> commonFriendIds = user.getFriends().stream()
                .filter(other.getFriends()::contains)
                .collect(Collectors.toSet());

        log.info("Найдено {} общих друзей", commonFriendIds.size());

        return commonFriendIds.stream()
                .map(this::getUserById)
                .collect(Collectors.toList());
    }
}
