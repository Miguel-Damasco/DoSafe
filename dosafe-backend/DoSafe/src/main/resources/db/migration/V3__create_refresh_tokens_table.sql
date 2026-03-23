CREATE TABLE refresh_token (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    token      VARCHAR(255) NOT NULL,
    user_id    BIGINT       NOT NULL,
    expires_at DATETIME(6),
    revoked    TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uq_refresh_token        UNIQUE (token),
    CONSTRAINT fk_refresh_token_user   FOREIGN KEY (user_id) REFERENCES users (id)
);
