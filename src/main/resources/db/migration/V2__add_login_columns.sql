ALTER TABLE users
    ADD COLUMN login_id VARCHAR(100) NULL,
    ADD COLUMN password_hash VARCHAR(100) NULL;

UPDATE users
SET
    login_id = 'hanako',
    password_hash = '$2a$10$EePXAlTGa6A883brWHI1OuWpzpEj0JZANk6HMBj6nZwRw1Jpl3f82'
WHERE id = 1;

UPDATE users
SET
    login_id = 'admin',
    password_hash = '$2a$10$HfiVf59zYq7hWa20Ka2/7OjNX/dCcouj1/1QzKCK.vPp5TqswtTq.'
WHERE id = 2;

UPDATE users
SET
    login_id = 'saburo',
    password_hash = '$2a$10$W4FRHFTddgnaZzW0fd5arurSeTW8AOGNRtWvyIoA7ZGW5hXBy8.kG'
WHERE id = 3;

ALTER TABLE users
    MODIFY COLUMN login_id VARCHAR(100) NOT NULL,
    MODIFY COLUMN password_hash VARCHAR(100) NOT NULL,
    ADD CONSTRAINT uk_users_login_id
        UNIQUE (login_id);
