package ru.yandex.practicum.filmorate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FilmorateApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Film validFilm;
    private User validUser;

    private static int userCounter = 0;
    private static int filmCounter = 0;

    @BeforeEach
    void setUp() {
        userCounter++;
        filmCounter++;

        validFilm = new Film();
        validFilm.setName("Test Film " + filmCounter);
        validFilm.setDescription("Test Description " + filmCounter);
        validFilm.setReleaseDate(LocalDate.of(2000, 1, 1));
        validFilm.setDuration(120);

        validUser = new User();
        validUser.setEmail("test" + userCounter + "@mail.com");
        validUser.setLogin("testlogin" + userCounter);
        validUser.setBirthday(LocalDate.of(1990, 1, 1));
    }

    // Тесты для фильмов
    @Test
    void createValidFilm() throws Exception {
        mockMvc.perform(post("/films")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validFilm)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(validFilm.getName()));
    }

    @Test
    void createFilmWithEmptyName() throws Exception {
        Film film = new Film();
        film.setName("");
        film.setDescription("Test Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        mockMvc.perform(post("/films")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(film)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createFilmWithTooLongDescription() throws Exception {
        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("A".repeat(201)); // 201 символ
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        mockMvc.perform(post("/films")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(film)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createFilmWithOldReleaseDate() throws Exception {
        Film film = new Film();
        film.setName("Old Film");
        film.setDescription("Test");
        film.setReleaseDate(LocalDate.of(1890, 1, 1)); // До 1895-12-28
        film.setDuration(120);

        mockMvc.perform(post("/films")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(film)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage")
                        .value("Дата релиза не может быть раньше 28 декабря 1895 года"));
    }

    @Test
    void createFilmWithNegativeDuration() throws Exception {
        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("Test Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(-10);

        mockMvc.perform(post("/films")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(film)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateFilm() throws Exception {
        // Сначала создаем фильм
        String filmJson = mockMvc.perform(post("/films")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validFilm)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Film createdFilm = objectMapper.readValue(filmJson, Film.class);

        // Обновляем фильм
        createdFilm.setName("Updated Film");
        createdFilm.setDescription("Updated Description");
        createdFilm.setDuration(150);

        mockMvc.perform(put("/films")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createdFilm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Film"))
                .andExpect(jsonPath("$.description").value("Updated Description"))
                .andExpect(jsonPath("$.duration").value(150));
    }

    @Test
    void updateNonExistentFilm() throws Exception {
        validFilm.setId(9999); // Несуществующий ID

        mockMvc.perform(put("/films")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validFilm)))
                .andExpect(status().isNotFound()) // Должен быть 404
                .andExpect(jsonPath("$.error")
                        .value("Объект не найден"));
    }

    @Test
    void getAllFilms() throws Exception {
        // Создаем фильм с уникальным именем
        Film uniqueFilm = new Film();
        uniqueFilm.setName("Unique Film for GetAll");
        uniqueFilm.setDescription("Unique Description");
        uniqueFilm.setReleaseDate(LocalDate.of(2000, 1, 1));
        uniqueFilm.setDuration(120);

        mockMvc.perform(post("/films")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(uniqueFilm)))
                .andExpect(status().isCreated());

        // Получаем все фильмы
        mockMvc.perform(get("/films"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)));
    }

    // Тесты для пользователей
    @Test
    void createValidUser() throws Exception {
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(validUser.getEmail()))
                .andExpect(jsonPath("$.login").value(validUser.getLogin()));
    }

    @Test
    void createUserWithInvalidEmail() throws Exception {
        User user = new User();
        user.setEmail("invalid-email"); // Нет @
        user.setLogin("testlogin");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUserWithEmptyEmail() throws Exception {
        User user = new User();
        user.setEmail(""); // Пустой email
        user.setLogin("testlogin");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUserWithEmptyLogin() throws Exception {
        User user = new User();
        user.setEmail("test@mail.com");
        user.setLogin(""); // Пустой login
        user.setBirthday(LocalDate.of(1990, 1, 1));

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUserWithLoginContainingSpaces() throws Exception {
        User user = new User();
        user.setEmail("test@mail.com");
        user.setLogin("test login"); // Пробел в логине
        user.setBirthday(LocalDate.of(1990, 1, 1));

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUserWithEmptyName() throws Exception {
        User user = new User();
        user.setEmail("empty@mail.com");
        user.setLogin("emptylogin");
        user.setName(""); // Пустое имя
        user.setBirthday(LocalDate.of(1990, 1, 1));

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("emptylogin")); // Должен подставиться login
    }

    @Test
    void createUserWithNullName() throws Exception {
        User user = new User();
        user.setEmail("null@mail.com");
        user.setLogin("nulllogin");
        user.setName(null); // null имя
        user.setBirthday(LocalDate.of(1990, 1, 1));

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("nulllogin")); // Должен подставиться login
    }

    @Test
    void createUserWithFutureBirthday() throws Exception {
        User user = new User();
        user.setEmail("future@mail.com");
        user.setLogin("futurelogin");
        user.setName("Future User");
        user.setBirthday(LocalDate.now().plusDays(1)); // Дата в будущем

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUser() throws Exception {
        // Сначала создаем пользователя
        String userJson = mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUser)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        User createdUser = objectMapper.readValue(userJson, User.class);

        // Обновляем пользователя
        createdUser.setEmail("updated@mail.com");
        createdUser.setLogin("updatedlogin");
        createdUser.setName("Updated Name");

        mockMvc.perform(put("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createdUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("updated@mail.com"))
                .andExpect(jsonPath("$.login").value("updatedlogin"))
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }

    @Test
    void updateNonExistentUser() throws Exception {
        validUser.setId(9999); // Несуществующий ID

        mockMvc.perform(put("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUser)))
                .andExpect(status().isNotFound()) // Должен быть 404, а не 400
                .andExpect(jsonPath("$.error")
                        .value("Объект не найден"));
    }

    @Test
    void updateUserWithoutId() throws Exception {
        User user = new User();
        user.setEmail("no_id@mail.com");
        user.setLogin("noidlogin");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        // ID не установлен

        mockMvc.perform(put("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isBadRequest()); // Должен быть 400
    }

    @Test
    void getAllUsers() throws Exception {
        // Создаем пользователя с уникальным email
        User uniqueUser = new User();
        uniqueUser.setEmail("allusers@mail.com");
        uniqueUser.setLogin("alluserslogin");
        uniqueUser.setName("All Users Test");
        uniqueUser.setBirthday(LocalDate.of(1990, 1, 1));

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(uniqueUser)))
                .andExpect(status().isCreated());

        // Получаем всех пользователей
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void getUserById() throws Exception {
        // Создаем пользователя с уникальным email
        User uniqueUser = new User();
        uniqueUser.setEmail("getbyid@mail.com");
        uniqueUser.setLogin("getbyidlogin");
        uniqueUser.setBirthday(LocalDate.of(1990, 1, 1));

        String userJson = mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(uniqueUser)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        User createdUser = objectMapper.readValue(userJson, User.class);

        // Получаем пользователя по ID
        mockMvc.perform(get("/users/{id}", createdUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdUser.getId()))
                .andExpect(jsonPath("$.email").value("getbyid@mail.com"))
                .andExpect(jsonPath("$.login").value("getbyidlogin"));
    }

    @Test
    void getUserByIdNotFound() throws Exception {
        mockMvc.perform(get("/users/{id}", 9999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Объект не найден"));
    }

    @Test
    void getFilmById() throws Exception {
        // Создаем фильм с уникальным именем
        Film uniqueFilm = new Film();
        uniqueFilm.setName("Get Film By ID Test");
        uniqueFilm.setDescription("Test Description");
        uniqueFilm.setReleaseDate(LocalDate.of(2000, 1, 1));
        uniqueFilm.setDuration(120);

        String filmJson = mockMvc.perform(post("/films")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(uniqueFilm)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Film createdFilm = objectMapper.readValue(filmJson, Film.class);

        // Получаем фильм по ID
        mockMvc.perform(get("/films/{id}", createdFilm.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdFilm.getId()))
                .andExpect(jsonPath("$.name").value("Get Film By ID Test"))
                .andExpect(jsonPath("$.description").value("Test Description"));
    }

    @Test
    void getFilmByIdNotFound() throws Exception {
        mockMvc.perform(get("/films/{id}", 9999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Объект не найден"));
    }
}
