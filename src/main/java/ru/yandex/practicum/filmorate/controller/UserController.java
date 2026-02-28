package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exceptions.ValidationException;
import ru.yandex.practicum.filmorate.ErrorResponse;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
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

        // Валидация email
        if (user.getEmail() == null || user.getEmail().isBlank() || !user.getEmail().contains("@")) {
            log.warn("Проблема с полем 'Email'");
            throw new ValidationException("Электронная почта не может быть пустой и должна содержать символ @");
        }

        // Валидация login
        if (user.getLogin() == null || user.getLogin().isBlank() || user.getLogin().contains(" ")) {
            log.warn("Проблема с полем 'Login'");
            throw new ValidationException("Логин не может быть пустым и содержать пробелы");
        }

        // Валидация даты рождения
        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            log.warn("Проблема с полем 'Дата рождения'");
            throw new ValidationException("Дата рождения не может быть в будущем");
        }

        Long id = generateNextId();
        log.debug("Сгенерирован новый id - {}", id);
        user.setId(id);

        if (user.getName() == null || user.getName().isBlank()) {
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
            log.warn("Не найдено пользователей с указанным id - {}", user.getId());
            // Используем ErrorResponse вместо NotFoundResponse
            ErrorResponse error = ErrorResponse.notFound("Не найдено пользователей с указанным id");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        // Обновление с валидацией
        if (user.getEmail() != null) {
            if (!user.getEmail().contains("@")) {
                log.warn("Проблема с полем 'Email' при обновлении");
                throw new ValidationException("Электронная почта должна содержать символ @");
            }
            oldUser.setEmail(user.getEmail());
            log.debug("Обновили почту пользователя - {}", oldUser.getEmail());
        }

        if (user.getLogin() != null) {
            if (user.getLogin().contains(" ")) {
                log.warn("Проблема с полем 'Login' при обновлении");
                throw new ValidationException("Логин не может содержать пробелы");
            }
            oldUser.setLogin(user.getLogin());
            log.debug("Обновили логин пользователя - {}", oldUser.getLogin());
        }

        if (user.getName() != null && !user.getName().isBlank()) {
            oldUser.setName(user.getName());
            log.debug("Обновили имя пользователя - {}", oldUser.getName());
        }

        if (user.getBirthday() != null) {
            if (user.getBirthday().isAfter(LocalDate.now())) {
                log.warn("Проблема с полем 'Дата рождения' при обновлении");
                throw new ValidationException("Дата рождения не может быть в будущем");
            }
            oldUser.setBirthday(user.getBirthday());
            log.debug("Обновили дату рождения пользователя - {}", oldUser.getBirthday());
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

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException e) {
        log.error("Validation error: {}", e.getMessage());
        ErrorResponse error = ErrorResponse.badRequest(e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception e) {
        log.error("Unexpected error: {}", e.getMessage());
        ErrorResponse error = ErrorResponse.internalError("Internal server error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
