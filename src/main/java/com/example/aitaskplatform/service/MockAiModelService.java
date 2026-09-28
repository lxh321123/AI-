package com.example.aitaskplatform.service;

import com.example.aitaskplatform.entity.AiTask;
import com.example.aitaskplatform.entity.AiTaskResult;

public interface MockAiModelService {

    AiTaskResult execute(AiTask task);
}
