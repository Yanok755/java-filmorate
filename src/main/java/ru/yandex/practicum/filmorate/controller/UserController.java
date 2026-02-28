package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.Exceptions.ValidationException;
import ru.yandex.practicum.filmorate.NotFoundResponse;
import ru.yandex.practicum.filmorate.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {
    private final Map<Long, User> users = new HashMap<>();

    @GetMapping
    public ResponseEntity<List<User>> getUsers() {
        log.info("Вызван эндпоинт на получение всех пользователей");
        return ResponseEntity.ok(new ArrayList<>(users.values()));
    }

    @PostMapping
    public ResponseEntity<User> addUser(@Valid @RequestBody User user) {
        log.info("Вызван эндпоинт на создание нового пользователя");

        if (user == null) {
            log.warn("Пустой запрос");
            throw new ValidationException("Запрос некорректен");
        }

        Long id = generateNextId();
        log.debug("Сгенерирован новый id - {}", id);
        user.setId(id);

        if (user.getName() == null || user.getName().isEmpty()) {
            user.setName(user.getLogin());
            log.debug("Имя пусто, используется логин - {}", user.getName());
        }

        users.put(user.getId(), user);
        log.info("Успешно добавлен новый пользователь с id = {}", user.getId());

        return ResponseEntity.ok(user);
    }

    @PutMapping
    public ResponseEntity<?> updateUser(@Valid @RequestBody User user) {
        log.info("Вызван эндпоинт на обновление данных пользователя");

        validateRequestBody(user);
        log.trace("Валидация запроса прошла успешно");

        User oldUser = users.get(user.getId());

        if (oldUser == null) {
            log.warn("Не найдено фильмов с указанным id - {}", user.getId());
            NotFoundResponse error = new NotFoundResponse(HttpStatusCode.valueOf(404), "Не найдено пользователей с указанным id", System.currentTimeMillis());
            return ResponseEntity.status(404).body(error);
        }

        if (user.getName() != null && !user.getName().isEmpty()) {
            oldUser.setName(user.getName());
            log.debug("Обновили имя пользователя - {}", oldUser.getName());
        }

        if (user.getBirthday() != null) {
            oldUser.setBirthday(user.getBirthday());
            log.debug("Обновили дату рождения пользователя - {}", oldUser.getBirthday());
        }

        if (user.getEmail() != null) {
            oldUser.setEmail(user.getEmail());
            log.debug("Обновили почту пользователя - {}", oldUser.getEmail());
        }

        if (user.getLogin() != null) {
            oldUser.setLogin(user.getLogin());
            log.debug("Обновили логин пользователя - {}", oldUser.getLogin());
        }

        log.info("Данные пользователя с id = {} успешно обновлены", user.getId());

        return ResponseEntity.ok(oldUser);
    }

    private void validateRequestBody(User user) {
        if (user.getId() == null) {
            log.warn("Отсутствует id");
            throw new ValidationException("Укажите id для обновления пользователя");
        }
    }

    private Long generateNextId() {
        log.trace("Генерация нового id");
        long currentId = users
                .keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0L);

        return currentId + 1;
    }
}
