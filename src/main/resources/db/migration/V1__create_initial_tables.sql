CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE public.t_users
(
    id                uuid         NOT NULL DEFAULT uuid_generate_v4(),
    "name"            varchar(50)  NOT NULL,
    "password"        varchar(256) NOT NULL,
    roles             varchar(50)  NOT NULL,
    preferences       varchar(256) NOT NULL,
    registration_date timestamp    NOT NULL,
    totp_secret       varchar(256) NULL,
    root_folder_id    uuid         NULL,
    salt              varchar(64)  NULL,
    CONSTRAINT t_users_pkey PRIMARY KEY (id)
);

CREATE TABLE public.t_files
(
    id         uuid         NOT NULL,
    "name"     varchar(300) NOT NULL,
    "path"     varchar(128) NOT NULL,
    mime_type  varchar(128) NOT NULL,
    parent     uuid         NULL,
    "owner"    uuid         NOT NULL,
    "size"     int8         NOT NULL,
    size_hr    varchar(128) NOT NULL,
    "password" varchar(256) NULL,
    created    timestamp    NOT NULL,
    is_folder  bool         NOT NULL,
    hash       varchar(128) NULL,
    is_root    bool         NOT NULL,
    CONSTRAINT t_files_pkey PRIMARY KEY (id),
    CONSTRAINT fk_t_files_owner_id FOREIGN KEY ("owner") REFERENCES public.t_users (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_t_files_parent_id FOREIGN KEY (parent) REFERENCES public.t_files (id) ON DELETE RESTRICT ON UPDATE RESTRICT
);


CREATE TABLE public.t_login_log
(
    id         uuid         NOT NULL,
    "user"     uuid         NOT NULL,
    ip         varchar(255) NOT NULL,
    user_agent varchar(255) NOT NULL,
    "date"     timestamp    NOT NULL,
    CONSTRAINT t_login_log_pkey PRIMARY KEY (id),
    CONSTRAINT fk_t_login_log_user_id FOREIGN KEY ("user") REFERENCES public.t_users (id) ON DELETE RESTRICT ON UPDATE RESTRICT
);


CREATE TABLE public.t_shares
(
    id             uuid         NOT NULL,
    file           uuid         NOT NULL,
    "user"         uuid         NOT NULL,
    creation_date  timestamp    NOT NULL,
    max_downloads  int8         NULL,
    download_count int8         NOT NULL,
    vanity_name    varchar(255) NULL,
    "password"     varchar(255) NULL,
    CONSTRAINT t_shares_pkey PRIMARY KEY (id),
    CONSTRAINT fk_t_shares_file_id FOREIGN KEY (file) REFERENCES public.t_files (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_t_shares_user_id FOREIGN KEY ("user") REFERENCES public.t_users (id) ON DELETE RESTRICT ON UPDATE RESTRICT
);



CREATE TABLE public.t_download_log
(
    id            uuid         NOT NULL,
    file          uuid         NULL,
    "user"        uuid         NULL,
    ip            varchar(50)  NOT NULL,
    download_date timestamp    NOT NULL,
    read_bytes    int8         NOT NULL,
    read_duration int8         NOT NULL,
    user_agent    varchar(500) NOT NULL,
    zip_file_name varchar(500) NULL,
    CONSTRAINT t_download_log_pkey PRIMARY KEY (id),
    CONSTRAINT fk_t_download_log_file_id FOREIGN KEY (file) REFERENCES public.t_files (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_t_download_log_user_id FOREIGN KEY ("user") REFERENCES public.t_users (id) ON DELETE RESTRICT ON UPDATE RESTRICT
);