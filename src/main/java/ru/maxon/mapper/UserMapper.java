package ru.maxon.mapper;

import org.springframework.stereotype.Component;
import ru.maxon.dto.UserResponse;
import ru.maxon.entity.User;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getAge(),
                user.getCreatedAt().toInstant()
        );
    }
}