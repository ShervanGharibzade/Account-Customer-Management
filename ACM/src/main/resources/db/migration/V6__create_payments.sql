CREATE TABLE payments (
                          id           BIGSERIAL PRIMARY KEY,
                          reference_id VARCHAR(100) NOT NULL UNIQUE,
                          amount       NUMERIC(18,2) NOT NULL,
                          status       VARCHAR(20)  NOT NULL,
                          from_account VARCHAR(100),
                          to_account   VARCHAR(100),
                          ledger_reference VARCHAR(100),
                          failure_reason   VARCHAR(500),
                          created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                          CONSTRAINT idx_payments_reference_id UNIQUE (reference_id)
);