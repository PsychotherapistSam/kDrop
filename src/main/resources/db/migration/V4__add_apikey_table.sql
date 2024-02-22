CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE TABLE t_api_keys
(
    id         uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    api_key    VARCHAR(255) NOT NULL,
    user_id    uuid         NOT NULL,
    created_at TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES t_users (id)
);
