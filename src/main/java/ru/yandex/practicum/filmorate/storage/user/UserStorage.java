package ru.yandex.practicum.filmorate.storage.user;

import ru.yandex.practicum.filmorate.model.FriendshipStatus;
import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface UserStorage {
    User createUser(User user);

    User updateUser(User user);

    Collection<User> findAllUsers();

    Optional<User> getUserById(Long id);

    boolean deleteUser(Long id);

    boolean containsUser(Long id);

    int getUsersCount();

    // Методы для работы с друзьями
    void addFriend(Long userId, Long friendId);

    void removeFriend(Long userId, Long friendId);

    Collection<Long> getFriendIds(Long userId);

    Map<Long, FriendshipStatus> getFriendsWithStatus(Long userId);

    Collection<User> getFriendsAsUsers(Long userId);

    Collection<User> getCommonFriends(Long userId, Long otherId);

    boolean areFriends(Long userId, Long otherId);

    int getFriendsCount(Long userId);

    void updateFriendshipStatus(Long userId, Long friendId, FriendshipStatus status);
}

