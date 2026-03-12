package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserService {

    private final UserStorage userStorage;

    @Autowired
    public UserService(@Qualifier("userDbStorage") UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public User createUser(User user) {
        log.debug("Создание пользователя с email: {}", user.getEmail());
        validateUser(user, "создании");
        normalizeUserName(user);

        User createdUser = userStorage.createUser(user);
        log.info("Пользователь {} успешно создан", createdUser.getId());
        return createdUser;
    }

    public User updateUser(User user) {
        if (user.getId() == null) {
            log.error("ID пользователя не может быть null при обновлении");
            throw new ValidationException("ID пользователя должен быть указан");
        }

        log.debug("Обновление пользователя с id: {}", user.getId());

        // Проверяем существование пользователя
        if (!userStorage.containsUser(user.getId())) {
            log.error("Пользователь с id {} не найден при обновлении", user.getId());
            throw new NotFoundException("Пользователь с id " + user.getId() + " не найден");
        }

        validateUser(user, "обновлении");
        normalizeUserName(user);

        User updatedUser = userStorage.updateUser(user);
        log.info("Пользователь {} успешно обновлен", updatedUser.getId());
        return updatedUser;
    }

    public Collection<User> findAllUsers() {
        log.debug("Получение всех пользователей");
        return userStorage.findAllUsers();
    }

    public User getUserById(Long id) {
        log.debug("Получение пользователя с id: {}", id);
        return userStorage.getUserById(id);
    }

    public void addFriend(Long userId, Long friendId) {
        log.debug("Добавление друга {} пользователю {}", friendId, userId);

        validateUserExists(userId);
        validateUserExists(friendId);

        userStorage.addFriend(userId, friendId);
        log.info("Друг {} добавлен пользователю {}", friendId, userId);
    }

    public void removeFriend(Long userId, Long friendId) {
        log.debug("Удаление друга {} у пользователя {}", friendId, userId);

        validateUserExists(userId);
        validateUserExists(friendId);

        userStorage.removeFriend(userId, friendId);
        log.info("Друг {} удален у пользователя {}", friendId, userId);
    }

    public Collection<User> getUserFriends(Long userId) {
        log.debug("Получение друзей пользователя {}", userId);

        validateUserExists(userId);

        Set<Long> friendIds = userStorage.getUserFriends(userId);
        return friendIds.stream()
                .map(userStorage::getUserById)
                .collect(Collectors.toList());
    }

    public Collection<User> getCommonFriends(Long userId, Long otherId) {
        log.debug("Получение общих друзей пользователей {} и {}", userId, otherId);

        validateUserExists(userId);
        validateUserExists(otherId);

        Set<Long> userFriends = userStorage.getUserFriends(userId);
        Set<Long> otherFriends = userStorage.getUserFriends(otherId);

        return userFriends.stream()
                .filter(otherFriends::contains)
                .map(userStorage::getUserById)
                .collect(Collectors.toList());
    }

    private void validateUserExists(Long userId) {
        if (!userStorage.containsUser(userId)) {
            log.error("Пользователь с id {} не найден", userId);
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }
    }

    private void validateUser(User user, String operation) {
        // Валидация email
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            log.error("Ошибка валидации при {}: электронная почта не должна быть пустой", operation);
            throw new ValidationException("Электронная почта не должна быть пустой");
        }
        if (!user.getEmail().contains("@")) {
            log.error("Ошибка валидации при {}: email {} не содержит символ @", operation, user.getEmail());
            throw new ValidationException("Электронная почта должна содержать символ @");
        }

        // Валидация логина
        if (user.getLogin() == null || user.getLogin().isBlank()) {
            log.error("Ошибка валидации при {}: логин не должен быть пустым", operation);
            throw new ValidationException("Логин не может быть пустым");
        }
        if (user.getLogin().contains(" ")) {
            log.error("Ошибка валидации при {}: логин {} содержит пробелы", operation, user.getLogin());
            throw new ValidationException("Логин не может содержать пробелы");
        }

        // Валидация даты рождения
        if (user.getBirthday() == null) {
            log.error("Ошибка валидации при {}: дата рождения не указана", operation);
            throw new ValidationException("Дата рождения должна быть указана");
        }
        if (user.getBirthday().isAfter(LocalDate.now())) {
            log.error("Ошибка валидации при {}: дата рождения {} в будущем", operation, user.getBirthday());
            throw new ValidationException("Дата рождения не может быть в будущем");
        }
    }

    private void normalizeUserName(User user) {
        // Если имя не указано, используем логин
        if (user.getName() == null || user.getName().isBlank()) {
            String oldName = user.getName();
            user.setName(user.getLogin());
            log.debug("Имя пользователя изменено с '{}' на логин '{}'", oldName, user.getLogin());
        }
    }
}
