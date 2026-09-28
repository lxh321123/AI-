package com.example.aitaskplatform.service.impl;

import com.example.aitaskplatform.common.TaskType;
import com.example.aitaskplatform.entity.AiTask;
import com.example.aitaskplatform.entity.AiTaskResult;
import com.example.aitaskplatform.service.MockAiModelService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class MockAiModelServiceImpl implements MockAiModelService {

    @Override
    public AiTaskResult execute(AiTask task) {
        long start = System.currentTimeMillis();
        simulateCost();

        String resultText = buildResult(task);
        AiTaskResult result = new AiTaskResult();
        result.setTaskId(task.getTaskId());
        result.setResultText(resultText);
        result.setResultSummary(resultText.length() > 80 ? resultText.substring(0, 80) : resultText);
        result.setModelName("mock-ai-model-v1");
        result.setTokenCount(Math.max(16, task.getInputText().length() / 2));
        result.setCostTimeMs(System.currentTimeMillis() - start);
        result.setCreatedAt(LocalDateTime.now());
        result.setUpdatedAt(LocalDateTime.now());
        return result;
    }

    private void simulateCost() {
        try {
            Thread.sleep(300L);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private String buildResult(AiTask task) {
        String prefix = switch (TaskType.valueOf(task.getTaskType())) {
            case SUMMARY -> "摘要结果：";
            case RISK_CHECK -> "风险识别结果：未发现高风险表达，建议人工复核关键条款。";
            case QA_GENERATE -> "问答生成结果：已根据输入内容生成 3 个候选问题及回答。";
            case DOCUMENT_ANALYSIS -> "文档分析结果：已抽取主题、关键词和待办事项。";
        };
        return prefix + "任务[" + task.getTaskName() + "]处理完成，输入片段：" + task.getInputText();
    }
}
