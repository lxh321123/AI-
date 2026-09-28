package com.example.aitaskplatform.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTaskRequest {

    @NotBlank(message = "taskName cannot be blank")
    @Size(max = 64, message = "taskName length must be less than 64")
    private String taskName;

    @NotBlank(message = "taskType cannot be blank")
    private String taskType;

    @NotBlank(message = "inputText cannot be blank")
    @Size(max = 4000, message = "inputText length must be less than 4000")
    private String inputText;

    @NotBlank(message = "bizId cannot be blank")
    @Size(max = 64, message = "bizId length must be less than 64")
    private String bizId;

    @NotNull(message = "priority cannot be null")
    @Min(value = 1, message = "priority must be between 1 and 10")
    @Max(value = 10, message = "priority must be between 1 and 10")
    private Integer priority;
}
