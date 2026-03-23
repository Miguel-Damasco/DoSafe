CREATE TABLE alerts (
    id          VARCHAR(36)  NOT NULL,
    id_user     BIGINT       NOT NULL,
    id_document BINARY(16)   NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    sent_at     DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_alerts_user     FOREIGN KEY (id_user)     REFERENCES users (id),
    CONSTRAINT fk_alerts_document FOREIGN KEY (id_document) REFERENCES documents (id)
);
