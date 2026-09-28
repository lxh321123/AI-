package com.example.aitaskplatform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aitaskplatform.dto.CreateTaskRequest;
import com.example.aitaskplatform.dto.TaskPageRequest;
import com.example.aitaskplatform.vo.CreateTaskResponse;
import com.example.aitaskplatform.vo.TaskDetailVO;
import com.example.aitaskplatform.vo.TaskResultVO;

public interface AiTaskService {

    CreateTaskResponse createTask(CreateTaskRequest request);

    TaskDetailVO getTaskDetail(String taskId);

    Page<TaskDetailVO> pageTasks(TaskPageRequest request);

    TaskResultVO getTaskResult(String taskId);

    void retryTask(String taskId);

    void processTask(String taskId);

    int markTimeoutTasksFailed(int timeoutMinutes);

    int retryFailedTasks();
}
