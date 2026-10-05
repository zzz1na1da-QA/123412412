package ru.maxon.service;

import ru.maxon.dao.UserDao;
import ru.maxon.entity.User;

import java.util.List;

public class UserService {
    private final UserDao userDao;

    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    public User create(String name, String email, int age) {
        validate(name, email, age);
        return userDao.create(new User(name.trim(), email.trim(), age));
    }

    public User findById(long id) {
        validateId(id);
        return userDao.findById(id);
    }

    public List<User> findAll() {
        return userDao.findAll();
    }

    public boolean update(long id, String name, String email, int age) {
        validateId(id);
        validate(name, email, age);
        return userDao.update(id, name.trim(), email.trim(), age);
    }

    public boolean delete(long id) {
        validateId(id);
        return userDao.delete(id);
    }

    private void validate(String name, String email, int age) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Имя не должно быть пустым");
        }
        if (email == null || email.isBlank() || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Укажите корректный email");
        }
        if (age < 0 || age > 150) {
            throw new IllegalArgumentException("Возраст должен быть от 0 до 150");
        }
    }

    private void validateId(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID должен быть положительным");
        }
    }
}
