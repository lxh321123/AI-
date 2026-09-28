# AI 任务调度与结果管理平台

这是一个面向 Java 后端校招面试的完整业务型项目。业务上模拟企业内部 AI 任务平台，支持创建 AI 分析任务、异步调度处理、结果查询、失败重试和超时补偿；AI 能力使用 Mock 服务模拟，项目重点放在 Spring Boot 后端系统设计。

## 技术栈

- Java 17、Spring Boot 3、Maven
- MySQL 8、MyBatis-Plus
- Redis 7
- RabbitMQ
- XXL-Job
- Docker Compose、Nginx
- JUnit 5、Mockito
- Knife4j / OpenAPI

## 核心亮点

- 使用 `bizId` + Redis 锁 + MySQL 唯一索引实现任务创建幂等。
- 使用 RabbitMQ 解耦任务创建和 AI 处理，避免接口长时间阻塞。
- 使用事务提交后投递 MQ，降低“消息发出但数据库回滚”的不一致风险。
- 消费端使用 Redis 分布式锁 + 状态判断 + 结果唯一索引防止重复消费。
- 使用 Redis 缓存任务详情和任务结果，状态变化后主动清理缓存。
- 使用 XXL-Job 定时扫描超时任务和可重试失败任务。
- 提供统一响应、统一异常处理、参数校验、任务日志和基础测试。

## 项目结构

```text
ai-task-platform
├── src/main/java/com/example/aitaskplatform
│   ├── controller      # REST 接口
│   ├── service         # 业务接口
│   ├── service/impl    # 业务实现
│   ├── mapper          # MyBatis-Plus Mapper
│   ├── entity          # 数据库实体
│   ├── dto             # 请求对象 / MQ 消息
│   ├── vo              # 响应对象
│   ├── mq              # RabbitMQ 生产者 / 消费者
│   ├── job             # XXL-Job / 本地定时任务
│   ├── config          # 中间件配置
│   ├── common          # 通用枚举和响应结构
│   ├── exception       # 统一异常处理
│   └── util            # Redis Key 工具
├── sql                 # 建表与初始化数据
├── docs                # 接口、架构、面试讲解文档
├── docker              # Nginx / XXL-Job 相关配置
├── docker-compose.yml
├── Dockerfile
└── pom.xml
```

## 本地启动

先启动依赖服务：

```bash
docker compose up -d mysql redis rabbitmq
```

再启动应用：

```bash
mvn spring-boot:run
```

访问：

- 应用健康检查：http://localhost:8080/api/health
- Knife4j 文档：http://localhost:8080/doc.html
- RabbitMQ 控制台：http://localhost:15672，账号 `guest`，密码 `guest`

## Docker Compose 一键启动

```bash
docker compose up -d --build
```

启动后可以通过 Nginx 入口访问：

```bash
curl http://localhost/api/health
```

XXL-Job Admin 地址：

```text
http://localhost:8088/xxl-job-admin
账号：admin
密码：123456
```

默认应用内也启用了 Spring `@Scheduled`，所以不配置 XXL-Job 也能看到超时扫描和失败重试逻辑。需要注册到 XXL-Job 时，把环境变量 `XXL_JOB_ENABLED=true`，然后在 Admin 中创建执行器 `ai-task-platform-executor`，任务 Handler 分别配置为 `markTimeoutAiTaskJob` 和 `retryFailedAiTaskJob`。

## 常用接口示例

创建任务：

```bash
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d "{\"taskName\":\"简历摘要任务\",\"taskType\":\"SUMMARY\",\"inputText\":\"请总结这段文本\",\"bizId\":\"resume-001\",\"priority\":8}"
```

查询任务详情：

```bash
curl http://localhost:8080/api/tasks/{taskId}
```

查询任务结果：

```bash
curl http://localhost:8080/api/tasks/{taskId}/result
```

手动重试失败任务：

```bash
curl -X POST http://localhost:8080/api/tasks/{taskId}/retry
```

分页查询：

```bash
curl "http://localhost:8080/api/tasks?pageNo=1&pageSize=10&taskStatus=SUCCESS"
```

## 面试可讲点

- 为什么创建接口不直接同步调用 AI：AI 任务耗时不稳定，使用 MQ 可以削峰、异步处理、提升接口响应速度。
- 如何做幂等：入口用 `bizId` 识别业务请求，Redis 锁防并发，MySQL 唯一索引兜底。
- 如何处理消息一致性：任务先落库，事务提交后再投递 MQ，避免数据库回滚后消息已经被消费。
- 如何防重复消费：消费端 Redis 锁、任务状态判断、结果表 `task_id` 唯一索引三层防护。
- 如何做失败补偿：失败任务记录原因，支持手动重试和 XXL-Job 定时自动重试。
- 如何做缓存：任务详情和结果查 Redis，状态变化后删除缓存，避免读到旧状态。

## 常见问题

### 为什么 AI 服务是 Mock？

这个项目目标是证明 Java 后端能力。真实大模型调用可以替换 `MockAiModelService`，但简历和面试重点应放在任务调度、异步处理、缓存、幂等、事务和部署。

### 为什么同时有 XXL-Job 和 `@Scheduled`？

为了本地体验更简单，`@Scheduled` 可以直接跑；为了体现企业级调度能力，同一个方法也标了 `@XxlJob`，接入 XXL-Job 后可由调度中心统一管理。

### 这个项目适合怎么写进简历？

见 [docs/resume-description.md](docs/resume-description.md)。
