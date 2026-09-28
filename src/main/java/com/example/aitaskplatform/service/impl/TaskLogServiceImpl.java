package com.example.aitaskplatform.service.impl;

import com.example.aitaskplatform.entity.AiTaskLog;
import com.example.aitaskplatform.mapper.AiTaskLogMapper;
import com.example.aitaskplatform.service.TaskLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TaskLogServiceImpl implements TaskLogService {

    private final AiTaskLogMapper aiTaskLogMapper;

    @Override
    public void record(String taskId, String eventType, String message) {
        AiTaskLog taskLog = new AiTaskLog();
        taskLog.setTaskId(taskId);
        taskLog.setEventType(eventType);
        taskLog.setMessage(message);
        taskLog.setCreatedAt(LocalDateTime.now());
        aiTaskLogMapper.insert(taskLog);
    }
}
