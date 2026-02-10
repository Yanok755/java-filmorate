package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/users")
@Slf4j
public class UserController {
    private final Map<Integer, User> users = new HashMap<>();
    private Integer idCounter = 1;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public User createUser(@Valid @RequestBody User user) {
        validateUser(user);

        user.setId(idCounter++);
        users.put(user.getId(), user);

        log.info("Добавлен пользователь: {}", user);
        return user;
    }

    @PutMapping
    public User updateUser(@Valid @RequestBody User user) {
        if (user.getId() == null) {
            log.error("ID пользователя не указан");
            throw new ValidationException("ID пользователя не указан");
        }

        if (!users.containsKey(user.getId())) {
            log.error("Пользователь с id {} не найден", user.getId());
            throw new NotFoundException("Пользователь с указанным id не найден");
        }

        validateUser(user);
        users.put(user.getId(), user);

        log.info("Обновлен пользователь: {}", user);
        return user;
    }

    @GetMapping
    public List<User> getAllUsers() {
        log.info("Получен список всех пользователей, количество: {}", users.size());
        return new ArrayList<>(users.values());
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable Integer id) {
        User user = users.get(id);
        if (user == null) {
            log.error("Пользователь с id {} не найден", id);
            throw new NotFoundException("Пользователь с указанным id не найден");
        }

        log.info("Получен пользователь с id {}", id);
        return user;
    }

    private void validateUser(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank() || !user.getEmail().contains("@")) {
            log.error("Электронная почта не может быть пустой и должна содержать символ @");
            throw new ValidationException("Электронная почта не может быть пустой и должна содержать символ @");
        }

        if (user.getLogin() == null || user.getLogin().isBlank()) {
            log.error("Логин не может быть пустым");
            throw new ValidationException("Логин не может быть пустым");
        }

        if (user.getLogin().contains(" ")) {
            log.error("Логин не может содержать пробелы");
            throw new ValidationException("Логин не может содержать пробелы");
        }

        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            log.error("Дата рождения не может быть в будущем");
            throw new ValidationException("Дата рождения не может быть в будущем");
        }
    }
}
