CREATE TABLE t_sharex_integration
(
    user_id   uuid PRIMARY KEY,
    folder_id uuid,

    FOREIGN KEY (user_id) REFERENCES t_users (id) ON DELETE CASCADE,
    FOREIGN KEY (folder_id) REFERENCES t_files (id) ON DELETE CASCADE
)