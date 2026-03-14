package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class InMemoryUserStorage implements UserStorage {

    private final Map<Long, User> users = new HashMap<>();
    private final Map<Long, Set<Long>> friendships = new HashMap<>(); // userId -> Set of friendIds
    private long currentId = 0;

    @Override
    public User createUser(User user) {
        user.setId(getNextId());
        users.put(user.getId(), user);
        friendships.put(user.getId(), new HashSet<>()); // Инициализируем пустой список друзей

        log.debug("Пользователь сохранен: id={}", user.getId());
        return user;
    }

    @Override
    public User updateUser(User user) {
        User existingUser = users.get(user.getId());

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
            friendships.remove(id); // Удаляем и его друзей
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
        if (!users.containsKey(userId) || !users.containsKey(friendId)) {
            throw new IllegalArgumentException("Пользователь не найден");
        }

        friendships.computeIfAbsent(userId, k -> new HashSet<>()).add(friendId);
        log.debug("Добавлен друг: пользователь {} добавил {}", userId, friendId);
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        if (friendships.containsKey(userId)) {
            friendships.get(userId).remove(friendId);
            log.debug("Удален друг: пользователь {} удалил {}", userId, friendId);
        }
    }

    @Override
    public Collection<User> getUserFriends(Long userId) {
        Set<Long> friendIds = friendships.getOrDefault(userId, Collections.emptySet());

        return friendIds.stream()
                .map(users::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<User> getCommonFriends(Long userId, Long otherUserId) {
        Set<Long> userFriends = friendships.getOrDefault(userId, Collections.emptySet());
        Set<Long> otherFriends = friendships.getOrDefault(otherUserId, Collections.emptySet());

        // Находим пересечение множеств
        Set<Long> commonFriendIds = userFriends.stream()
                .filter(otherFriends::contains)
                .collect(Collectors.toSet());

        log.debug("Найдено {} общих друзей для пользователей {} и {}", 
                commonFriendIds.size(), userId, otherUserId);

        // Получаем объекты пользователей
        return commonFriendIds.stream()
                .map(users::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private long getNextId() {
        return ++currentId;
    }
}
