CREATE TABLE ledger_transactions (
                                     id BIGSERIAL PRIMARY KEY,
                                     reference VARCHAR(100) NOT NULL,
                                     description VARCHAR(255),
                                     status VARCHAR(20) NOT NULL,
                                     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                     CONSTRAINT uk_ledger_tx_reference UNIQUE (reference)
);
