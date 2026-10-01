INSERT INTO users (
    name,
    login_id,
    password_hash,
    role,
    is_active
)
SELECT
    '佐藤 花子',
    'hanako',
    '$2a$10$EePXAlTGa6A883brWHI1OuWpzpEj0JZANk6HMBj6nZwRw1Jpl3f82',
    'MEMBER',
    TRUE
WHERE NOT EXISTS (
    SELECT 1
    FROM users
    WHERE login_id = 'hanako'
);

INSERT INTO users (
    name,
    login_id,
    password_hash,
    role,
    is_active
)
SELECT
    '鈴木 一郎',
    'admin',
    '$2a$10$HfiVf59zYq7hWa20Ka2/7OjNX/dCcouj1/1QzKCK.vPp5TqswtTq.',
    'ADMIN',
    TRUE
WHERE NOT EXISTS (
    SELECT 1
    FROM users
    WHERE login_id = 'admin'
);

INSERT INTO users (
    name,
    login_id,
    password_hash,
    role,
    is_active
)
SELECT
    '田中 三郎',
    'saburo',
    '$2a$10$W4FRHFTddgnaZzW0fd5arurSeTW8AOGNRtWvyIoA7ZGW5hXBy8.kG',
    'MEMBER',
    FALSE
WHERE NOT EXISTS (
    SELECT 1
    FROM users
    WHERE login_id = 'saburo'
);

INSERT INTO inquiries (
    title,
    body,
    requester_name,
    status,
    assignee_id,
    priority,
    created_at,
    updated_at,
    closed_at
)
SELECT
    'プリンタが印刷できません',
    '3階の複合機で印刷しようとするとエラーになります。',
    '山田 太郎',
    'NEW',
    NULL,
    'MIDDLE',
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6),
    NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM inquiries
    WHERE title = 'プリンタが印刷できません'
      AND requester_name = '山田 太郎'
);
