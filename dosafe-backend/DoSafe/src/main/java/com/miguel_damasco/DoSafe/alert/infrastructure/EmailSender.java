package com.miguel_damasco.DoSafe.alert.infrastructure;

import com.miguel_damasco.DoSafe.alert.domain.AlertModel;

public interface EmailSender {

    // Sends an expiration alert email to the user associated with the given alert.
    void send(AlertModel pAlert);
}
