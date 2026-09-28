package com.example.aitaskplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_task")
public class AiTask {

    @TableId(type = IdType.AUTO)
    private Long id;
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
