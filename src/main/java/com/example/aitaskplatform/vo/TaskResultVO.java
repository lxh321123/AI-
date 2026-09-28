package com.example.aitaskplatform.vo;

import lombok.Data;

@Data
public class TaskResultVO {

    private String taskId;
    private String status;
    private String failReason;
    private String resultText;
    private String resultSummary;
    private String modelName;
    private Integer tokenCount;
    private Long costTimeMs;
}
