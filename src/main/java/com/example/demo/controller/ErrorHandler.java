package com.example.demo.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.ResponseEntity.badRequest;
import static org.springframework.http.ResponseEntity.status;

@ControllerAdvice
public class ErrorHandler {

    // Ошибка использования неправильных аргументов
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException e) {
        return badRequest().body(e.getMessage());
    }

    // Ошибка, если процесс удаления уже запущен для одной из таблиц
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleIllegalState(IllegalStateException e) {
        return status(CONFLICT)
                .body(e.getMessage());
    }

    // Обработка неизвестных ошибок
    @ExceptionHandler
    public ResponseEntity<?> unknownErrorHandler(Throwable t) {
        return status(INTERNAL_SERVER_ERROR)
                .body(t.getMessage());
    }

}
