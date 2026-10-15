package ru.maxon.dto;

import java.time.Instant;

public record UserResponse(
        Long id,
        String name,
        String email,
        Integer age,
        Instant createdAt
) {
}