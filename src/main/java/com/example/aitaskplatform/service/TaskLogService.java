package com.example.aitaskplatform.service;

public interface TaskLogService {

    void record(String taskId, String eventType, String message);
}
