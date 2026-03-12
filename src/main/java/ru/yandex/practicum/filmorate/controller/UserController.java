package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;

import java.util.Collection;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public User createUser(@RequestBody User user) {
        log.info("Запрос на создание пользователя: {}", user.getEmail());
        User createdUser = userService.createUser(user);
        log.info("Пользователь с id {} успешно создан", createdUser.getId());
        return createdUser;
    }

    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    public User updateUser(@RequestBody User user) {
        log.info("Запрос на обновление пользователя с id: {}", user.getId());
        User updatedUser = userService.updateUser(user);
        log.info("Пользователь с id {} успешно обновлен", updatedUser.getId());
        return updatedUser;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Collection<User> findAllUsers() {
        log.info("Запрос на получение всех пользователей");
        return userService.findAllUsers();
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public User getUserById(@PathVariable Long id) {
        log.info("Запрос на получение пользователя с id: {}", id);
        return userService.getUserById(id);
    }

    @PutMapping("/{id}/friends/{friendId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addFriend(@PathVariable Long id, @PathVariable Long friendId) {
        log.info("Запрос на добавление в друзья: пользователь {} добавляет пользователя {}", id, friendId);
        userService.addFriend(id, friendId);
        log.info("Пользователи {} и {} теперь друзья", id, friendId);
    }

    @DeleteMapping("/{id}/friends/{friendId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFriend(@PathVariable Long id, @PathVariable Long friendId) {
        log.info("Запрос на удаление из друзей: пользователь {} удаляет пользователя {}", id, friendId);
        userService.removeFriend(id, friendId);
        log.info("Пользователи {} и {} больше не друзья", id, friendId);
    }

    @GetMapping("/{id}/friends")
    @ResponseStatus(HttpStatus.OK)
    public Collection<User> getUserFriends(@PathVariable Long id) {
        log.info("Запрос на получение друзей пользователя с id: {}", id);
        return userService.getUserFriends(id);
    }

    @GetMapping("/{id}/friends/common/{otherId}")
    @ResponseStatus(HttpStatus.OK)
    public Collection<User> getCommonFriends(@PathVariable Long id, @PathVariable Long otherId) {
        log.info("Запрос на получение общих друзей пользователей {} и {}", id, otherId);
        return userService.getCommonFriends(id, otherId);
    }
}
