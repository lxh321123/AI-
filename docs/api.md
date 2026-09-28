# 接口文档

统一响应格式：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

## 1. 创建任务

- URL：`POST /api/tasks`
- 说明：创建 AI 任务，写入 MySQL 后异步投递 RabbitMQ。

请求：

```json
{
  "taskName": "简历摘要任务",
  "taskType": "SUMMARY",
  "inputText": "请总结这段文本",
  "bizId": "resume-001",
  "priority": 8
}
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "taskId": "AITxxxxxxxxxxxxxxxxxx"
  }
}
```

说明：

- `taskType` 支持 `SUMMARY`、`RISK_CHECK`、`QA_GENERATE`、`DOCUMENT_ANALYSIS`。
- `bizId` 用于幂等，重复提交会返回同一个 `taskId`。

## 2. 查询任务详情

- URL：`GET /api/tasks/{taskId}`
- 说明：优先查询 Redis，未命中再查询 MySQL。

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "taskId": "AITDEMO000000000001",
    "bizId": "demo-biz-001",
    "taskName": "合同风险识别演示任务",
    "taskType": "RISK_CHECK",
    "status": "SUCCESS",
    "priority": 8,
    "retryCount": 0,
    "maxRetryCount": 3,
    "failReason": null
  }
}
```

## 3. 分页查询任务列表

- URL：`GET /api/tasks`
- Query 参数：

| 参数 | 说明 |
| --- | --- |
| taskStatus | 可选，任务状态 |
| taskType | 可选，任务类型 |
| pageNo | 页码，默认 1 |
| pageSize | 每页数量，默认 10 |

示例：

```bash
curl "http://localhost:8080/api/tasks?taskStatus=SUCCESS&pageNo=1&pageSize=10"
```

## 4. 查询任务结果

- URL：`GET /api/tasks/{taskId}/result`
- 说明：成功任务返回结果；失败任务返回失败原因；处理中任务返回当前状态。

成功响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "taskId": "AITDEMO000000000001",
    "status": "SUCCESS",
    "resultText": "风险识别结果：付款周期较长...",
    "resultSummary": "付款周期较长...",
    "modelName": "mock-ai-model-v1",
    "tokenCount": 64,
    "costTimeMs": 320
  }
}
```

## 5. 手动重试任务

- URL：`POST /api/tasks/{taskId}/retry`
- 说明：仅 `FAILED` 状态任务可重试，且不能超过最大重试次数。

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

## 6. 健康检查

- URL：`GET /api/health`

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "status": "UP",
    "time": "2026-09-28T14:00:00"
  }
}
```

## 常见错误码

| code | 说明 |
| --- | --- |
| 400 | 参数错误 |
| 404 | 任务不存在 |
| 409 | 当前状态不允许操作 |
| 4091 | 相同 bizId 正在创建 |
| 429 | 超过最大重试次数 |
| 500 | 系统异常 |
