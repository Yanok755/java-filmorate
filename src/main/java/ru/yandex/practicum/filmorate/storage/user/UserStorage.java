package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.User;
import java.util.Collection;
import java.util.Set;

public interface UserStorage {
    User createUser(User user);

    User updateUser(User user);

    Collection<User> findAllUsers();

    User getUserById(Long id);

    boolean containsUser(Long id);

    // Методы для работы с друзьями
    void addFriend(Long userId, Long friendId);

    void removeFriend(Long userId, Long friendId);

    Set<Long> getUserFriends(Long userId);
}
