package ru.yandex.practicum.filmorate.filmTests;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@Import({FilmDbStorage.class, UserDbStorage.class})
@Sql(scripts = {"/schema.sql", "/data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class FilmDbStorageTest {

    @Autowired
    private FilmDbStorage filmStorage;

    @Autowired
    private UserDbStorage userStorage;

    private Mpa createMpa(int id, String name) {
        Mpa mpa = new Mpa();
        mpa.setId(id);
        mpa.setName(name);
        return mpa;
    }

    private Genre createGenre(int id, String name) {
        Genre genre = new Genre();
        genre.setId(id);
        genre.setName(name);
        return genre;
    }

    @Test
    void testCreateFilm() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("Test Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        film.setMpa(createMpa(3, "PG-13"));

        Set<Genre> genres = new HashSet<>();
        genres.add(createGenre(1, "Комедия"));
        genres.add(createGenre(6, "Боевик"));
        film.setGenres(genres);

        Film created = filmStorage.createFilm(film);

        assertThat(created.getId()).isNotNull();

        Optional<Film> found = filmStorage.getFilmById(created.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Film");
        assertThat(found.get().getMpa().getId()).isEqualTo(3);
        assertThat(found.get().getMpa().getName()).isEqualTo("PG-13");
        assertThat(found.get().getGenres()).hasSize(2);
    }

    @Test
    void testAddLike() {
        User user = new User();
        user.setEmail("user@test.com");
        user.setLogin("testuser");
        user.setName("Test User");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        User createdUser = userStorage.createUser(user);

        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("Test Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        film.setMpa(createMpa(3, "PG-13"));
        Film createdFilm = filmStorage.createFilm(film);

        filmStorage.addLike(createdFilm.getId(), createdUser.getId());

        Set<Long> likes = filmStorage.getLikesForFilm(createdFilm.getId());
        assertThat(likes).contains(createdUser.getId());
    }

    @Test
    void testRemoveLike() {
        User user = new User();
        user.setEmail("user@test.com");
        user.setLogin("testuser");
        user.setName("Test User");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        User createdUser = userStorage.createUser(user);

        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("Test Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        film.setMpa(createMpa(3, "PG-13"));
        Film createdFilm = filmStorage.createFilm(film);

        filmStorage.addLike(createdFilm.getId(), createdUser.getId());
        filmStorage.removeLike(createdFilm.getId(), createdUser.getId());

        Set<Long> likes = filmStorage.getLikesForFilm(createdFilm.getId());
        assertThat(likes).doesNotContain(createdUser.getId());
    }

    @Test
    void testUpdateFilm() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("Test Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        film.setMpa(createMpa(3, "PG-13"));

        Film created = filmStorage.createFilm(film);

        created.setName("Updated Film");
        created.setMpa(createMpa(4, "R"));

        Set<Genre> genres = new HashSet<>();
        genres.add(createGenre(2, "Драма"));
        created.setGenres(genres);

        filmStorage.updateFilm(created);

        Optional<Film> found = filmStorage.getFilmById(created.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Updated Film");
        assertThat(found.get().getMpa().getId()).isEqualTo(4);
        assertThat(found.get().getMpa().getName()).isEqualTo("R");
        assertThat(found.get().getGenres()).hasSize(1);
    }

    @Test
    void testDeleteFilm() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("Test Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        film.setMpa(createMpa(3, "PG-13"));

        Film created = filmStorage.createFilm(film);

        boolean deleted = filmStorage.deleteFilm(created.getId());
        assertThat(deleted).isTrue();

        Optional<Film> found = filmStorage.getFilmById(created.getId());
        assertThat(found).isEmpty();
    }
}
