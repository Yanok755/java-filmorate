package ru.yandex.practicum.filmorate;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FilmorateApplicationTests {
	private static final String BASE_URL = "http://localhost:8080";
	private final TestRestTemplate restTemplate = new TestRestTemplate();

	@Test
	void getEmptyListOfUsers() {
		log.info("Тест GET запроса пользователей с пустого сервера");
		ResponseEntity<User[]> response = restTemplate.getForEntity(BASE_URL + "/users", User[].class);
		assertEquals(200, response.getStatusCode().value());

		User[] users = response.getBody();
		assertNotNull(users);
        assertEquals(0, users.length);

		log.info("Тест прошел успешно");
	}

	@Test
	void getEmptyListOfFilms() {
		log.info("Тест GET запроса фильмов с пустого сервера");
		ResponseEntity<Film[]> response = restTemplate.getForEntity(BASE_URL + "/films", Film[].class);
		assertEquals(200, response.getStatusCode().value());

		Film[] films = response.getBody();
		assertNotNull(films);
		assertEquals(0, films.length);

		log.info("Тест прошел успешно");
	}

	@Test
	void getNotEmptyListOfUsers() {
		log.info("Тест GET запроса пользователей из непустого сервера");
		User someUser = new User(1L, "1@aaa.com", "abc", "", LocalDate.of(2000, 1, 1));
		restTemplate.postForEntity(BASE_URL + "/users", someUser, User.class);

		ResponseEntity<User[]> response = restTemplate.getForEntity(BASE_URL + "/users", User[].class);
		assertEquals(200, response.getStatusCode().value());

		User[] users = response.getBody();

		assertNotNull(users);
		assertEquals(1, users.length);
		assertEquals("1@aaa.com", users[0].getEmail());

		log.info("Тест прошел успешно");
	}

	@Test
	void getNotEmptyListOfFilms() {
		log.info("Тест GET запроса фильмов из непустого сервера");
		Film someFilm1 = new Film(1L, "Аватар", "Самый кассовый фильм", LocalDate.of(2009, 1, 1), 150);
		Film someFilm2 = new Film(2L, "Гладиатор", null, LocalDate.of(2000, 2, 1), 130);
		restTemplate.postForEntity(BASE_URL + "/films", someFilm1, Film.class);
		restTemplate.postForEntity(BASE_URL + "/films", someFilm2, Film.class);

		ResponseEntity<Film[]> response = restTemplate.getForEntity(BASE_URL + "/films", Film[].class);
		assertEquals(200, response.getStatusCode().value());

		Film[] films = response.getBody();

		assertNotNull(films);
		assertEquals(2, films.length);
		assertEquals(150, films[0].getDuration());
		assertEquals("Гладиатор", films[1].getName());

		log.info("Тест прошел успешно");
	}

	@Test
	void postFilmWithoutName() {
		log.info("Тест POST запроса фильма без названия");
		Film film = new Film(1L,"",null, LocalDate.of(1895, 12, 28), 150);

		ResponseEntity<Film> response = restTemplate.postForEntity(BASE_URL + "/films", film, Film.class);
		assertEquals(400, response.getStatusCode().value(), "Должна вернуться ошибка 400");

		log.info("Тест прошел успешно");
	}

	@Test
	void postUserWithoutLogin() {
		log.info("Тест POST запроса пользователя без логина");
		User user = new User(1L, "1@aaa.com", "", "ab", LocalDate.of(2000, 1, 1));

		ResponseEntity<User> response = restTemplate.postForEntity(BASE_URL + "/users", user, User.class);
		assertEquals(400, response.getStatusCode().value(), "Должна вернуться ошибка 400");

		log.info("Тест прошел успешно");
	}

	@Test
	void postFilmWithBadReleaseDate() {
		log.info("Тест POST запроса фильма с некорректной датой релиза");
		Film film = new Film(1L, "Аватар", "", LocalDate.of(1895, 2, 27), 150);

		ResponseEntity<Film> response = restTemplate.postForEntity(BASE_URL + "/films", film, Film.class);
		assertEquals(400, response.getStatusCode().value(), "Должна вернуться ошибка 400");

		log.info("Тест прошел успешно");
	}

	@Test
	void postUserWithBadEmail() {
		log.info("Тест POST запроса пользователя с некорректной почтой");
		User user = new User(1L, "1aaa.com", "XY", "ab", LocalDate.of(2000, 1, 1));

		ResponseEntity<User> response = restTemplate.postForEntity(BASE_URL + "/users", user, User.class);
		assertEquals(400, response.getStatusCode().value(), "Должна вернуться ошибка 400");

		log.info("Тест прошел успешно");
	}

	@Test
	void postNullRequestUser() {
		log.info("Тест POST запроса фильма с отсутствующим телом");
		ResponseEntity<User> response = restTemplate.postForEntity(BASE_URL + "/users", null, User.class);
		assertEquals(415, response.getStatusCode().value(), "Должна вернуться ошибка 415");

		log.info("Тест прошел успешно");
	}

	@Test
	void postNullRequestFilm() {
		log.info("Тест POST запроса пользователя с отсутствующим телом");
		ResponseEntity<Film> response = restTemplate.postForEntity(BASE_URL + "/films", null, Film.class);
		assertEquals(415, response.getStatusCode().value(), "Должна вернуться ошибка 415");

		log.info("Тест прошел успешно");
	}

	@Test
	void postFilmWithoutId() {
		log.info("Тест POST запроса фильма без id");
		Film film = new Film(null, "Аватар", "", LocalDate.of(2009, 1, 1), 150);

		ResponseEntity<Film> response = restTemplate.postForEntity(BASE_URL + "/films", film, Film.class);
		assertEquals(200, response.getStatusCode().value());

		Film postedFilm = response.getBody();
		assertNotNull(postedFilm);
		assertEquals(1L, postedFilm.getId());

		log.info("Тест прошел успешно");
	}

	@Test
	void postSomeUsersWithoutId() {
		log.info("Тест POST запроса пользователей без id");
		User user1 = new User(null, "1aaa@bbb.com", "XYZ", "abc", LocalDate.of(2000, 1, 1));
		User user2 = new User(null, "2aaa@bbb.com", "XY", "ab", LocalDate.of(2000, 1, 1));

		ResponseEntity<User> postResponse1 = restTemplate.postForEntity(BASE_URL + "/users", user1, User.class);
		ResponseEntity<User> postResponse2 = restTemplate.postForEntity(BASE_URL + "/users", user2, User.class);
		assertEquals(200, postResponse1.getStatusCode().value());
		assertEquals(200, postResponse2.getStatusCode().value());

		ResponseEntity<User[]> getResponse = restTemplate.getForEntity(BASE_URL + "/users", User[].class);
		assertEquals(200, getResponse.getStatusCode().value());

		User[] users = getResponse.getBody();

		assertNotNull(users);
		assertEquals(2, users.length);
		assertEquals(2L, users[1].getId());

		log.info("Тест прошел успешно");
	}

	@Test
	void putFilmWithoutId() {
		log.info("Тест PUT запроса фильма без id");
		Film film = new Film(1L, "Аватар", "", LocalDate.of(2009, 1, 1), 150);
		restTemplate.postForEntity(BASE_URL + "/films", film, Film.class);

		Film newFilm = new Film(2L, "Аватар2", "", LocalDate.of(2009, 1, 1), 150);
		ResponseEntity<Film> response = restTemplate.exchange(BASE_URL + "/films", HttpMethod.PUT, new HttpEntity<>(newFilm), Film.class);

		assertEquals(404, response.getStatusCode().value(), "Должна вернуться ошибка 404");

		log.info("Тест прошел успешно");
	}

	@Test
	void putUserWithoutId() {
		log.info("Тест PUT запроса пользователя без id");
		User user = new User(null, "1aaa@bbb.com", "XYZ", "abc", LocalDate.of(2000, 1, 1));
		restTemplate.postForEntity(BASE_URL + "/users", user, User.class);

		User newUser = new User(10L, "2aaa@bbb.com", "ZYX", "abc", LocalDate.of(2000, 1, 1));
		ResponseEntity<User> response = restTemplate.exchange(BASE_URL + "/users", HttpMethod.PUT, new HttpEntity<>(newUser), User.class);

		assertEquals(404, response.getStatusCode().value(), "Должна вернуться ошибка 404");

		log.info("Тест прошел успешно");
	}

	@Test
	void putNewDurationToFilm() {
		log.info("Тест PUT запроса фильма с новой длительностью");
		Film film = new Film(1L, "Аватар", "", LocalDate.of(2009, 1, 1), 150);
		restTemplate.postForEntity(BASE_URL + "/films", film, Film.class);

		Film newFilm = new Film(1L, "Аватар", "", LocalDate.of(2009, 1, 1), 200);
		ResponseEntity<Film> response = restTemplate.exchange(BASE_URL + "/films", HttpMethod.PUT, new HttpEntity<>(newFilm), Film.class);

		assertEquals(200, response.getStatusCode().value());

		Film updatedFilm = response.getBody();
		assertNotNull(updatedFilm);
		assertEquals(200, updatedFilm.getDuration());

		log.info("Тест прошел успешно");
	}

	@Test
	void putNewEmailAndNameToUser() {
		log.info("Тест PUT запроса пользователя с новой почтой и именем");
		User user = new User(1L, "1aaa@bbb.com", "XYZ", "abc", LocalDate.of(2000, 1, 1));
		restTemplate.postForEntity(BASE_URL + "/users", user, User.class);

		User newUser = new User(1L, "2aaa@bbb.com", "XYZ", "bca", LocalDate.of(2000, 1, 1));
		ResponseEntity<User> response = restTemplate.exchange(BASE_URL + "/users", HttpMethod.PUT, new HttpEntity<>(newUser), User.class);

		assertEquals(200, response.getStatusCode().value());

		User updatedUser = response.getBody();
		assertNotNull(updatedUser);
		assertEquals("2aaa@bbb.com", updatedUser.getEmail());
		assertEquals("bca", updatedUser.getName());

		log.info("Тест прошел успешно");
	}
}
