# UAI 项目

UAI 是一个面向 Agent/LLM 的服务集合，包含 MCP 工具管理、会话记忆、SSE 流式输出及 Web 端调试能力。

## 模块说明

- `uai-mcp-server`：MCP Server，负责工具注册、工具调用与对话编排。
- `uai-mcp-base`：MCP 基础能力与数据库结构（DDL）。
- `uai-web-boot`：Spring Boot Web 容器能力（含 WebSocket/SSE 集成）。
- 其他模块（`uai-common`、`uai-util`、`uai-rag`、`uai-vec`、`uai-graph`）提供公共能力与扩展支撑。

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

## 启动建议

本地连接数据库启动（示例）：

```bash
mvn clean install jetty:run -Dspring.profiles.active=local
```

说明：配置读取依赖 `spring.profiles.active`，请确保对应 profile 的数据源与依赖配置正确。

## 开源前敏感信息配置

为避免密钥泄露，`uai-mcp-base/src/main/java/com/uni/uai/mcp/llm/ChatModelFactory.java` 已改为从环境变量或 JVM 参数读取 LLM 配置，不再在代码中写死 key。

必填配置（二选一）：

- 环境变量：`UAI_LLM_API_KEY`
- JVM 参数：`-Duai.llm.api-key=...`

可选配置（有默认值）：

- `UAI_LLM_BASE_URL` 或 `-Duai.llm.base-url=...`（默认 `https://openapi-ait.ke.com/v1`）
- `UAI_LLM_MODEL_NAME` 或 `-Duai.llm.model-name=...`（默认 `gpt-5-mini`）

启动示例：

```bash
export UAI_LLM_API_KEY="your_api_key"
export UAI_LLM_BASE_URL="https://openapi-ait.ke.com/v1"
export UAI_LLM_MODEL_NAME="gpt-5-mini"
mvn clean install jetty:run -Dspring.profiles.active=local
```

如果未配置 API Key，服务启动时会抛出 `IllegalStateException` 并提示配置项名称。

### 使用 `.env.example` 快速配置

项目根目录已提供 `.env.example`，可直接复制为本地私有配置：

```bash
cp .env.example .env
```

然后把 `.env` 中的 `UAI_LLM_API_KEY` 改成真实值。  
注意：`.env` 已被 `.gitignore` 忽略，不会被提交到仓库。

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
