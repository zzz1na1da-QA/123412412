package ru.maxon.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.maxon.entity.User;
import ru.maxon.exception.EmailAlreadyExistsException;
import ru.maxon.exception.UserNotFoundException;
import ru.maxon.repository.UserRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User create(String name, String email, int age) {
        validate(name, email, age);

        String normalizedName = name.trim();
        String normalizedEmail = email.trim();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        User user = new User(normalizedName, normalizedEmail, age);

        return userRepository.save(user);
    }

    public User findById(long id) {
        validateId(id);

        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    public List<User> findAll() {
        return userRepository.findAll(Sort.by("id"));
    }

    @Transactional
    public User update(long id, String name, String email, int age) {
        validateId(id);
        validate(name, email, age);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        String normalizedName = name.trim();
        String normalizedEmail = email.trim();

        if (userRepository.existsByEmailAndIdNot(normalizedEmail, id)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        user.setName(normalizedName);
        user.setEmail(normalizedEmail);
        user.setAge(age);

        return userRepository.save(user);
    }

    @Transactional
    public void delete(long id) {
        validateId(id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        userRepository.delete(user);
    }

    private void validate(String name, String email, int age) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Имя не должно быть пустым"
            );
        }

        if (name.trim().length() > 100) {
            throw new IllegalArgumentException(
                    "Имя не должно быть длиннее 100 символов"
            );
        }

        if (email == null
                || email.trim().length() > 255
                || !email.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException(
                    "Укажите корректный email"
            );
        }

        if (age < 0 || age > 150) {
            throw new IllegalArgumentException(
                    "Возраст должен быть от 0 до 150"
            );
        }
    }

    private void validateId(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "ID должен быть положительным"
            );
        }
    }
}