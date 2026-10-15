package ru.maxon.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(long id) {
        super("Пользователь с ID " + id + " не найден");
    }
}