package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class User {
    private Long id;

    @NotBlank
    @Email(message = "Введите почту в корректном формате")
    private String email;

    @NotEmpty(message = "Логин не может быть пустым")
    @Pattern(regexp = "\\S+", message = "Логин должен состоять из одного слова, без пробелов")
    private String login;

    private String name;

    @PastOrPresent(message = "Дата рождения не может быть позже сегодняшнего дня")
    private LocalDate birthday;
}
