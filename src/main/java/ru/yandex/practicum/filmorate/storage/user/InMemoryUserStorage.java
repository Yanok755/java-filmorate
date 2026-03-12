package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class InMemoryUserStorage implements UserStorage {

    private final Map<Long, User> users = new HashMap<>();
    private final Map<Long, Set<Long>> friends = new HashMap<>();
    private long nextId = 1;

    @Override
    public User createUser(User user) {
        user.setId(nextId++);
        users.put(user.getId(), user);
        friends.put(user.getId(), new HashSet<>());
        return user;
    }

    @Override
    public User updateUser(User user) {
        if (!users.containsKey(user.getId())) {
            throw new NotFoundException("Пользователь с id " + user.getId() + " не найден");
        }
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public Collection<User> findAllUsers() {
        return users.values();
    }

    @Override
    public Optional<User> getUserById(Long id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public boolean deleteUser(Long id) {
        if (users.containsKey(id)) {
            users.remove(id);
            friends.remove(id);
            return true;
        }
        return false;
    }

    @Override
    public boolean containsUser(Long id) {
        return users.containsKey(id);
    }

    @Override
    public int getUsersCount() {
        return users.size();
    }

    @Override
    public void addFriend(Long userId, Long friendId) {
        Set<Long> userFriends = friends.computeIfAbsent(userId, k -> new HashSet<>());
        userFriends.add(friendId);
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        Set<Long> userFriends = friends.get(userId);
        if (userFriends != null) {
            userFriends.remove(friendId);
        }
    }

    @Override
    public Collection<User> getUserFriends(Long userId) {
        Set<Long> friendIds = friends.getOrDefault(userId, new HashSet<>());
        return friendIds.stream()
                .map(users::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<User> getCommonFriends(Long userId, Long otherId) {
        Set<Long> userFriends = friends.getOrDefault(userId, new HashSet<>());
        Set<Long> otherFriends = friends.getOrDefault(otherId, new HashSet<>());

        return userFriends.stream()
                .filter(otherFriends::contains)
                .map(users::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
