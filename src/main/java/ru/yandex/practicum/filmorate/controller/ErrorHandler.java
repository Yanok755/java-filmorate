package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;

import javax.validation.ConstraintViolationException;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class ErrorHandler {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFoundException(final NotFoundException e) {
        log.error("Объект не найден: {}", e.getMessage());
        return Map.of("error", "Объект не найден",
                     "errorMessage", e.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidationException(final ValidationException e) {
        log.error("Ошибка валидации: {}", e.getMessage());
        return Map.of("error", "Ошибка валидации",
                     "errorMessage", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleMethodArgumentNotValidException(
            final MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Неизвестная ошибка валидации");

        log.error("Ошибка валидации данных: {}", errorMessage);
        return Map.of("error", "Ошибка валидации данных",
                     "errorMessage", errorMessage);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleConstraintViolationException(
            final ConstraintViolationException e) {
        log.error("Ошибка ограничений: {}", e.getMessage());
        return Map.of("error", "Ошибка ограничений",
                     "errorMessage", e.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleThrowable(final Throwable e) {
        log.error("Произошла непредвиденная ошибка: {}", e.getMessage(), e);
        return Map.of("error", "Произошла непредвиденная ошибка",
                     "errorMessage", e.getMessage());
    }
}
