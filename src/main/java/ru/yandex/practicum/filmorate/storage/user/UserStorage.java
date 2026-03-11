package ru.yandex.practicum.filmorate.storage.user;

import ru.yandex.practicum.filmorate.model.User;
import java.util.Collection;
import java.util.Optional;

public interface UserStorage {
    User createUser(User user);

    User updateUser(User user);

    Collection<User> findAllUsers();

    Optional<User> getUserById(Long id);

    boolean deleteUser(Long id);

    boolean containsUser(Long id);

    int getUsersCount();
}
