DROP TABLE IF EXISTS refresh_tokens;
DROP TABLE IF EXISTS sr_history;
DROP TABLE IF EXISTS sr;
DROP TABLE IF EXISTS open_api_survey;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS organizations;
DROP SERIAL IF EXISTS user_seq;
DROP SERIAL IF EXISTS sr_seq;
DROP SERIAL IF EXISTS sr_history_seq;
DROP SERIAL IF EXISTS open_api_survey_seq;

CREATE SERIAL user_seq START WITH 1 INCREMENT BY 1;
CREATE SERIAL sr_seq START WITH 1 INCREMENT BY 1;
CREATE SERIAL sr_history_seq START WITH 1 INCREMENT BY 1;
CREATE SERIAL open_api_survey_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    user_role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE organizations (
    code VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE open_api_survey (
    id BIGINT PRIMARY KEY,
    organization_code VARCHAR(20) NOT NULL,
    department VARCHAR(100) NOT NULL,
    contact_name VARCHAR(50) NOT NULL,
    contact_phone VARCHAR(30) NOT NULL,
    contact_email VARCHAR(100) NOT NULL,
    received_file_name VARCHAR(255),
    received_date DATE NOT NULL,
    system_name VARCHAR(100) NOT NULL,
    current_method VARCHAR(20) NOT NULL,
    desired_method VARCHAR(20) NOT NULL,
    reason_for_distributed VARCHAR(4096),
    maintenance_operation VARCHAR(30) NOT NULL,
    maintenance_location VARCHAR(20) NOT NULL,
    maintenance_address VARCHAR(255),
    maintenance_note VARCHAR(4096),
    operation_env VARCHAR(20) NOT NULL,
    server_location VARCHAR(255),
    web_server_os VARCHAR(20),
    web_server_os_type VARCHAR(50),
    web_server_os_version VARCHAR(50),
    web_server_type VARCHAR(20),
    web_server_type_other VARCHAR(50),
    web_server_version VARCHAR(50),
    was_server_os VARCHAR(20),
    was_server_os_type VARCHAR(50),
    was_server_os_version VARCHAR(50),
    was_server_type VARCHAR(20),
    was_server_type_other VARCHAR(50),
    was_server_version VARCHAR(50),
    db_server_os VARCHAR(20),
    db_server_os_type VARCHAR(50),
    db_server_os_version VARCHAR(50),
    db_server_type VARCHAR(20),
    db_server_type_other VARCHAR(50),
    db_server_version VARCHAR(50),
    dev_language VARCHAR(20),
    dev_language_other VARCHAR(50),
    dev_language_version VARCHAR(50),
    dev_framework VARCHAR(20),
    dev_framework_other VARCHAR(50),
    dev_framework_version VARCHAR(50),
    other_requests VARCHAR(4096),
    note VARCHAR(4096),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sr (
    id BIGINT PRIMARY KEY,
    sr_id VARCHAR(20) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(4096),
    processing_details VARCHAR(4096),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    requester_id BIGINT NOT NULL,
    assignee_id BIGINT,
    open_api_survey_id BIGINT,
    applicant_name VARCHAR(100),
    applicant_phone VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    FOREIGN KEY (requester_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (assignee_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE sr_history (
    id BIGINT PRIMARY KEY,
    sr_id BIGINT NOT NULL,
    content VARCHAR(4096) NOT NULL,
    history_type VARCHAR(20) NOT NULL,
    previous_value VARCHAR(4096),
    new_value VARCHAR(4096),
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sr_id) REFERENCES sr(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE refresh_tokens (
    id VARCHAR(36) PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
