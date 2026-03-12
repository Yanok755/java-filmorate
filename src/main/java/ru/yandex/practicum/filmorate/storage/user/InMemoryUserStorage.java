package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
@Qualifier("userInMemoryStorage")
public class InMemoryUserStorage implements UserStorage {

    private final Map<Long, User> users = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, FriendshipStatus>> friendships = new ConcurrentHashMap<>();
    private long currentId = 0;

    @Override
    public User createUser(User user) {
        user.setId(getNextId());
        users.put(user.getId(), user);
        friendships.put(user.getId(), new ConcurrentHashMap<>());

        log.debug("Пользователь сохранен: id={}", user.getId());
        return user;
    }

    @Override
    public User updateUser(User user) {
        User existingUser = users.get(user.getId());
        if (existingUser == null) {
            throw new NotFoundException("Пользователь с id " + user.getId() + " не найден");
        }

        existingUser.setEmail(user.getEmail());
        existingUser.setLogin(user.getLogin());
        existingUser.setName(user.getName());
        existingUser.setBirthday(user.getBirthday());

        log.debug("Пользователь обновлен: id={}", user.getId());
        return existingUser;
    }

    @Override
    public Collection<User> findAllUsers() {
        log.debug("Получены все пользователи. Всего: {} пользователей", users.size());
        return users.values();
    }

    @Override
    public Optional<User> getUserById(Long id) {
        log.trace("Поиск пользователя по id: {}", id);
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public boolean deleteUser(Long id) {
        if (users.containsKey(id)) {
            users.remove(id);
            friendships.remove(id);
            // Удаляем пользователя из списков друзей других пользователей
            friendships.values().forEach(friends -> friends.remove(id));
            log.debug("Пользователь удален: id={}", id);
            return true;
        }
        log.warn("Попытка удалить несуществующего пользователя: id={}", id);
        return false;
    }

    @Override
    public boolean containsUser(Long id) {
        boolean exists = users.containsKey(id);
        log.trace("Проверка существования пользователя id={}: {}", id, exists);
        return exists;
    }

    @Override
    public int getUsersCount() {
        int count = users.size();
        log.trace("Текущее количество пользователей: {}", count);
        return count;
    }

    @Override
    public void addFriend(Long userId, Long friendId) {
        validateUsersExist(userId, friendId);

        friendships.get(userId).put(friendId, FriendshipStatus.PENDING);
        friendships.get(friendId).put(userId, FriendshipStatus.PENDING);

        log.debug("Друг добавлен: пользователь {} добавил пользователя {}", userId, friendId);
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        validateUsersExist(userId, friendId);

        friendships.get(userId).remove(friendId);
        friendships.get(friendId).remove(userId);

        log.debug("Друг удален: пользователь {} удалил пользователя {}", userId, friendId);
    }

    @Override
    public Collection<Long> getFriendIds(Long userId) {
        validateUserExists(userId);
        return friendships.get(userId).keySet();
    }

    @Override
    public Map<Long, FriendshipStatus> getFriendsWithStatus(Long userId) {
        validateUserExists(userId);
        return new HashMap<>(friendships.get(userId));
    }

    @Override
    public Collection<User> getFriendsAsUsers(Long userId) {
        validateUserExists(userId);
        return friendships.get(userId).keySet().stream()
                .map(users::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<User> getCommonFriends(Long userId, Long otherId) {
        validateUsersExist(userId, otherId);

        Set<Long> commonFriendIds = friendships.get(userId).keySet().stream()
                .filter(friendships.get(otherId).keySet()::contains)
                .collect(Collectors.toSet());

        return commonFriendIds.stream()
                .map(users::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public boolean areFriends(Long userId, Long otherId) {
        validateUsersExist(userId, otherId);
        return friendships.get(userId).containsKey(otherId);
    }

    @Override
    public int getFriendsCount(Long userId) {
        validateUserExists(userId);
        return friendships.get(userId).size();
    }

    @Override
    public void updateFriendshipStatus(Long userId, Long friendId, FriendshipStatus status) {
        validateUsersExist(userId, friendId);

        friendships.get(userId).put(friendId, status);
        friendships.get(friendId).put(userId, status);

        log.debug("Статус дружбы обновлен: пользователи {} и {}, статус {}", userId, friendId, status);
    }

    private long getNextId() {
        return ++currentId;
    }

    private void validateUserExists(Long userId) {
        if (!users.containsKey(userId)) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }
    }

    private void validateUsersExist(Long userId, Long otherId) {
        validateUserExists(userId);
        validateUserExists(otherId);
    }
}
