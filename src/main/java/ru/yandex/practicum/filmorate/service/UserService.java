package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserStorage userStorage;

    public User createUser(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        return userStorage.createUser(user);
    }

    public User updateUser(User user) {
        if (!userStorage.containsUser(user.getId())) {
            throw new NotFoundException("Пользователь с id " + user.getId() + " не найден");
        }
        if (user.getName() == null || user.getName().isBlank()) {
            User existingUser = userStorage.getUserById(user.getId())
                    .orElseThrow(() -> new NotFoundException("Пользователь с id " + user.getId() + " не найден"));
            user.setName(existingUser.getLogin());
        }
        return userStorage.updateUser(user);
    }

    public Collection<User> findAllUsers() {
        return userStorage.findAllUsers();
    }

    public User getUserById(Long id) {
        return userStorage.getUserById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + id + " не найден"));
    }

    public void addFriend(Long userId, Long friendId) {
        // Проверяем существование пользователей
        getUserById(userId);
        getUserById(friendId);

        if (userId.equals(friendId)) {
            throw new ValidationException("Нельзя добавить самого себя в друзья");
        }

        // Вызываем метод интерфейса - никаких instanceof!
        userStorage.addFriend(userId, friendId);
        log.info("Пользователь {} добавил в друзья {}", userId, friendId);
    }

    public void removeFriend(Long userId, Long friendId) {
        // Проверяем существование пользователей
        getUserById(userId);
        getUserById(friendId);

        // Вызываем метод интерфейса
        userStorage.removeFriend(userId, friendId);
        log.info("Пользователь {} удалил из друзей {}", userId, friendId);
    }

    public Collection<User> getUserFriends(Long userId) {
        // Проверяем существование пользователя
        getUserById(userId);

        // Вызываем метод интерфейса
        return userStorage.getUserFriends(userId);
    }

    public Collection<User> getCommonFriends(Long userId, Long otherId) {
        // Проверяем существование пользователей
        getUserById(userId);
        getUserById(otherId);

        // Вызываем метод интерфейса
        Collection<User> commonFriends = userStorage.getCommonFriends(userId, otherId);

        log.info("Найдено {} общих друзей для пользователей {} и {}", 
                commonFriends.size(), userId, otherId);

        return commonFriends;
    }
}
