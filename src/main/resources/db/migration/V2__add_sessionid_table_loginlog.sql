ALTER TABLE public.t_login_log
    ADD COLUMN session_id varchar NULL,
    ADD COLUMN revoked    boolean NOT NULL DEFAULT false;

