package ru.yandex.practicum.filmorate;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
@AllArgsConstructor
public class ErrorResponse {
    private int statusCode;
    private String message;
    private long timestamp;
    private String error;
    private String path;
    
    // Конструктор для простых случаев
    public ErrorResponse(int statusCode, String message, long timestamp) {
        this.statusCode = statusCode;
        this.message = message;
        this.timestamp = timestamp;
        this.error = HttpStatus.valueOf(statusCode).getReasonPhrase();
        this.path = "";
    }
    
    // Конструктор с дополнительной информацией
    public ErrorResponse(HttpStatus status, String message, long timestamp, String path) {
        this.statusCode = status.value();
        this.message = message;
        this.timestamp = timestamp;
        this.error = status.getReasonPhrase();
        this.path = path;
    }
    
    // Статические фабричные методы для удобства создания
    public static ErrorResponse of(HttpStatus status, String message) {
        return new ErrorResponse(status.value(), message, System.currentTimeMillis());
    }
    
    public static ErrorResponse of(HttpStatus status, String message, String path) {
        return new ErrorResponse(status, message, System.currentTimeMillis(), path);
    }
    
    public static ErrorResponse notFound(String message) {
        return new ErrorResponse(HttpStatus.NOT_FOUND.value(), message, System.currentTimeMillis());
    }
    
    public static ErrorResponse badRequest(String message) {
        return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message, System.currentTimeMillis());
    }
    
    public static ErrorResponse internalError(String message) {
        return new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), message, System.currentTimeMillis());
    }
}
