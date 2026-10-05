package dao;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import ru.maxon.dao.UserDao;
import ru.maxon.entity.User;
import ru.maxon.util.HibernateUtil;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class UserDaoIT {
    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18-alpine")
                    .withDatabaseName("user_service_test")
                    .withUsername("test")
                    .withPassword("test");

    private UserDao userDao;

    @BeforeEach
    void setUp() {
        System.setProperty("test.db.url", POSTGRES.getJdbcUrl());
        System.setProperty("test.db.username", POSTGRES.getUsername());
        System.setProperty("test.db.password", POSTGRES.getPassword());
        System.setProperty("test.db.schema", "create-drop");

        SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
        userDao = new UserDao(sessionFactory);
    }

    @AfterEach
    void tearDown() {
        HibernateUtil.shutdown();
        System.clearProperty("test.db.url");
        System.clearProperty("test.db.username");
        System.clearProperty("test.db.password");
        System.clearProperty("test.db.schema");
    }

    @Test
    void create_persistsUserAndSetsGeneratedIdAndCreatedAt() {
        User saved = userDao.create(new User("Maxim", "maxim@example.com", 23));

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        User loaded = userDao.findById(saved.getId());
        assertNotNull(loaded);
        assertEquals("Maxim", loaded.getName());
        assertEquals("maxim@example.com", loaded.getEmail());
        assertEquals(23, loaded.getAge());
    }

    @Test
    void findById_missingId_returnsNull() {
        assertNull(userDao.findById(9999L));
    }

    @Test
    void findAll_returnsOnlyPersistedUsers() {
        userDao.create(new User("First", "first@example.com", 20));
        userDao.create(new User("Second", "second@example.com", 30));

        List<User> users = userDao.findAll();

        assertEquals(2, users.size());
        assertEquals("First", users.get(0).getName());
        assertEquals("Second", users.get(1).getName());
    }

    @Test
    void update_existingUser_updatesMutableFieldsAndKeepsCreatedAt() {
        User saved = userDao.create(new User("Before", "before@example.com", 20));
        var originalCreatedAt = saved.getCreatedAt();

        assertTrue(userDao.update(saved.getId(), "After", "after@example.com", 21));

        User updated = userDao.findById(saved.getId());
        assertEquals("After", updated.getName());
        assertEquals("after@example.com", updated.getEmail());
        assertEquals(21, updated.getAge());
        assertEquals(originalCreatedAt, updated.getCreatedAt());
    }

    @Test
    void update_missingUser_returnsFalse() {
        assertFalse(userDao.update(9999L, "Nobody", "nobody@example.com", 40));
    }

    @Test
    void delete_existingUser_removesIt() {
        User saved = userDao.create(new User("Delete", "delete@example.com", 22));

        assertTrue(userDao.delete(saved.getId()));
        assertNull(userDao.findById(saved.getId()));
    }

    @Test
    void delete_missingUser_returnsFalse() {
        assertFalse(userDao.delete(9999L));
    }
}

