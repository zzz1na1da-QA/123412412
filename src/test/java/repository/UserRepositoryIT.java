package repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import ru.maxon.entity.User;
import ru.maxon.repository.UserRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.sql.init.mode=never"
})
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@ContextConfiguration(
        classes = UserRepositoryIT.JpaTestConfiguration.class
)
@Testcontainers
class UserRepositoryIT {

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18-alpine")
                    .withDatabaseName("user_service_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.datasource.url",
                POSTGRES::getJdbcUrl
        );
        registry.add(
                "spring.datasource.username",
                POSTGRES::getUsername
        );
        registry.add(
                "spring.datasource.password",
                POSTGRES::getPassword
        );
    }

    @Configuration(proxyBeanMethods = false)
    @EntityScan(basePackageClasses = User.class)
    @EnableJpaRepositories(basePackageClasses = UserRepository.class)
    static class JpaTestConfiguration {
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void saveShouldPersistUserAndGenerateIdAndCreatedAt() {
        User saved = userRepository.saveAndFlush(
                new User("Maxim", "maxim@example.com", 23)
        );

        Long id = saved.getId();

        assertNotNull(id);
        assertNotNull(saved.getCreatedAt());

        entityManager.clear();

        User loaded = userRepository.findById(id).orElseThrow();

        assertAll(
                () -> assertEquals(id, loaded.getId()),
                () -> assertEquals("Maxim", loaded.getName()),
                () -> assertEquals(
                        "maxim@example.com",
                        loaded.getEmail()
                ),
                () -> assertEquals(23, loaded.getAge()),
                () -> assertNotNull(loaded.getCreatedAt())
        );
    }

    @Test
    void findByIdShouldReturnEmptyForMissingUser() {
        assertTrue(userRepository.findById(-1L).isEmpty());
    }

    @Test
    void findAllShouldReturnUsersSortedById() {
        User first = userRepository.saveAndFlush(
                new User("First", "first@example.com", 20)
        );
        User second = userRepository.saveAndFlush(
                new User("Second", "second@example.com", 30)
        );

        Long firstId = first.getId();
        Long secondId = second.getId();

        entityManager.clear();

        List<User> users = userRepository.findAll(Sort.by("id"));

        assertEquals(2, users.size());

        assertAll(
                () -> assertEquals(firstId, users.get(0).getId()),
                () -> assertEquals("First", users.get(0).getName()),
                () -> assertEquals(secondId, users.get(1).getId()),
                () -> assertEquals("Second", users.get(1).getName())
        );
    }

    @Test
    void updateShouldPersistChangesAndKeepCreatedAt() {
        User saved = userRepository.saveAndFlush(
                new User("Before", "before@example.com", 20)
        );

        Long id = saved.getId();

        entityManager.clear();

        User user = userRepository.findById(id).orElseThrow();
        long originalCreatedAt = user.getCreatedAt().getTime();

        user.setName("After");
        user.setEmail("after@example.com");
        user.setAge(21);

        userRepository.saveAndFlush(user);
        entityManager.clear();

        User updated = userRepository.findById(id).orElseThrow();

        assertAll(
                () -> assertEquals("After", updated.getName()),
                () -> assertEquals(
                        "after@example.com",
                        updated.getEmail()
                ),
                () -> assertEquals(21, updated.getAge()),
                () -> assertEquals(
                        originalCreatedAt,
                        updated.getCreatedAt().getTime()
                )
        );
    }

    @Test
    void deleteShouldRemoveUser() {
        User saved = userRepository.saveAndFlush(
                new User("Delete", "delete@example.com", 22)
        );

        Long id = saved.getId();

        userRepository.delete(saved);
        userRepository.flush();
        entityManager.clear();

        assertTrue(userRepository.findById(id).isEmpty());
    }

    @Test
    void existsByEmailShouldDetectExistingEmail() {
        userRepository.saveAndFlush(
                new User("Maxim", "maxim@example.com", 23)
        );

        entityManager.clear();

        assertAll(
                () -> assertTrue(
                        userRepository.existsByEmail("maxim@example.com")
                ),
                () -> assertFalse(
                        userRepository.existsByEmail("missing@example.com")
                )
        );
    }

    @Test
    void existsByEmailAndIdNotShouldExcludeCurrentUser() {
        User first = userRepository.saveAndFlush(
                new User("First", "first@example.com", 20)
        );
        User second = userRepository.saveAndFlush(
                new User("Second", "second@example.com", 30)
        );

        Long firstId = first.getId();
        Long secondId = second.getId();

        entityManager.clear();

        assertAll(
                () -> assertFalse(
                        userRepository.existsByEmailAndIdNot(
                                "first@example.com",
                                firstId
                        )
                ),
                () -> assertTrue(
                        userRepository.existsByEmailAndIdNot(
                                "first@example.com",
                                secondId
                        )
                ),
                () -> assertFalse(
                        userRepository.existsByEmailAndIdNot(
                                "missing@example.com",
                                firstId
                        )
                )
        );
    }

    @Test
    void saveShouldRejectDuplicateEmail() {
        userRepository.saveAndFlush(
                new User("First", "same@example.com", 20)
        );

        entityManager.clear();

        assertThrows(
                DataIntegrityViolationException.class,
                () -> userRepository.saveAndFlush(
                        new User("Second", "same@example.com", 30)
                )
        );
    }
}