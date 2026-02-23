CREATE TABLE ledger_entries (
                                id BIGSERIAL PRIMARY KEY,
                                transaction_id BIGINT NOT NULL,
                                ledger_account_id BIGINT NOT NULL,
                                entry_type VARCHAR(10) NOT NULL,
                                amount NUMERIC(18,2) NOT NULL,

                                CONSTRAINT fk_entry_transaction
                                    FOREIGN KEY (transaction_id)
                                        REFERENCES ledger_transactions (id),

                                CONSTRAINT fk_entry_ledger_account
                                    FOREIGN KEY (ledger_account_id)
                                        REFERENCES ledger_accounts (id)
);
