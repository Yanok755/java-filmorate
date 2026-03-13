package ru.yandex.practicum.filmorate.userTests;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@Import(UserDbStorage.class)
@Sql(scripts = {"/schema.sql", "/data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class UserDbStorageTest {

    @Autowired
    private UserDbStorage userStorage;

    @Test
    void testCreateUser() {
        User user = new User();
        user.setEmail("test@mail.ru");
        user.setLogin("testLogin");
        user.setName("Test Name");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        User created = userStorage.createUser(user);

        assertThat(created.getId()).isNotNull();

        Optional<User> found = userStorage.getUserById(created.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@mail.ru");
    }

    @Test
    void testUpdateUser() {
        User user = new User();
        user.setEmail("test@mail.ru");
        user.setLogin("testLogin");
        user.setName("Test Name");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        User created = userStorage.createUser(user);

        created.setName("Updated Name");
        created.setEmail("updated@mail.ru");

        userStorage.updateUser(created);

        Optional<User> found = userStorage.getUserById(created.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Updated Name");
        assertThat(found.get().getEmail()).isEqualTo("updated@mail.ru");
    }

    @Test
    void testDeleteUser() {
        User user = new User();
        user.setEmail("test@mail.ru");
        user.setLogin("testLogin");
        user.setName("Test Name");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        User created = userStorage.createUser(user);

        boolean deleted = userStorage.deleteUser(created.getId());
        assertThat(deleted).isTrue();

        Optional<User> found = userStorage.getUserById(created.getId());
        assertThat(found).isEmpty();
    }

    @Test
    void testAddFriend() {
        User user1 = new User();
        user1.setEmail("user1@mail.ru");
        user1.setLogin("user1");
        user1.setName("User 1");
        user1.setBirthday(LocalDate.of(1990, 1, 1));

        User user2 = new User();
        user2.setEmail("user2@mail.ru");
        user2.setLogin("user2");
        user2.setName("User 2");
        user2.setBirthday(LocalDate.of(1995, 1, 1));

        User created1 = userStorage.createUser(user1);
        User created2 = userStorage.createUser(user2);

        userStorage.addFriend(created1.getId(), created2.getId());

        Collection<Long> friends = userStorage.getFriendIds(created1.getId());
        assertThat(friends).contains(created2.getId());

        Collection<Long> friends2 = userStorage.getFriendIds(created2.getId());
        assertThat(friends2).doesNotContain(created1.getId());
    }

    @Test
    void testRemoveFriend() {
        User user1 = new User();
        user1.setEmail("user1@mail.ru");
        user1.setLogin("user1");
        user1.setName("User 1");
        user1.setBirthday(LocalDate.of(1990, 1, 1));

        User user2 = new User();
        user2.setEmail("user2@mail.ru");
        user2.setLogin("user2");
        user2.setName("User 2");
        user2.setBirthday(LocalDate.of(1995, 1, 1));

        User created1 = userStorage.createUser(user1);
        User created2 = userStorage.createUser(user2);

        userStorage.addFriend(created1.getId(), created2.getId());
        userStorage.removeFriend(created1.getId(), created2.getId());

        Collection<Long> friends = userStorage.getFriendIds(created1.getId());
        assertThat(friends).doesNotContain(created2.getId());
    }
}
