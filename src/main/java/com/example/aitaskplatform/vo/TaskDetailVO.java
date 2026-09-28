package com.example.aitaskplatform.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskDetailVO {

    private String taskId;
    private String bizId;
    private String taskName;
    private String taskType;
    private String inputText;
    private String status;
    private Integer priority;
    private Integer retryCount;
    private Integer maxRetryCount;
    private String failReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
