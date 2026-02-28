package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {

    private final Map<Long, User> users = new HashMap<>();

    @PostMapping
    public User createUser(@RequestBody User user) {
        log.info("Запрос на создание пользователя: {}", user);

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            log.error("Ошибка валидации при создании: электронная почта не должна быть пустой");
            throw new ValidationException("Электронная почта не должна быть пустой");
        }

        if (!user.getEmail().contains("@")) {
            log.error("Ошибка валидации при создании: электронная почта {} не содержит символ @", user.getEmail());
            throw new ValidationException("Электронная почта должна содержать символ @");
        }

        if (user.getLogin() == null || user.getLogin().isBlank()) {
            log.error("Ошибка валидации при создании: логин не должен быть пустым");
            throw new ValidationException("Логин не может быть пустым");
        }

        if (user.getLogin().contains(" ")) {
            log.error("Ошибка валидации при создании: логин {} содержит пробелы", user.getLogin());
            throw new ValidationException("Логин не может содержать пробелы");
        }

        if (user.getName() == null || user.getName().isBlank()) {
            log.info("Имя пользователя не указано при создании, используем логин: {}", user.getLogin());
            user.setName(user.getLogin());
        }

        if (user.getBirthday().isAfter(LocalDate.now())) {
            log.error("Ошибка валидации при создании: дата рождения {} в будущем", user.getBirthday());
            throw new ValidationException("Дата рождения не может быть в будущем");
        }

        user.setId(getNextId());
        users.put(user.getId(), user);

        log.info("Пользователь {} успешно создан", user.getId());

        return user;
    }

    @PutMapping
    public User updateUser(@RequestBody User newUser) {
        log.info("Запрос на обновление пользователя: {}", newUser.getId());

        if (!users.containsKey(newUser.getId())) {
            log.error("Пользователь с id {} не найден", newUser.getId());
            throw new ValidationException("Пользователь с id " + newUser.getId() + " не найден");
        }

        if (newUser.getEmail() == null || newUser.getEmail().isBlank()) {
            log.error("Ошибка валидации при обновлении: электронная почта не должна быть пустой");
            throw new ValidationException("Электронная почта не должна быть пустой");
        }

        if (!newUser.getEmail().contains("@")) {
            log.error("Ошибка валидации при обновлении: email {} не содержит символ @", newUser.getEmail());
            throw new ValidationException("Электронная почта должна содержать символ @");
        }

        if (newUser.getLogin() == null || newUser.getLogin().isBlank()) {
            log.error("Ошибка валидации при обновлении: логин не должен быть пустым");
            throw new ValidationException("Логин не может быть пустым");
        }

        if (newUser.getLogin().contains(" ")) {
            log.error("Ошибка валидации при обновлении: логин {} содержит пробелы", newUser.getLogin());
            throw new ValidationException("Логин не может содержать пробелы");
        }

        if (newUser.getName() == null || newUser.getName().isBlank()) {
            log.info("Имя пользователя не указано при обновлении, используем логин: {}", newUser.getLogin());
            newUser.setName(newUser.getLogin());
        }

        if (newUser.getBirthday().isAfter(LocalDate.now())) {
            log.error("Ошибка валидации при обновлении: дата рождения {} в будущем", newUser.getBirthday());
            throw new ValidationException("Дата рождения не может быть в будущем");
        }

        User oldUser = users.get(newUser.getId());

        oldUser.setEmail(newUser.getEmail());
        oldUser.setLogin(newUser.getLogin());
        oldUser.setName(newUser.getName());
        oldUser.setBirthday(newUser.getBirthday());

        log.info("Пользователь {} успешно обновлен", newUser.getId());

        return oldUser;
    }

    @GetMapping
    public Collection<User> findAllUsers() {
        log.info("Получен запрос на получение всех пользователей. Всего пользователей: {}", users.size());
        return users.values();
    }

    private long getNextId() {
        long currentMaxId = users.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }
}
