package ru.yandex.practicum.filmorate.model;

import lombok.Data;
import javax.validation.constraints.*;
import java.time.LocalDate;

@Data
public class Film {
    private Integer id;

    @NotBlank(message = "Название не может быть пустым")
    private String name;

    @Size(max = 200, message = "Максимальная длина описания — 200 символов")
    private String description;

    @NotNull(message = "Дата релиза обязательна")
    private LocalDate releaseDate;

    @Positive(message = "Продолжительность фильма должна быть положительным числом")
    private Integer duration;
}
