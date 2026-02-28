package ru.yandex.practicum.filmorate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponse;

@AllArgsConstructor
public class NotFoundResponse implements ErrorResponse {
    private HttpStatusCode statusCode;
    @Getter
    private String message;
    @Getter
    private long timestamp;

    @Override
    public HttpStatusCode getStatusCode() {
        return statusCode;
    }

    @Override
    public ProblemDetail getBody() {
        return null;
    }
}
