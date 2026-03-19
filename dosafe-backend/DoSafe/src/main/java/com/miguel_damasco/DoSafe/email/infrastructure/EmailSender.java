package com.miguel_damasco.DoSafe.email.infrastructure;

public interface EmailSender {

    // Generic email abstraction — knows nothing about alerts, users, or documents.
    // Any module (alert, user verification, etc.) can depend on this without cross-module coupling.
    void send(String pTo, String pSubject, String pBody);
}
