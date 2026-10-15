package ru.maxon.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserRequest(

        @NotBlank(message = "Имя не должно быть пустым")
        @Size(max = 100, message = "Имя не должно быть длиннее 100 символов")
        String name,

        @NotBlank(message = "Email не должен быть пустым")
        @Email(message = "Укажите корректный email")
        @Size(max = 255, message = "Email не должен быть длиннее 255 символов")
        String email,

        @NotNull(message = "Возраст обязателен")
        @Min(value = 0, message = "Возраст не может быть отрицательным")
        @Max(value = 150, message = "Возраст не должен превышать 150")
        Integer age

) {
}