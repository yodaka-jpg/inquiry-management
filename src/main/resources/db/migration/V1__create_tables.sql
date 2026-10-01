CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    PRIMARY KEY (id),

    CONSTRAINT chk_users_role
        CHECK (role IN ('ADMIN', 'MEMBER'))
);

CREATE TABLE inquiries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    body TEXT NOT NULL,
    requester_name VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    assignee_id BIGINT NULL,
    priority VARCHAR(10) NOT NULL DEFAULT 'MIDDLE',
    created_at DATETIME(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),
    closed_at DATETIME(6) NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_inquiries_assignee
        FOREIGN KEY (assignee_id)
        REFERENCES users (id),

    CONSTRAINT chk_inquiries_status
        CHECK (
            status IN (
                'NEW',
                'IN_PROGRESS',
                'PENDING',
                'DONE'
            )
        ),

    CONSTRAINT chk_inquiries_priority
        CHECK (
            priority IN (
                'HIGH',
                'MIDDLE',
                'LOW'
            )
        )
);

CREATE TABLE inquiry_histories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    inquiry_id BIGINT NOT NULL,
    changed_by BIGINT NOT NULL,
    field_name VARCHAR(20) NOT NULL,
    old_value VARCHAR(50) NULL,
    new_value VARCHAR(50) NULL,
    changed_at DATETIME(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),
    comment VARCHAR(200) NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_histories_inquiry
        FOREIGN KEY (inquiry_id)
        REFERENCES inquiries (id),

    CONSTRAINT fk_histories_changed_by
        FOREIGN KEY (changed_by)
        REFERENCES users (id),

    CONSTRAINT chk_histories_field
        CHECK (
            field_name IN (
                'status',
                'assignee'
            )
        )
);

CREATE INDEX idx_inquiries_status
    ON inquiries (status);

CREATE INDEX idx_inquiries_assignee
    ON inquiries (assignee_id);

CREATE INDEX idx_histories_inquiry_changed
    ON inquiry_histories (
        inquiry_id,
        changed_at DESC,
        id DESC
    );
