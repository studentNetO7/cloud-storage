---- Включаем pgcrypto для UUID
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Таблица users
CREATE TABLE IF NOT EXISTS users (
                                     id UUID NOT NULL DEFAULT gen_random_uuid(),
                                     username VARCHAR(255) NOT NULL,
                                     password_hash VARCHAR(255) NOT NULL,
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     CONSTRAINT users_pkey PRIMARY KEY (id),
                                     CONSTRAINT users_username_key UNIQUE (username)
);

-- Таблица files
CREATE TABLE IF NOT EXISTS files (
                                     id UUID NOT NULL DEFAULT gen_random_uuid(),
                                     user_id UUID NOT NULL,
                                     filename VARCHAR(255) NOT NULL,
                                     size BIGINT NOT NULL,
                                     upload_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     path TEXT NOT NULL CHECK (char_length(path) > 0),
                                     is_deleted BOOLEAN DEFAULT FALSE,
                                     CONSTRAINT files_pkey PRIMARY KEY (id),
                                     CONSTRAINT files_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
