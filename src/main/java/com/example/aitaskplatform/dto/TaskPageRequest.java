package com.example.aitaskplatform.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class TaskPageRequest {

    private String taskStatus;
    private String taskType;

    @Min(value = 1, message = "pageNo must be greater than 0")
    private long pageNo = 1;

    @Min(value = 1, message = "pageSize must be greater than 0")
    @Max(value = 100, message = "pageSize must be less than 100")
    private long pageSize = 10;
}
