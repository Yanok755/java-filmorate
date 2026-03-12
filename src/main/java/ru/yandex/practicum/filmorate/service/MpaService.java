package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.MpaStorage;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MpaService {

    private final MpaStorage mpaStorage;

    public List<Mpa> getAllMpa() {
        log.debug("Получение всех рейтингов MPA");
        return mpaStorage.findAll();
    }

    public Mpa getMpaById(Integer id) {
        log.debug("Получение рейтинга MPA с id {}", id);
        return mpaStorage.findById(id)
                .orElseThrow(() -> {
                    log.error("Рейтинг MPA с id {} не найден", id);
                    return new NotFoundException("Рейтинг MPA с id " + id + " не найден");
                });
    }

    public boolean existsById(Integer id) {
        log.debug("Проверка существования рейтинга MPA с id {}", id);
        return mpaStorage.existsById(id);
    }
}
