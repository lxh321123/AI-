USE ai_task_platform;

INSERT INTO ai_task (
    task_id, biz_id, task_name, task_type, input_text, status, priority,
    retry_count, max_retry_count, fail_reason, created_at, updated_at, started_at, finished_at
) VALUES (
    'AITDEMO000000000001', 'demo-biz-001', '合同风险识别演示任务', 'RISK_CHECK',
    '请检查这段合同文本中是否存在付款周期过长、违约责任缺失等风险。',
    'SUCCESS', 8, 0, 3, NULL, NOW(), NOW(), NOW(), NOW()
);

INSERT INTO ai_task_result (
    task_id, result_text, result_summary, model_name, token_count, cost_time_ms, created_at, updated_at
) VALUES (
    'AITDEMO000000000001',
    '风险识别结果：付款周期较长，建议补充逾期付款责任和交付验收标准。',
    '付款周期较长，建议补充逾期付款责任和交付验收标准。',
    'mock-ai-model-v1', 64, 320, NOW(), NOW()
);

INSERT INTO ai_task_log (task_id, event_type, message, created_at) VALUES
('AITDEMO000000000001', 'CREATE', 'demo task initialized', NOW()),
('AITDEMO000000000001', 'SUCCESS', 'demo task processed successfully', NOW());
