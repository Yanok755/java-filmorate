package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.User;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class InMemoryUserStorage implements UserStorage {

    private final Map<Long, User> users = new HashMap<>();
    private long currentId = 0;

    @Override
    public User createUser(User user) {
        user.setId(getNextId());
        users.put(user.getId(), user);

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

    private long getNextId() {
        return ++currentId;
    }
}
