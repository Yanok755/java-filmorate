package ru.yandex.practicum.filmorate;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;
import jakarta.servlet.http.HttpServletRequest;  // Добавить импорт

@Data
@AllArgsConstructor
public class ErrorResponse {
    private int statusCode;
    private String message;
    private long timestamp;
    private String error;
    private String path;

    public ErrorResponse(int statusCode, String message, long timestamp) {
        this.statusCode = statusCode;
        this.message = message;
        this.timestamp = timestamp;
        this.error = HttpStatus.valueOf(statusCode).getReasonPhrase();
        this.path = "";
    }

    public ErrorResponse(HttpStatus status, String message, long timestamp, String path) {
        this.statusCode = status.value();
        this.message = message;
        this.timestamp = timestamp;
        this.error = status.getReasonPhrase();
        this.path = path;
    }

    public static ErrorResponse of(HttpStatus status, String message) {
        return new ErrorResponse(status.value(), message, System.currentTimeMillis());
    }

    public static ErrorResponse of(HttpStatus status, String message, String path) {
        return new ErrorResponse(status, message, System.currentTimeMillis(), path);
    }

    public static ErrorResponse notFound(String message) {
        return new ErrorResponse(HttpStatus.NOT_FOUND.value(), message, System.currentTimeMillis());
    }

    public static ErrorResponse notFound(String message, HttpServletRequest request) {  // Добавить с путем
        return new ErrorResponse(HttpStatus.NOT_FOUND, message, System.currentTimeMillis(), request.getRequestURI());
    }

    public static ErrorResponse badRequest(String message) {
        return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message, System.currentTimeMillis());
    }

    public static ErrorResponse badRequest(String message, HttpServletRequest request) {  // Добавить с путем
        return new ErrorResponse(HttpStatus.BAD_REQUEST, message, System.currentTimeMillis(), request.getRequestURI());
    }

    public static ErrorResponse internalError(String message) {
        return new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), message, System.currentTimeMillis());
    }

    public static ErrorResponse internalError(String message, HttpServletRequest request) {  // Добавить с путем
        return new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, message, System.currentTimeMillis(), request.getRequestURI());
    }
}
