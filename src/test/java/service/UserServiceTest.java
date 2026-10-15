package service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import ru.maxon.entity.User;
import ru.maxon.exception.EmailAlreadyExistsException;
import ru.maxon.exception.UserNotFoundException;
import ru.maxon.repository.UserRepository;
import ru.maxon.service.UserService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    @Test
    void createShouldSaveUser() {
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.create(
                " Maxim ",
                " maxim@example.com ",
                23
        );

        assertEquals("Maxim", result.getName());
        assertEquals("maxim@example.com", result.getEmail());
        assertEquals(23, result.getAge());

        verify(userRepository).existsByEmail("maxim@example.com");
        verify(userRepository).save(result);
    }

    @Test
    void createShouldRejectDuplicateEmail() {
        when(userRepository.existsByEmail("maxim@example.com"))
                .thenReturn(true);

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> userService.create("Maxim", "maxim@example.com", 23)
        );

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createShouldRejectInvalidData() {
        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> userService.create("", "maxim@example.com", 23)
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> userService.create("Maxim", "invalid-email", 23)
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> userService.create("Maxim", "maxim@example.com", -1)
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> userService.create("Maxim", "maxim@example.com", 151)
                )
        );

        verifyNoInteractions(userRepository);
    }

    @Test
    void findByIdShouldReturnUser() {
        User user = new User("Maxim", "maxim@example.com", 23);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        assertSame(user, userService.findById(1L));
    }

    @Test
    void findByIdShouldRejectMissingUser() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> userService.findById(1L)
        );
    }

    @Test
    void findByIdShouldRejectInvalidId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.findById(0)
        );

        verifyNoInteractions(userRepository);
    }

    @Test
    void findAllShouldReturnSortedUsers() {
        List<User> users = List.of(
                new User("Maxim", "maxim@example.com", 23),
                new User("Anna", "anna@example.com", 25)
        );

        when(userRepository.findAll(Sort.by("id")))
                .thenReturn(users);

        assertEquals(users, userService.findAll());
    }

    @Test
    void updateShouldSaveChanges() {
        User user = new User("Before", "before@example.com", 20);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));
        when(userRepository.save(user))
                .thenReturn(user);

        User result = userService.update(
                1L,
                " After ",
                " after@example.com ",
                21
        );

        assertSame(user, result);
        assertEquals("After", result.getName());
        assertEquals("after@example.com", result.getEmail());
        assertEquals(21, result.getAge());

        verify(userRepository).existsByEmailAndIdNot(
                "after@example.com",
                1L
        );
        verify(userRepository).save(user);
    }

    @Test
    void updateShouldRejectMissingUser() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> userService.update(
                        1L,
                        "Maxim",
                        "maxim@example.com",
                        23
                )
        );

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateShouldRejectDuplicateEmail() {
        User user = new User("Before", "before@example.com", 20);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndIdNot("taken@example.com", 1L))
                .thenReturn(true);

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> userService.update(
                        1L,
                        "After",
                        "taken@example.com",
                        21
                )
        );

        assertEquals("Before", user.getName());
        assertEquals("before@example.com", user.getEmail());

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deleteShouldRemoveUser() {
        User user = new User("Maxim", "maxim@example.com", 23);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        userService.delete(1L);

        verify(userRepository).delete(user);
    }

    @Test
    void deleteShouldRejectMissingUser() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> userService.delete(1L)
        );

        verify(userRepository, never()).delete(any(User.class));
    }
}