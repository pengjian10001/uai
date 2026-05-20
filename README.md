# UAI 项目

UAI 是一个面向 Agent/LLM 的服务集合，包含 MCP 工具管理、会话记忆、SSE 流式输出及 Web 端调试能力。

## 模块说明

- `uai-mcp-server`：MCP Server，负责工具注册、工具调用与对话编排。
- `uai-mcp-base`：MCP 基础能力与数据库结构（DDL）。
- `uai-web-boot`：Spring Boot Web 容器能力（含 WebSocket/SSE 集成）。
- 其他模块（`uai-common`、`uai-util`、`uai-rag`、`uai-vec`、`uai-graph`）提供公共能力与扩展支撑。

## 快速开始

### 1. 初始化数据库

```bash
mysql -u root -p < uai-mcp-base/src/main/resources/sql/mcp_ddl.sql
```

若库已存在且写入 emoji 报错，再执行 `uai-mcp-base/src/main/resources/sql/mcp_utf8mb4_fix.sql`。

### 2. 配置 LLM

阻塞调用与 SSE 流式输出**共用同一套 LLM 配置**（见 `ChatModelFactory`），请确保两者指向同一 provider，避免流式阶段中文乱码。

#### 配置项说明

| 环境变量 | JVM / YAML 属性 | 说明 | 默认值 |
|---------|----------------|------|--------|
| `UAI_LLM_API_KEY` | `uai.llm.api-key` | API Key（必填，demo 除外） | 无 |
| `UAI_LLM_BASE_URL` | `uai.llm.base-url` | OpenAI 兼容接口地址 | `https://api.deepseek.com` |
| `UAI_LLM_MODEL_NAME` | `uai.llm.model-name` | 模型名称 | `deepseek-chat` |

#### 配置优先级（高 → 低）

1. **OS 环境变量**（`export UAI_LLM_API_KEY=...`）
2. **JVM 参数**（`-Duai.llm.api-key=...`）
3. **`.env` 文件**（项目根目录，启动时由 `EnvFileLoader` 自动加载为 System Property；**不覆盖** 1、2 中已存在的值）
4. **`application-{profile}.yml`** 中的 `uai.llm.*`
5. **代码默认值**

> 注意：Java 进程**不会**像 Node.js 那样自动读取 `.env`。本项目在 `ChatModelFactory` 初始化前调用 `EnvFileLoader.loadIfPresent()`，将 `.env` 中的 `UAI_LLM_*` 映射为 `uai.llm.*` 系统属性，供后续配置解析使用。

#### 方式 A：使用 `.env`（本地开发推荐）

```bash
cp .env.example .env
# 编辑 .env，变量名必须为 UAI_LLM_API_KEY，不能写成 api-key
```

`.env` 格式：

```bash
UAI_LLM_API_KEY=sk-your-real-api-key
UAI_LLM_BASE_URL=https://api.deepseek.com
UAI_LLM_MODEL_NAME=deepseek-chat
```

启动成功后，日志中应出现：

```
Loaded env file: /path/to/uai/.env
```

`.env` 会从当前工作目录向上最多查找 6 层父目录，一般放在**项目根目录**即可。

#### 方式 B：export 环境变量

```bash
export UAI_LLM_API_KEY="your_api_key"
export UAI_LLM_BASE_URL="https://api.deepseek.com"
export UAI_LLM_MODEL_NAME="deepseek-chat"
```

#### 方式 C：YAML 配置

在 `uai-mcp-server/src/main/resources/application-local.yml`（本地私有，勿提交密钥）中配置：

```yaml
uai:
  llm:
    base-url: https://api.deepseek.com
    model-name: deepseek-chat
    api-key: your_api_key
```

使用 `langchain4j.dev` demo 时，`UAI_LLM_API_KEY` 可省略，api-key 自动为 `demo`。

### 3. 配置数据库连接

编辑对应 profile 的配置文件（如 `uai-mcp-server/src/main/resources/application-local.yml`），修改 `spring.datasource` 与 `mysql.datasource` 的地址、用户名、密码。

### 4. 启动服务

```bash
mvn clean install jetty:run -Dspring.profiles.active=local
```

配置读取依赖 `spring.profiles.active`，请确保对应 profile 的数据源与 LLM 配置正确。

### 5. 验证对话

浏览器打开流式对话页：

```
http://localhost:8080/static/mcp/chat/stream/ssebox
```

或使用 curl 调用 SSE 接口：

```bash
curl 'http://localhost:8080/commonsse/api/query_ai' \
  -H 'Connection: keep-alive' \
  -H 'Content-Type: application/json' \
  --data '{"sessionId":"test-session-001","clientName":"test-client","ucid":"0","message":"你有什么工具，表格展示"}'
```

建议请求中显式传递 `sessionId` 与 `ucid`，便于多轮记忆与日志追踪。

## MCP 核心设计

MCP 通过「工具 + 标签 + 标签工具关系」实现 Agent 角色化编排：

- `t_tool`：工具定义（名称、描述、参数 schema、返回类型、数据源配置等）。
- `t_label`：标签定义（可作为 clientName 对应的角色，`description` 常用于 system prompt）。
- `t_label_tool`：标签与工具绑定关系（某个角色可用哪些工具）。

典型流程：

1. 开发并注册原子能力到 `t_tool`。
2. 在 `t_label` 中定义角色（system prompt）。
3. 在 `t_label_tool` 关联角色与工具。
4. 调用时通过 `clientName` 选择角色，驱动工具调用链。

## 数据库初始化

参考 `uai-mcp-base/src/main/resources/sql/mcp_ddl.sql`，默认库名为 `mcp`，核心表包括：

- `t_chat_message`：会话消息历史（`session_id`、`single_id`、消息类型与内容）。
- `t_server`：工具服务定义（`local`/`http` 数据源）。
- `t_tool`：工具定义与执行配置。
- `t_prompt`：Prompt 模板定义与参数 schema。
- `t_label`、`t_label_tool`：角色标签及工具绑定。

建议先执行 DDL，再导入初始工具与标签数据。

```bash
mysql -u root -p < uai-mcp-base/src/main/resources/sql/mcp_ddl.sql
```

### 字符集（utf8mb4）

会话消息可能包含 emoji 等 4 字节 UTF-8 字符，`t_chat_message.content` 必须使用 `utf8mb4`。若遇到如下报错：

```
Incorrect string value: '\xF0\x9F...' for column 'content'
```

请执行修复脚本：

```bash
mysql -u root -p mcp < uai-mcp-base/src/main/resources/sql/mcp_utf8mb4_fix.sql
```

JDBC 连接也需使用 utf8mb4（项目 `application-*.yml` 已默认配置）：

```
characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci
```

## clientName 路由规则

`clientName` 用于选择角色标签，优先级从高到低：

1. 用户输入前缀 `@clientName`
2. URL 参数 `clientName`
3. MCP Client 默认传参 `clientName`

系统内置标签 `_mcp` 可用于系统工具能力（例如自然语言查询 MCP 元数据）。

## 会话与记忆

- 支持 `sessionId` 透传并持久化历史消息到 `t_chat_message`。
- 可进行多轮记忆对话（同一 `sessionId` 下上下文连续）。
- 建议请求中显式传递 `sessionId` 与 `ucid`，便于追踪与鉴权集成。

## SSE 流式流程（UAI 侧）

SSE 的关键链路如下：

1. Servlet 初始化 SSE 响应头并启动异步上下文（`AsyncContext`）。
2. 异步线程处理业务并通过上下文持续 `send` 数据。
3. 业务完成或异常后调用 `complete()` 结束连接。
4. 对接 LangChain4j 流式回调时，需要显式传递 `AsyncContext`，避免异步嵌套导致 ThreadLocal 丢失。

`DynamicServletListener` 注册时需开启异步支持：

- 路径：`/commonsse/*`
- 配置：`setAsyncSupported(true)`

## MCP + SSE 的调用特点

当请求包含 MCP 工具调用时，流式过程通常分两段：

1. **阻塞判定阶段**：先调用模型判断是否需要调用工具（此阶段常出现 `AiMessage.text == null`，不向前端流式输出）。
2. **流式回答阶段**：工具执行完成后，再进行第二次模型调用并流式输出最终答案。

该策略已合并到统一的 MCP Client 基类流程中。

## 调试与测试入口

常用页面与接口：

- `http://localhost:8080/pages/mcp/chat/box`
- `http://localhost:8080/static/mcp/chat/lighthtml`
- `http://localhost:8080/static/mcp/chat/lightmarkdown`
- `http://localhost:8080/static/mcp/chat/stream/ssebox`
- `http://localhost:8080/static/mcp/chat/stream/uploadssebox`
- `http://localhost:8080/static/mcp/chat/websocketbox`

SSE curl 示例：

```bash
curl 'http://localhost:8080/commonsse/api/query_ai' \
  -H 'Connection: keep-alive' \
  -H 'Content-Type: application/json' \
  --data '{"sessionId":"mhn9kcp3hj9woc4k","clientName":"test-client","ucid":"0","message":"北京天气和交通情况"}'
```

## 敏感信息配置

LLM 相关配置由 `ChatModelFactory` 统一创建阻塞模型与流式模型，密钥不在代码中写死。

实现要点：

- `EnvFileLoader`：启动时将项目根目录 `.env` 加载为 System Property
- `ChatModelFactory.resolveConfig()`：按优先级合并环境变量、JVM 参数、YAML 与默认值
- 阻塞模型（`getDefaultChatModel`）与流式模型（`getDefaultStreamingChatModel`）必须使用相同 `baseUrl` / `modelName` / `apiKey`

必填配置（任选一种来源）：

- 环境变量：`UAI_LLM_API_KEY`
- JVM 参数：`-Duai.llm.api-key=...`
- `.env` 文件：`UAI_LLM_API_KEY=...`（自动加载）
- YAML：`uai.llm.api-key`

可选配置（有默认值）：

- `UAI_LLM_BASE_URL` / `uai.llm.base-url`（默认 `https://api.deepseek.com`）
- `UAI_LLM_MODEL_NAME` / `uai.llm.model-name`（默认 `deepseek-chat`）

启动示例（`.env` 方式）：

```bash
cp .env.example .env   # 编辑填入真实 key
mvn clean install jetty:run -Dspring.profiles.active=local
```

启动示例（export 方式）：

```bash
export UAI_LLM_API_KEY="your_api_key"
export UAI_LLM_BASE_URL="https://api.deepseek.com"
export UAI_LLM_MODEL_NAME="deepseek-chat"
mvn clean install jetty:run -Dspring.profiles.active=local
```

若未配置 API Key，首次调用 LLM 时会在 `ChatModelFactory` 静态初始化阶段抛出 `IllegalStateException`。

### 提交前防泄露（pre-commit 推荐）

建议在本地安装 `pre-commit` 与 `gitleaks`，在每次提交前自动扫描密钥：

```bash
brew install pre-commit gitleaks
```

在项目根目录创建 `.pre-commit-config.yaml`：

```yaml
repos:
  - repo: https://github.com/gitleaks/gitleaks
    rev: v8.24.2
    hooks:
      - id: gitleaks
```

启用钩子：

```bash
pre-commit install
pre-commit run --all-files
```

如果扫描命中疑似密钥，先处理后再提交。

## 常见问题

### Missing LLM API key（`.env` 未生效）

**现象**：请求时报错 `Missing LLM API key. Please set environment variable UAI_LLM_API_KEY or JVM property uai.llm.api-key`。

**常见原因**：

1. `.env` 中变量名写错（如写成 `api-key=` 而非 `UAI_LLM_API_KEY=`）
2. `.env` 放在错误目录（应放在项目根目录，或确保从根目录启动）
3. 修改 `.env` 后未重新编译启动（`ChatModelFactory` 在类加载时初始化，需重启进程）
4. `.env` 中的值仍为占位符 `replace_with_your_real_api_key`

**处理**：

1. 对照 `.env.example` 检查变量名与格式
2. 重启并确认日志出现 `Loaded env file: ...`；若无此行，说明未找到 `.env`
3. 或改用 `export UAI_LLM_API_KEY=...` / YAML / JVM 参数

### 写入会话历史报错（MySQL 1366）

**现象**：日志出现 `Incorrect string value: '\xF0\x9F...' for column 'content'`。

**原因**：数据库连接或表字符集不支持 emoji 等 4 字节字符。

**处理**：

1. 执行 `uai-mcp-base/src/main/resources/sql/mcp_utf8mb4_fix.sql`
2. 确认 JDBC URL 含 `connectionCollation=utf8mb4_unicode_ci`
3. 重启服务

### SSE 流式响应中文乱码

**现象**：第一次 LLM 调用日志正常，但前端或 `onCompleteResponse` 输出为 `????`。

**原因**：阻塞模型与流式模型使用了不同的 LLM provider。例如阻塞走 DeepSeek、流式仍走 `langchain4j.dev` demo（SSE 响应 `charset=iso-8859-1`），会导致中文解析错误。

**处理**：统一 `UAI_LLM_BASE_URL`、`UAI_LLM_MODEL_NAME`、`UAI_LLM_API_KEY`，使阻塞与流式使用同一 provider，然后重启服务。

### LogPushTask 未启动

**现象**：日志反复打印 `LogPushTask未启动，ubag.log.timer.task.push.enable = false`。

**说明**：这是 ubag 日志推送开关未开启的提示，不影响 MCP 对话与 `t_chat_message` 写入。如需开启，在 `ubag-conf.properties` 中设置 `ubag.log.timer.task.push.enable = true`。
