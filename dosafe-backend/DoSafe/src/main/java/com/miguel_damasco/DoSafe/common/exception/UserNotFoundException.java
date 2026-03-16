package com.miguel_damasco.DoSafe.common.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends DoSafeException {

    public UserNotFoundException(String username) {
        super("User '" + username + "' not found", HttpStatus.NOT_FOUND, "USER_NOT_FOUND");
    }
}
