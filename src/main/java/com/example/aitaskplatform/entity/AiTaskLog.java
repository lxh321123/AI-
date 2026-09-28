package com.example.aitaskplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_task_log")
public class AiTaskLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskId;
    private String eventType;
    private String message;
    private LocalDateTime createdAt;
}
