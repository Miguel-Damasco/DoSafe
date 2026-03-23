CREATE TABLE email_verification_tokens (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    token      VARCHAR(255) NOT NULL,
    user_id    BIGINT       NOT NULL,
    expires_at DATETIME(6),
    used_at    DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_email_verification_token      UNIQUE (token),
    CONSTRAINT fk_email_verification_token_user FOREIGN KEY (user_id) REFERENCES users (id)
);
