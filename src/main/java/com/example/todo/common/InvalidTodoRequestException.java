package com.example.todo.common;

public class InvalidTodoRequestException extends RuntimeException {
    public InvalidTodoRequestException(String message) {
        super(message);
    }
}
