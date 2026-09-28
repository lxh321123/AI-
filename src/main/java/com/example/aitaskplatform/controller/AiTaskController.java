package com.example.aitaskplatform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aitaskplatform.common.Result;
import com.example.aitaskplatform.dto.CreateTaskRequest;
import com.example.aitaskplatform.dto.TaskPageRequest;
import com.example.aitaskplatform.service.AiTaskService;
import com.example.aitaskplatform.vo.CreateTaskResponse;
import com.example.aitaskplatform.vo.TaskDetailVO;
import com.example.aitaskplatform.vo.TaskResultVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tasks")
public class AiTaskController {

    private final AiTaskService aiTaskService;

    @PostMapping
    public Result<CreateTaskResponse> createTask(@Valid @RequestBody CreateTaskRequest request) {
        return Result.success(aiTaskService.createTask(request));
    }

    @GetMapping("/{taskId}")
    public Result<TaskDetailVO> getTaskDetail(@PathVariable @NotBlank String taskId) {
        return Result.success(aiTaskService.getTaskDetail(taskId));
    }

    @GetMapping
    public Result<Page<TaskDetailVO>> pageTasks(@Valid TaskPageRequest request) {
        return Result.success(aiTaskService.pageTasks(request));
    }

    @GetMapping("/{taskId}/result")
    public Result<TaskResultVO> getTaskResult(@PathVariable @NotBlank String taskId) {
        return Result.success(aiTaskService.getTaskResult(taskId));
    }

    @PostMapping("/{taskId}/retry")
    public Result<Void> retryTask(@PathVariable @NotBlank String taskId) {
        aiTaskService.retryTask(taskId);
        return Result.success();
    }
}
