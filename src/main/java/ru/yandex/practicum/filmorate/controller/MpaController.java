package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.MpaStorage;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/mpa")
@RequiredArgsConstructor
public class MpaController {

    private final MpaStorage mpaStorage;

    @GetMapping
    public List<Mpa> getAllMpa() {
        log.info("Запрос на получение всех рейтингов MPA");
        return mpaStorage.findAll();
    }

    @GetMapping("/{id}")
    public Mpa getMpaById(@PathVariable int id) {
        log.info("Запрос на получение рейтинга MPA с id {}", id);
        return mpaStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Рейтинг mpa с id " + id + " не найден"));
    }
}
