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

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserStorage userStorage;

    public User createUser(User user) {
        log.debug("Создание нового пользователя: {}", user.getEmail());

        validateUser(user, "создании");
        setNameIfBlank(user, "создании");

        User createdUser = userStorage.createUser(user);
        log.info("Пользователь успешно создан с id: {}", createdUser.getId());
        return createdUser;
    }

    public User updateUser(User user) {
        log.debug("Обновление пользователя с id: {}", user.getId());

        if (user.getId() == null) {
            log.error("ID пользователя не может быть null при обновлении");
            throw new ValidationException("ID пользователя должен быть указан");
        }

        if (!userStorage.containsUser(user.getId())) {
            log.error("Пользователь с id {} не найден", user.getId());
            throw new NotFoundException("Пользователь с id " + user.getId() + " не найден");
        }

        validateUser(user, "обновлении");
        setNameIfBlank(user, "обновлении");

        User updatedUser = userStorage.updateUser(user);
        log.info("Пользователь с id {} успешно обновлен", user.getId());
        return updatedUser;
    }

    public Collection<User> findAllUsers() {
        return userStorage.findAllUsers();
    }

    public User getUserById(Long id) {
        return userStorage.getUserById(id)
                .orElseThrow(() -> {
                    log.error("Пользователь с id {} не найден", id);
                    return new NotFoundException("Пользователь с id " + id + " не найден");
                });
    }

    public void addFriend(Long userId, Long friendId) {
        log.debug("Добавление в друзья: пользователь {} добавляет пользователя {}", userId, friendId);

        getUserById(userId);
        getUserById(friendId);

        if (userId.equals(friendId)) {
            log.error("Пользователь {} пытается добавить сам себя в друзья", userId);
            throw new ValidationException("Нельзя добавить самого себя в друзья");
        }

        userStorage.addFriend(userId, friendId);
        log.debug("Пользователи {} и {} теперь друзья", userId, friendId);
    }

    public void removeFriend(Long userId, Long friendId) {
        log.debug("Удаление из друзей: пользователь {} удаляет пользователя {}", userId, friendId);

        getUserById(userId);
        getUserById(friendId);

        userStorage.removeFriend(userId, friendId);
        log.debug("Пользователи {} и {} больше не друзья", userId, friendId);
    }

    public Collection<User> getUserFriends(Long userId) {
        log.debug("Получение друзей пользователя с id: {}", userId);

        getUserById(userId);

        return userStorage.getUserFriends(userId);
    }

    public Collection<User> getCommonFriends(Long userId, Long otherId) {
        log.debug("Получение общих друзей пользователей {} и {}", userId, otherId);

        getUserById(userId);
        getUserById(otherId);

        if (userId.equals(otherId)) {
            log.warn("Пользователь {} пытается получить общих друзей с самим собой", userId);
            return Set.of();
        }

        return userStorage.getCommonFriends(userId, otherId);
    }

    private void validateUser(User user, String operation) {
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

        if (user.getBirthday() == null) {
            log.error("Ошибка валидации при {}: дата рождения не указана", operation);
            throw new ValidationException("Дата рождения должна быть указана");
        }
        if (user.getBirthday().isAfter(LocalDate.now())) {
            log.error("Ошибка валидации при {}: дата рождения {} в будущем", operation, user.getBirthday());
            throw new ValidationException("Дата рождения не может быть в будущем");
        }
    }

    private void setNameIfBlank(User user, String operation) {
        if (user.getName() == null || user.getName().isBlank()) {
            log.info("Имя пользователя не указано при {}, используем логин: {}", operation, user.getLogin());
            user.setName(user.getLogin());
        }
    }
}
