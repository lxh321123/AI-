package com.example.aitaskplatform.util;

public final class RedisKeys {

    private static final String PREFIX = "ai-task:";

    private RedisKeys() {
    }

    public static String createLock(String bizId) {
        return PREFIX + "create-lock:" + bizId;
    }

    public static String bizTask(String bizId) {
        return PREFIX + "biz-task:" + bizId;
    }

    public static String taskDetail(String taskId) {
        return PREFIX + "task-detail:" + taskId;
    }

    public static String taskResult(String taskId) {
        return PREFIX + "task-result:" + taskId;
    }

    public static String processLock(String taskId) {
        return PREFIX + "process-lock:" + taskId;
    }
}
