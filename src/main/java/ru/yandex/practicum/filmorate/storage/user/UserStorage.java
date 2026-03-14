package ru.yandex.practicum.filmorate.storage.user;

import ru.yandex.practicum.filmorate.model.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserStorage {

    User createUser(User user);

    User updateUser(User user);

    Collection<User> findAllUsers();

    Optional<User> getUserById(Long id);

    boolean deleteUser(Long id);

    boolean containsUser(Long id);

    int getUsersCount();

    void addFriend(Long userId, Long friendId);

    void removeFriend(Long userId, Long friendId);

    Collection<User> getUserFriends(Long userId);

    Collection<User> getCommonFriends(Long userId, Long otherUserId);
}
