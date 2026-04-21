<p align="center">
  <img src="https://img.shields.io/badge/Java-25-orange?style=flat-square&logo=openjdk&logoColor=white" alt="Java" />
  <img src="https://img.shields.io/badge/Spring-WebFlux-6DB33F?style=flat-square&logo=spring&logoColor=white" alt="Spring WebFlux" />
  <img src="https://img.shields.io/badge/Quartz-Scheduler-FF6B35?style=flat-square&logo=quartz&logoColor=white" alt="Quartz" />
  <img src="https://img.shields.io/badge/Version-1.0--SNAPSHOT-blue?style=flat-square" alt="Version" />
  <img src="https://img.shields.io/badge/License-MIT-green?style=flat-square" alt="License" />
</p>



<p align="center">
  <img src="https://img.shields.io/github/last-commit/memglongdeqiangse/mini-claw?style=flat-square&logo=github&logoColor=white" alt="Last Commit" />
  <img src="https://img.shields.io/github/stars/memglongdeqiangse/mini-claw?style=flat-square&logo=github" alt="Stars" />
  <img src="https://img.shields.io/github/forks/memglongdeqiangse/mini-claw?style=flat-square&logo=github" alt="Forks" />
  <img src="https://img.shields.io/github/issues/memglongdeqiangse/mini-claw?style=flat-square&logo=github" alt="Issues" />
</p>

---

<!-- readme-gen:start:hook -->
> **mini-claw**，你自己的openclaw。
> **mini-claw** 提供多渠道接入、工具调用、定时任务调度和会话持久化，开箱即用。
<!-- readme-gen:end:hook -->

<!-- readme-gen:start:features -->
<table>
<tr>
<td width="50%" valign="top">

### 🚀 多渠道支持
标准输入输出、飞书机器人、Web API (HTTP + SSE) 一键切换,统一的消息处理流程。

</td>
<td width="50%" valign="top">

### 🔧 工具调用
内置文件读写、搜索、Shell 执行、日期工具,支持自定义 Skill 扩展。

</td>
</tr>
<tr>
<td width="50%" valign="top">

### ⏰ 定时任务
支持周期任务、Cron 表达式、一次性延迟任务,基于 Quartz 调度器实现。

</td>
<td width="50%" valign="top">

### 💾 会话持久化
基于 JsonSession 的会话存储与恢复,支持多模型切换 (OpenAI/Anthropic)。

</td>
</tr>
</table>
<!-- readme-gen:end:features -->


## 快速开始

<details open>
<summary><strong>环境要求</strong></summary>

- Java 25+ (启用 preview 特性)
- Maven 3.8+

</details>

<details open>
<summary><strong>配置文件</strong></summary>

在 `~/.mini-claw/config.json` 创建配置文件:

```json
{
  "provider": {
    "apiKey": "${MC_API_KEY}",
    "apiType": "anthropic",
    "baseUrl": "${MC_BASE_URL}",
    "modelName": "glm-5.1"
  },
  "agent": [{
    "description": "你是一个专业的飞书办公助手，拥有通过官方 lark-cli 工具直接操作飞书平台的能力。你可以理解用户的自然语言指令，自动调用合适的飞书 CLI 命令完成任务，并以清晰易懂的方式返回结果。",
    "name": "lark cli agent",
    "prompt": "你可以操作飞书以下 12 大业务域的功能：日历：查看日程、创建会议、查询忙闲状态、推荐最佳会议时间、即时通讯：发送 / 回复消息、管理群聊、搜索消息历史、上传下载文件；云文档：创建、读取、更新文档（支持 Markdown）；添加评论、搜索文档；多维表格：管理数据表、字段、记录、视图、仪表盘，进行数据聚合分析；电子表格：创建、读取、写入、追加、查找和导出表格数据；任务：创建、分配、更新和完成任务，管理子任务和提醒；邮箱：浏览、搜索、阅读邮件，发送、回复、转发邮件，管理草稿；云空间：上传下载文件、管理分享权限、处理文档评论；知识库：管理知识空间、节点和文档层级；通讯录：按姓名 / 邮箱 / 手机号搜索用户，获取用户详细信息；视频会议：搜索会议记录、获取会议纪要和逐字稿；审批：查询审批任务、同意 / 拒绝 / 转交审批任务",
    "skillPath": ["~/.agents/skills/"]
  }],
  "workspace": "~/program/workspace-6/",
  "skillPath": ["/Users/leilei/program/workspace-5/skills"],
  "channels": {
    "feishu":{
      "appId": "${MC_FEISHU_APP_ID}",
      "appSecret": "${MC_FEISHU_SECRET}",
      "botOpenId":"${MC_BOT_OPEN_ID}",
      "enabled": true
    },
    "web":{
      "enabled": true,
      "port": 8080,
      "basePath": "/api",
      "enableCors": true,
      "sseHeartbeatInterval": 30
    }
  }
}

```

### 配置字段说明

#### 顶层字段

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `provider` | object | 是 | LLM 提供者配置，详见下方 |
| `agent` | array | 否 | 自定义 Agent 列表，详见下方 |
| `workspace` | string | 否 | 工作空间路径，Agent 读写文件时的根目录 |
| `skillPath` | string[] | 否 | 全局 Skill 搜索路径列表，Agent 会从这些目录加载技能 |
| `channels` | object | 否 | 渠道配置，key 为渠道名（`stdio`/`web`/`feishu`），详见下方 |

#### provider（LLM 提供者）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `apiKey` | string | 是 | API 密钥，支持环境变量替换，如 `${MC_API_KEY}` |
| `apiType` | string | 是 | API 类型，可选值：`openai`（兼容 OpenAI 接口）、`anthropic`（兼容 Anthropic 接口） |
| `baseUrl` | string | 否 | API 基础地址，用于自定义端点或代理，如 `${MC_BASE_URL}` |
| `modelName` | string | 是 | 模型名称，如 `glm-5.1`、`claude-sonnet-4-6`、`qwen-max` 等 |

#### agent（自定义 Agent）

`agent` 是一个数组，每个元素定义一个子智能体，主 Agent 会将其作为工具调用。

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | string | 是 | Agent 名称，同时也是工具名，如 `lark cli agent` |
| `description` | string | 是 | Agent 描述，告诉主 Agent 何时使用此子智能体 |
| `prompt` | string | 是 | Agent 系统提示词，定义子智能体的行为和能力边界 |
| `tools` | string[] | 否 | 允许该 Agent 使用的工具列表，如 `["readFile", "searchFiles"]` |
| `skillPath` | string[] | 否 | 该 Agent 专属的 Skill 搜索路径，会与全局 `skillPath` 合并 |

#### channels（渠道配置）

所有渠道共享以下基础字段：

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `enabled` | boolean | 否 | `false` | 是否启用该渠道 |
| `botPrefix` | string | 否 | `""` | 机器人触发前缀，消息需以此开头才响应 |
| `filterToolMessages` | boolean | 否 | `false` | 是否过滤工具调用消息（不在输出中展示） |
| `filterThinking` | boolean | 否 | `false` | 是否过滤模型思考过程 |
| `allowFrom` | string[] | 否 | `[]` | 允许的用户/群组白名单，为空则不限制 |
| `denyMessage` | string | 否 | `""` | 用户不在白名单时的拒绝提示 |

**stdio 渠道** — 无额外字段

**web 渠道**

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `port` | int | 否 | `8080` | HTTP 服务端口 |
| `basePath` | string | 否 | `"/api"` | API 基础路径前缀 |
| `enableCors` | boolean | 否 | `true` | 是否启用跨域请求支持 |
| `sseHeartbeatInterval` | int | 否 | `30` | SSE 心跳间隔（秒），防止连接超时 |

**feishu 渠道**

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `appId` | string | 是 | — | 飞书应用 App ID |
| `appSecret` | string | 是 | — | 飞书应用密钥 |
| `botOpenId` | string | 否 | — | 机器人 Open ID，用于群聊中被 @时识别 |
| `markdownEnabled` | boolean | 否 | `true` | 是否启用 Markdown 渲染（飞书消息格式） |

> 配置支持环境变量替换，格式为 `${ENV_VAR}` 或 `${ENV_VAR:default}`（带默认值）。

</details>

<details open>
<summary><strong>运行方式</strong></summary>

**方式一: 直接运行**

```bash
mvn compile exec:java -Dexec.mainClass="com.miniclaw.Main"
```

**方式二: 打包运行**

```bash
mvn package
java --enable-preview -jar target/mini-claw-1.0-SNAPSHOT.jar
```

</details>

## 帮助文档
- [1.Agent基本概念.md](docs/zh/1.Agent%E5%9F%BA%E6%9C%AC%E6%A6%82%E5%BF%B5.md)
- [2.项目学习指南.md](docs/zh/2.%E9%A1%B9%E7%9B%AE%E5%AD%A6%E4%B9%A0%E6%8C%87%E5%8D%97.md)

## 架构设计

### 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                        用户层                                │
│   ┌──────────┐    ┌──────────┐    ┌──────────────┐         │
│   │  命令行   │    │ Web前端  │    │  飞书客户端   │         │
│   └────┬─────┘    └────┬─────┘    └──────┬───────┘         │
└───────│────────────────│─────────────────│─────────────────┘
        │                │                 │
        ↓                ↓                 ↓
┌─────────────────────────────────────────────────────────────┐
│                    渠道层 (Channel)                          │
│  ┌─────────────┐  ┌─────────────┐  ┌───────────────┐       │
│  │ StdChannel  │  │ WebChannel  │  │ FeishuChannel │       │
│  │ 标准输入输出  │  │ HTTP/SSE API│  │ 飞书WebSocket  │       │
│  └──────┬──────┘  └──────┬──────┘  └───────┬───────┘       │
└───────│────────────────│─────────────────│─────────────────┘
        │                │                 │
        ↓                ↓                 ↓
┌─────────────────────────────────────────────────────────────┐
│                        核心层                                │
│   ┌────────────────────┐    ┌──────────────────┐           │
│   │ AgentMessageQueue  │    │  ChannelManager  │           │
│   │     消息队列        │    │    渠道管理器     │           │
│   └───────┬────────────┘    └──────────────────┘           │
└───────────│─────────────────────────────────────────────────┘
            ↓
┌─────────────────────────────────────────────────────────────┐
│                       Agent 层                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ AgentRunner  │──│  ReActAgent  │──│ AutoContext  │      │
│  │    运行器     │  │ 推理执行Agent │  │    Memory    │      │
│  └──────┬───────┘  └──────┬───────┘  └──────────────┘      │
│         │                │                                   │
│         │         ┌──────┴───────┐                          │
│         │         │  JsonSession │                          │
│         │         │  会话持久化    │                          │
│         │         └──────────────┘                          │
└─────────│──────────│────────────────────────────────────────┘
          │          │
          │          ├─────────────────────────────────────┐
          │          │                                     │
          │          ↓                                     ↓
┌─────────│──────────────────────────────────────────────────┐
│         │              工具层 (Toolkit)                     │
│         │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────┐│
│         │  │FileIOTools│ │FileSearch│ │ShellTools│ │Cron ││
│         │  │  文件读写  │ │  文件搜索 │ │ 命令执行  │ │定时任务││
│         │  └──────────┘ └──────────┘ └──────────┘ └───┬──┘│
│         │                                              │   │
└─────────│──────────────────────────────────────────────│───┘
          │                                              │
          │                                              ↓
┌─────────│──────────────────────────────────────────────────┐
│         │                  调度层                           │
│         │       ┌───────────────────┐    ┌───────────────┐│
│         │       │  SchedulerService │───→│ QuartzScheduler││
│         │       │     调度服务       │    │   Quartz调度器 ││
│         │       └───────────────────┘    └───────┬───────┘│
└─────────│─────────────────────────────────────────│────────┘
          │                                         │
          │         ┌───────────────────────────────┘
          │         │
          ↓         ↓
┌─────────────────────────────────────────────────────────────┐
│                        模型层                                │
│            ┌──────────────────┐                             │
│            │      Model       │                             │
│            │     LLM 模型     │                             │
│            └───────┬──────────┘                             │
│                    │                                        │
│          ┌─────────┴──────────┐                             │
│          ↓                    ↓                             │
│   ┌────────────┐       ┌──────────────┐                    │
│   │ OpenAI API │       │ Anthropic API│                    │
│   └────────────┘       └──────────────┘                    │
└─────────────────────────────────────────────────────────────┘
```

### 消息处理流程

```
┌──────────┐
│   用户    │
└────┬─────┘
     │ 1. 发送消息
     ↓
┌──────────────────┐
│    Channel       │ ← 2. 权限校验(allowFrom)
│  (渠道层)         │
└────┬─────────────┘
     │ 3. 创建 AgentRequest
     ↓
┌──────────────────┐
│ AgentMessageQueue│
│    (消息队列)     │
└────┬─────────────┘
     │ 4. takeRequest()
     ↓
┌──────────────────┐
│   AgentRunner    │ ← 5. 加载会话 (JsonSession)
│    (运行器)       │ ← 6. 创建/恢复 Agent
└────┬─────────────┘
     │
     ↓
┌──────────────────────────────────────────────────────┐
│                    ReActAgent                        │
│                                                      │
│  ┌────────────────────────────────────────────────┐ │
│  │           推理-执行循环 (Loop)                   │ │
│  │                                                 │ │
│  │  7. 调用 LLM Model ──→ 8. 返回响应              │ │
│  │         │                                       │ │
│  │         ├─→ [需要工具调用]                       │ │
│  │         │        ↓                              │ │
│  │         │    9. 执行 Tools/Skills               │ │
│  │         │        ↓                              │ │
│  │         └─← 10. 工具返回结果                     │ │
│  │                                                 │ │
│  └────────────────────────────────────────────────┘ │
└────┬─────────────────────────────────────────────────┘
     │ 11. 最终响应
     ↓
┌──────────────────┐
│   AgentRunner    │ ← 12. 保存会话
└────┬─────────────┘
     │ 13. AgentResponse
     ↓
┌──────────────────┐
│ AgentMessageQueue│
└────┬─────────────┘
     │ 14. 分发给对应渠道
     ↓
┌──────────────────┐
│    Channel       │
└────┬─────────────┘
     │ 15. 发送给用户
     ↓
┌──────────┐
│   用户    │
└──────────┘
```

### 定时任务架构

```
┌─────────────────────────────────────────────┐
│              触发方式                        │
│  ┌────────────┐ ┌────────────┐ ┌──────────┐│
│  │  周期任务   │ │ Cron表达式  │ │ 一次性任务 ││
│  │every_seconds│ │ cron_expr  │ │at/delay   ││
│  └─────┬──────┘ └─────┬──────┘ └─────┬─────┘│
└────────│──────────────│──────────────│───────┘
         │              │              │
         └──────────────┼──────────────┘
                        ↓
         ┌──────────────────────────┐
         │  QuartzAgentScheduler    │
         │      (调度器)             │
         └────────────┬─────────────┘
                      │
                      ↓
┌─────────────────────────────────────────────┐
│                 执行                         │
│  ┌──────────────────────────┐              │
│  │  ScheduleAgentTask       │              │
│  └────────────┬─────────────┘              │
│               ↓                             │
│  ┌──────────────────────────┐              │
│  │  独立 Agent 实例          │              │
│  └────────────┬─────────────┘              │
│               ↓                             │
│  ┌──────────────────────────┐              │
│  │  DeliverToUserHook       │              │
│  │  (结果投递 Hook)          │              │
│  └────────────┬─────────────┘              │
└───────────────│─────────────────────────────┘
                │ deliver=true
                ↓
     ┌──────────────────┐
     │  MessageQueue    │
     └────────┬─────────┘
              ↓
     ┌──────────────────┐
     │     Channel      │
     └────────┬─────────┘
              ↓
     ┌──────────────────┐
     │       用户       │
     └──────────────────┘
```

### 渠道扩展机制

```
┌─────────────────────────────────────────────────────────────┐
│                     <<abstract>>                            │
│                      BaseChannel                             │
├─────────────────────────────────────────────────────────────┤
│  + ChannelType channelType                                   │
│  + AgentMessageQueue agentMessageQueue                       │
│  + Set allowFrom                                             │
├─────────────────────────────────────────────────────────────┤
│  + process(userId, sessionId, text)                          │
│  + isAllowed(userId): boolean                                │
│  + send(response): CompletableFuture                         │
│  + start()                                                   │
│  + stop()                                                    │
└─────────────────────┬───────────────────────────────────────┘
                      │
        ┌─────────────┼─────────────┐
        │             │             │
        ↓             ↓             ↓
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ StdChannel   │ │ WebChannel   │ │FeishuChannel │
├──────────────┤ ├──────────────┤ ├──────────────┤
│              │ │-config       │ │-config       │
│              │ │-sseManager   │ │-client       │
│              │ │-server       │ │-eventHandler │
├──────────────┤ ├──────────────┤ ├──────────────┤
│+start()      │ │+start()      │ │+start()      │
│+stop()       │ │+stop()       │ │+stop()       │
│+send()       │ │+send()       │ │+send()       │
│+isAllowed()  │ │              │ │+sendToUser() │
└──────────────┘ └──────────────┘ └──────────────┘
```

## 渠道配置

### Stdio 渠道

标准输入输出,适合命令行交互。

```json
{
  "channels": {
    "stdio": {
      "filterToolMessages": false,
      "filterThinking": true,
      "allowFrom": []
    }
  }
}
```

### Web 渠道

提供 HTTP REST API 和 SSE 推送。

```json
{
  "channels": {
    "web": {
      "port": 8080,
      "basePath": "/api",
      "sseHeartbeatInterval": 30,
      "allowFrom": []
    }
  }
}
```

**API 接口**:

- `POST /api/chat` - 发送消息,返回完整响应
- `POST /api/chat/stream` - 发送消息,流式返回
- `GET /api/sse/{sessionId}` - SSE 连接,接收推送消息
- `GET /api/health` - 健康检查

### 飞书渠道

飞书机器人,支持私聊和群聊 (@机器人触发)。

```json
{
  "channels": {
    "feishu": {
      "appId": "${FEISHU_APP_ID}",
      "appSecret": "${FEISHU_APP_SECRET}",
      "botOpenId": "ou_xxx",
      "allowFrom": ["ou_xxx"],
      "denyMessage": "抱歉,您没有权限使用此机器人"
    }
  }
}
```

## 工具

mini-claw 内置以下工具:

| 工具 | 功能 |
|------|------|
| `readFile` | 读取文件,支持行范围 |
| `writeFile` | 创建或覆盖文件 |
| `editFile` | 查找替换文件内容 |
| `appendFile` | 追加内容到文件 |
| `searchFiles` | 搜索文件内容 |
| `listFiles` | 列出目录文件 |
| `executeShell` | 执行 Shell 命令 |
| `currentDate` | 获取当前日期时间 |
| `cron` | 管理定时任务 |


## 支持自定义 Agent

可以在配置中添加自定义 Agent:

```json
{
  "agent": [
    {
      "name": "CodeReviewer",
      "description": "代码审查助手",
      "prompt": "你是一个专业的代码审查助手...",
      "tools": ["readFile", "searchFiles"],
      "skillPath": ["~/skills/code-review"]
    }
  ]
}
```

<!-- readme-gen:start:tree -->
## 项目结构

```
📦 mini-claw
├── 📂 src/main/java/com/miniclaw/
│   ├── 📄 Main.java                    # 入口
│   ├── 📂 runner/
│   │   └── 📄 AgentRunner.java         # Agent 运行器
│   ├── 📂 channel/
│   │   ├── 📄 BaseChannel.java         # 渠道基类
│   │   ├── 📄 ChannelType.java         # 渠道类型枚举
│   │   ├── 📂 stdio/StdChannel.java    # Stdio 渠道
│   │   ├── 📂 web/WebChannel.java      # Web 渠道
│   │   └── 📂 feishu/FeishuChannel.java # 飞书渠道
│   ├── 📂 tool/
│   │   ├── 📄 ToolManager.java         # 工具管理
│   │   ├── 📂 file/FileIOTools.java    # 文件工具
│   │   ├── 📂 shell/ShellTools.java    # Shell 工具
│   │   └── 📂 cron/CornTools.java      # 定时任务工具
│   ├── 📂 cron/
│   │   └── 📂 schedule/
│   │       └── 📄 SchedulerService.java # 调度服务
│   ├── 📂 config/
│   │   ├── 📄 Config.java              # 配置类
│   │   ├── 📄 ConfigLoader.java        # 配置加载
│   │   └── 📄 CustomerAgentConfig.java # 自定义 Agent 配置
│   ├── 📂 skill/
│   │   └── 📄 SkillManager.java        # Skill 管理
│   └── 📂 bus/
│       └── 📄 AgentMessageQueue.java   # 消息队列
├── 📄 pom.xml
└── 📄 README.md
```
<!-- readme-gen:end:tree -->



## 技术栈

- **agentscope-java** - Agent 框架核心
- **Spring WebFlux** - Web 渠道服务器
- **Reactor Netty** - HTTP 服务器
- **Quartz** - 定时任务调度
- **Lark SDK** - 飞书机器人集成
- **Jackson** - JSON 序列化
- **Lombok** - 代码简化


## 贡献指南

欢迎贡献代码、报告问题或提出建议!

## 其他

**注意：该项目未进行生产级的测试，仅用于学习和研究目的！**

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=gradient&customColorList=0,1&height=100&section=footer" width="100%" />

**Built with ❤️ by [memglongdeqiangse](https://github.com/memglongdeqiangse)**

</div>
