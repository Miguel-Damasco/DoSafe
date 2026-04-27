package com.miguel_damasco.DoSafe.common.exception;

import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends DoSafeException {

    public UserAlreadyExistsException() {
        super("Username or email already in use", HttpStatus.CONFLICT, "USER_ALREADY_EXISTS");
    }
}
