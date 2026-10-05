package service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.maxon.dao.UserDao;
import ru.maxon.entity.User;
import ru.maxon.service.UserService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserDao userDao;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userDao);
    }

    @Test
    void create_validData_delegatesToDao() {
        User user = new User("Maxim", "maxim@example.com", 23);
        when(userDao.create(any(User.class))).thenReturn(user);

        User result = userService.create("Maxim", "maxim@example.com", 23);

        assertSame(user, result);
        verify(userDao).create(argThat(u ->
                u.getName().equals("Maxim") &&
                        u.getEmail().equals("maxim@example.com") &&
                        u.getAge() == 23));
    }

    @Test
    void create_blankName_throwsAndDoesNotCallDao() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.create(" ", "maxim@example.com", 23));
        verifyNoInteractions(userDao);
    }

    @Test
    void create_invalidEmail_throwsAndDoesNotCallDao() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.create("Maxim", "not-an-email", 23));
        verifyNoInteractions(userDao);
    }

    @Test
    void create_ageOutOfRange_throwsAndDoesNotCallDao() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.create("Maxim", "maxim@example.com", 151));
        verifyNoInteractions(userDao);
    }

    @Test
    void findById_positiveId_delegatesToDao() {
        User user = new User("Maxim", "maxim@example.com", 23);
        when(userDao.findById(1L)).thenReturn(user);

        assertSame(user, userService.findById(1L));
        verify(userDao).findById(1L);
    }

    @Test
    void findById_nonPositiveId_throwsAndDoesNotCallDao() {
        assertThrows(IllegalArgumentException.class, () -> userService.findById(0));
        verifyNoInteractions(userDao);
    }

    @Test
    void findAll_delegatesToDao() {
        List<User> users = List.of(new User("Maxim", "maxim@example.com", 23));
        when(userDao.findAll()).thenReturn(users);

        assertEquals(users, userService.findAll());
        verify(userDao).findAll();
    }

    @Test
    void update_validData_delegatesToDao() {
        when(userDao.update(1L, "Maxim", "maxim@example.com", 23)).thenReturn(true);

        assertTrue(userService.update(1L, "Maxim", "maxim@example.com", 23));
        verify(userDao).update(1L, "Maxim", "maxim@example.com", 23);
    }

    @Test
    void delete_positiveId_delegatesToDao() {
        when(userDao.delete(1L)).thenReturn(true);

        assertTrue(userService.delete(1L));
        verify(userDao).delete(1L);
    }
}

