package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.util.Collection;
import java.util.List;

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
        getUserById(userId);
        getUserById(friendId);
        if (userId.equals(friendId)) {
            throw new ValidationException("Нельзя добавить самого себя в друзья");
        }
        if (userStorage instanceof UserDbStorage) {
            ((UserDbStorage) userStorage).addFriend(userId, friendId);
        }
    }

    public void removeFriend(Long userId, Long friendId) {
        getUserById(userId);
        getUserById(friendId);
        if (userStorage instanceof UserDbStorage) {
            ((UserDbStorage) userStorage).removeFriend(userId, friendId);
        }
    }

    public Collection<User> getUserFriends(Long userId) {
        getUserById(userId);
        if (userStorage instanceof UserDbStorage) {
            List<Long> friendIds = ((UserDbStorage) userStorage).getFriendIds(userId);
            return ((UserDbStorage) userStorage).getFriends(friendIds);
        }
        return List.of();
    }

    public Collection<User> getCommonFriends(Long userId, Long otherId) {
        getUserById(userId);
        getUserById(otherId);
        if (userStorage instanceof UserDbStorage) {
            List<Long> userFriends = ((UserDbStorage) userStorage).getFriendIds(userId);
            List<Long> otherFriends = ((UserDbStorage) userStorage).getFriendIds(otherId);

            List<Long> commonIds = userFriends.stream()
                    .filter(otherFriends::contains)
                    .toList();

            return ((UserDbStorage) userStorage).getFriends(commonIds);
        }
        return List.of();
    }
}
