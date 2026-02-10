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

    @BeforeEach
    void setUp() {
        validFilm = new Film();
        validFilm.setName("Test Film");
        validFilm.setDescription("Test Description");
        validFilm.setReleaseDate(LocalDate.of(2000, 1, 1));
        validFilm.setDuration(120);

        validUser = new User();
        validUser.setEmail("test@mail.com");
        validUser.setLogin("testlogin");
        validUser.setBirthday(LocalDate.of(1990, 1, 1));
    }

    @Test
    void createValidFilm() throws Exception {
        mockMvc.perform(post("/films")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validFilm)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Film"));
    }

    @Test
    void createFilmWithEmptyName() throws Exception {
        Film film = new Film();
        film.setName("");
        film.setDescription("Test");
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
                .andExpect(status().isBadRequest());
    }

    @Test
    void createValidUser() throws Exception {
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test@mail.com"));
    }

    @Test
    void createUserWithInvalidEmail() throws Exception {
        User user = new User();
        user.setEmail("invalid-email");
        user.setLogin("testlogin");
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
        user.setLogin("test login");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateNonExistentFilm() throws Exception {
        validFilm.setId(999); // Несуществующий ID
        mockMvc.perform(put("/films")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validFilm)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllFilms() throws Exception {
        mockMvc.perform(get("/films"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllUsers() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk());
    }
}
