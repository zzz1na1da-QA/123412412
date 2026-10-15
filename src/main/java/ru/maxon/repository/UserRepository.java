package ru.maxon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.maxon.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);
}