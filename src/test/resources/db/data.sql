-- Вставка тестового пользователя
INSERT INTO users (username, password_hash)
VALUES ('testuser', '$2a$10$ptWqdnoApEQ.W2lhdXorj.o8dPnRQDoM07H02y4hh1MEhkt5EpPgq');

INSERT INTO files (id, user_id, filename, size, upload_date, path, is_deleted)
VALUES (gen_random_uuid(),
        (SELECT id FROM users WHERE username = 'testuser'),
        'example.txt',
        123,
        NOW(),
        '/test-storage/example.txt',
        FALSE);
