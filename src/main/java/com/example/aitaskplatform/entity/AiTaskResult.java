package com.example.aitaskplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_task_result")
public class AiTaskResult {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskId;
    private String resultText;
    private String resultSummary;
    private String modelName;
    private Integer tokenCount;
    private Long costTimeMs;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
