CREATE DATABASE IF NOT EXISTS ai_task_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ai_task_platform;

DROP TABLE IF EXISTS ai_task_log;
DROP TABLE IF EXISTS ai_task_result;
DROP TABLE IF EXISTS ai_task;

CREATE TABLE ai_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'primary key',
    task_id VARCHAR(32) NOT NULL COMMENT 'business task id',
    biz_id VARCHAR(64) NOT NULL COMMENT 'caller idempotent business id',
    task_name VARCHAR(64) NOT NULL COMMENT 'task name',
    task_type VARCHAR(32) NOT NULL COMMENT 'SUMMARY/RISK_CHECK/QA_GENERATE/DOCUMENT_ANALYSIS',
    input_text TEXT NOT NULL COMMENT 'input payload',
    status VARCHAR(16) NOT NULL COMMENT 'PENDING/PROCESSING/SUCCESS/FAILED',
    priority INT NOT NULL DEFAULT 5 COMMENT '1-10, higher means more important',
    retry_count INT NOT NULL DEFAULT 0 COMMENT 'current retry count',
    max_retry_count INT NOT NULL DEFAULT 3 COMMENT 'max retry count',
    fail_reason VARCHAR(512) NULL COMMENT 'last failed reason',
    created_at DATETIME NOT NULL COMMENT 'created time',
    updated_at DATETIME NOT NULL COMMENT 'updated time',
    started_at DATETIME NULL COMMENT 'processing start time',
    finished_at DATETIME NULL COMMENT 'finished time',
    UNIQUE KEY uk_task_id (task_id),
    UNIQUE KEY uk_biz_id (biz_id),
    KEY idx_status_priority_created (status, priority, created_at),
    KEY idx_task_type_created (task_type, created_at),
    KEY idx_started_at (started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI task main table';

CREATE TABLE ai_task_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'primary key',
    task_id VARCHAR(32) NOT NULL COMMENT 'business task id',
    result_text TEXT NOT NULL COMMENT 'mock AI result text',
    result_summary VARCHAR(255) NOT NULL COMMENT 'result summary',
    model_name VARCHAR(64) NOT NULL COMMENT 'mock model name',
    token_count INT NOT NULL DEFAULT 0 COMMENT 'mock token count',
    cost_time_ms BIGINT NOT NULL DEFAULT 0 COMMENT 'mock cost time',
    created_at DATETIME NOT NULL COMMENT 'created time',
    updated_at DATETIME NOT NULL COMMENT 'updated time',
    UNIQUE KEY uk_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI task result table';

CREATE TABLE ai_task_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'primary key',
    task_id VARCHAR(32) NOT NULL COMMENT 'business task id',
    event_type VARCHAR(32) NOT NULL COMMENT 'event type',
    message VARCHAR(512) NOT NULL COMMENT 'event message',
    created_at DATETIME NOT NULL COMMENT 'created time',
    KEY idx_task_id_created (task_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI task operation log';
