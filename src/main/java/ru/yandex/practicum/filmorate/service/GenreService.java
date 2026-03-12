package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.GenreStorage;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenreService {

    private final GenreStorage genreStorage;

    public List<Genre> getAllGenres() {
        log.debug("Получение всех жанров");
        return genreStorage.findAll();
    }

    public Genre getGenreById(int id) {
        log.debug("Получение жанра с id {}", id);
        return genreStorage.findById(id)
                .orElseThrow(() -> {
                    log.error("Жанр с id {} не найден", id);
                    return new NotFoundException("Жанр с id " + id + " не найден");
                });
    }
}
