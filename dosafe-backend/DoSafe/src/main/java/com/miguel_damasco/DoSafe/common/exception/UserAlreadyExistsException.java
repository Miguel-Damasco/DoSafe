package com.miguel_damasco.DoSafe.common.exception;

import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends DoSafeException {

    public UserAlreadyExistsException(String username) {
        super("Username '" + username + "' is already taken", HttpStatus.CONFLICT, "USER_ALREADY_EXISTS");
    }
}
