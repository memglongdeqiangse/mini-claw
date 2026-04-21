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
> **mini-claw**, your own openclaw.
> **mini-claw** provides multi-channel integration, tool invocation, scheduled task management, and session persistence, ready to use out of the box.
<!-- readme-gen:end:hook -->

<!-- readme-gen:start:features -->
<table>
<tr>
<td width="50%" valign="top">

### 🚀 Multi-Channel Support
Standard input/output, Feishu bot, Web API (HTTP + SSE) with one-click switching, unified message processing flow.

</td>
<td width="50%" valign="top">

### 🔧 Tool Invocation
Built-in file read/write, search, shell execution, date tools, supports custom Skill extensions.

</td>
</tr>
<tr>
<td width="50%" valign="top">

### ⏰ Scheduled Tasks
Supports periodic tasks, Cron expressions, one-time delayed tasks, powered by Quartz scheduler.

</td>
<td width="50%" valign="top">

### 💾 Session Persistence
JsonSession-based session storage and recovery, supports multiple model switching (OpenAI/Anthropic).

</td>
</tr>
</table>
<!-- readme-gen:end:features -->


## Quick Start

<details open>
<summary><strong>Requirements</strong></summary>

- Java 25+ (with preview features enabled)
- Maven 3.8+

</details>

<details open>
<summary><strong>Configuration</strong></summary>

Create a configuration file at `~/.mini-claw/config.json`:

```json
{
  "provider": {
    "apiKey": "${MC_API_KEY}",
    "apiType": "anthropic",
    "baseUrl": "${MC_BASE_URL}",
    "modelName": "glm-5.1"
  },
  "agent": [{
    "description": "You are a professional Feishu office assistant with the ability to directly operate the Feishu platform through the official lark-cli tool. You can understand user's natural language commands, automatically call appropriate Feishu CLI commands to complete tasks, and return results in a clear and understandable way.",
    "name": "lark cli agent",
    "prompt": "You can operate Feishu's following 12 major business domains: Calendar: view schedules, create meetings, check busy/free status, recommend best meeting times; Instant Messaging: send/reply messages, manage group chats, search message history, upload/download files; Cloud Documents: create, read, update documents (Markdown supported); add comments, search documents; Bitable: manage data tables, fields, records, views, dashboards, perform data aggregation analysis; Sheets: create, read, write, append, find and export spreadsheet data; Tasks: create, assign, update and complete tasks, manage subtasks and reminders; Mail: browse, search, read emails, send, reply, forward emails, manage drafts; Drive: upload/download files, manage sharing permissions, handle document comments; Wiki: manage knowledge spaces, nodes and document hierarchy; Contacts: search users by name/email/phone, get user details; Video Conferencing: search meeting records, get meeting notes and transcripts; Approval: query approval tasks, approve/reject/transfer approval tasks",
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

### Configuration Field Reference

#### Top-Level Fields

| Field | Type | Required | Description |
|------|------|------|------|
| `provider` | object | Yes | LLM provider configuration, see below |
| `agent` | array | No | Custom Agent list, see below |
| `workspace` | string | No | Workspace path, root directory for Agent file operations |
| `skillPath` | string[] | No | Global Skill search path list, Agents will load skills from these directories |
| `channels` | object | No | Channel configuration, key is channel name (`stdio`/`web`/`feishu`), see below |

#### provider (LLM Provider)

| Field | Type | Required | Description |
|------|------|------|------|
| `apiKey` | string | Yes | API key, supports environment variable substitution, e.g. `${MC_API_KEY}` |
| `apiType` | string | Yes | API type, options: `openai` (OpenAI-compatible), `anthropic` (Anthropic-compatible) |
| `baseUrl` | string | No | API base URL, for custom endpoints or proxies, e.g. `${MC_BASE_URL}` |
| `modelName` | string | Yes | Model name, e.g. `glm-5.1`, `claude-sonnet-4-6`, `qwen-max`, etc. |

#### agent (Custom Agent)

`agent` is an array, each element defines a sub-agent that the main Agent will call as a tool.

| Field | Type | Required | Description |
|------|------|------|------|
| `name` | string | Yes | Agent name, also the tool name, e.g. `lark cli agent` |
| `description` | string | Yes | Agent description, tells the main Agent when to use this sub-agent |
| `prompt` | string | Yes | Agent system prompt, defines the sub-agent's behavior and capability boundaries |
| `tools` | string[] | No | List of tools allowed for this Agent, e.g. `["readFile", "searchFiles"]` |
| `skillPath` | string[] | No | Agent-specific Skill search paths, merged with global `skillPath` |

#### channels (Channel Configuration)

All channels share the following base fields:

| Field | Type | Required | Default | Description |
|------|------|------|--------|------|
| `enabled` | boolean | No | `false` | Whether to enable this channel |
| `botPrefix` | string | No | `""` | Bot trigger prefix, messages must start with this to trigger response |
| `filterToolMessages` | boolean | No | `false` | Whether to filter tool call messages (not shown in output) |
| `filterThinking` | boolean | No | `false` | Whether to filter model thinking process |
| `allowFrom` | string[] | No | `[]` | User/group whitelist, empty means no restriction |
| `denyMessage` | string | No | `""` | Denial message when user is not in whitelist |

**stdio channel** — No additional fields

**web channel**

| Field | Type | Required | Default | Description |
|------|------|------|--------|------|
| `port` | int | No | `8080` | HTTP server port |
| `basePath` | string | No | `"/api"` | API base path prefix |
| `enableCors` | boolean | No | `true` | Whether to enable CORS support |
| `sseHeartbeatInterval` | int | No | `30` | SSE heartbeat interval (seconds), prevents connection timeout |

**feishu channel**

| Field | Type | Required | Default | Description |
|------|------|------|--------|------|
| `appId` | string | Yes | — | Feishu App ID |
| `appSecret` | string | Yes | — | Feishu App Secret |
| `botOpenId` | string | No | — | Bot Open ID, for identifying when @mentioned in groups |
| `markdownEnabled` | boolean | No | `true` | Whether to enable Markdown rendering (Feishu message format) |

> Configuration supports environment variable substitution, format: `${ENV_VAR}` or `${ENV_VAR:default}` (with default value).

</details>

<details open>
<summary><strong>Run</strong></summary>

**Option 1: Direct Run**

```bash
mvn compile exec:java -Dexec.mainClass="com.miniclaw.Main"
```

**Option 2: Package and Run**

```bash
mvn package
java --enable-preview -jar target/mini-claw-1.0-SNAPSHOT.jar
```

</details>

## Documentation
- [1.Agent Basics](docs/zh/1.Agent%E5%9F%BA%E6%9C%AC%E6%A6%82%E5%BF%B5.md)
- [2.Project Learning Guide](docs/zh/2.%E9%A1%B9%E7%9B%AE%E5%AD%A6%E4%B9%A0%E6%8C%87%E5%8D%97.md)

## Architecture

### Overall Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        User Layer                            │
│   ┌──────────┐    ┌──────────┐    ┌──────────────┐          │
│   │  CLI      │    │ Web UI   │    │ Feishu Client│          │
│   └────┬─────┘    └────┬─────┘    └──────┬───────┘          │
└───────│────────────────│─────────────────│──────────────────┘
        │                │                 │
        ↓                ↓                 ↓
┌─────────────────────────────────────────────────────────────┐
│                    Channel Layer                             │
│  ┌─────────────┐  ┌─────────────┐  ┌───────────────┐       │
│  │ StdChannel  │  │ WebChannel  │  │ FeishuChannel │       │
│  │   Std I/O   │  │ HTTP/SSE API│  │ Feishu WS     │       │
│  └──────┬──────┘  └──────┬──────┘  └───────┬───────┘       │
└───────│────────────────│─────────────────│─────────────────┘
        │                │                 │
        ↓                ↓                 ↓
┌─────────────────────────────────────────────────────────────┐
│                        Core Layer                            │
│   ┌────────────────────┐    ┌──────────────────┐            │
│   │ AgentMessageQueue  │    │  ChannelManager  │            │
│   │   Message Queue    │    │ Channel Manager  │            │
│   └───────┬────────────┘    └──────────────────┘            │
└───────────│─────────────────────────────────────────────────┘
            ↓
┌─────────────────────────────────────────────────────────────┐
│                       Agent Layer                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │ AgentRunner  │──│  ReActAgent  │──│ AutoContext  │       │
│  │    Runner    │  │ Reason+Exec   │  │    Memory    │       │
│  └──────┬───────┘  └──────┬───────┘  └──────────────┘       │
│         │                │                                   │
│         │         ┌──────┴───────┐                           │
│         │         │  JsonSession │                           │
│         │         │Persistence   │                           │
│         │         └──────────────┘                           │
└─────────│──────────│─────────────────────────────────────────┘
          │          │
          │          ├─────────────────────────────────────┐
          │          │                                     │
          │          ↓                                     ↓
┌─────────│──────────────────────────────────────────────────┐
│         │              Tool Layer (Toolkit)                 │
│         │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────┐ │
│         │  │FileIOTools│ │FileSearch│ │ShellTools│ │Cron  │ │
│         │  │File I/O   │ │File Search│ │Cmd Exec │ │Scheduler│ │
│         │  └──────────┘ └──────────┘ └──────────┘ └───┬──┘ │
│         │                                              │    │
└─────────│──────────────────────────────────────────────│────┘
          │                                              │
          │                                              ↓
┌─────────│──────────────────────────────────────────────────┐
│         │              Scheduler Layer                     │
│         │       ┌───────────────────┐    ┌───────────────┐│
│         │       │  SchedulerService │───→│ QuartzScheduler││
│         │       │ Scheduler Service │    │Quartz Scheduler││
│         │       └───────────────────┘    └───────┬───────┘│
└─────────│─────────────────────────────────────────│────────┘
          │                                         │
          │         ┌───────────────────────────────┘
          │         │
          ↓         ↓
┌─────────────────────────────────────────────────────────────┐
│                        Model Layer                           │
│            ┌──────────────────┐                              │
│            │      Model       │                              │
│            │     LLM Model    │                              │
│            └───────┬──────────┘                              │
│                    │                                         │
│          ┌─────────┴──────────┐                              │
│          ↓                    ↓                              │
│   ┌────────────┐       ┌──────────────┐                     │
│   │ OpenAI API │       │ Anthropic API│                     │
│   └────────────┘       └──────────────┘                     │
└─────────────────────────────────────────────────────────────┘
```

### Message Processing Flow

```
┌──────────┐
│   User   │
└────┬─────┘
     │ 1. Send message
     ↓
┌──────────────────┐
│    Channel       │ ← 2. Permission check (allowFrom)
│  (Channel Layer) │
└────┬─────────────┘
     │ 3. Create AgentRequest
     ↓
┌──────────────────┐
│ AgentMessageQueue│
│  (Message Queue) │
└────┬─────────────┘
     │ 4. takeRequest()
     ↓
┌──────────────────┐
│   AgentRunner    │ ← 5. Load session (JsonSession)
│     (Runner)     │ ← 6. Create/Restore Agent
└────┬─────────────┘
     │
     ↓
┌──────────────────────────────────────────────────────┐
│                    ReActAgent                        │
│                                                      │
│  ┌────────────────────────────────────────────────┐ │
│  │           Reasoning-Execution Loop              │ │
│  │                                                 │ │
│  │  7. Call LLM Model ──→ 8. Return response       │ │
│  │         │                                       │ │
│  │         ├─→ [Tool call needed]                  │ │
│  │         │        ↓                              │ │
│  │         │    9. Execute Tools/Skills            │ │
│  │         │        ↓                              │ │
│  │         └─← 10. Tool returned result            │ │
│  │                                                 │ │
│  └────────────────────────────────────────────────┘ │
└────┬─────────────────────────────────────────────────┘
     │ 11. Final response
     ↓
┌──────────────────┐
│   AgentRunner    │ ← 12. Save session
└────┬─────────────┘
     │ 13. AgentResponse
     ↓
┌──────────────────┐
│ AgentMessageQueue│
└────┬─────────────┘
     │ 14. Dispatch to corresponding channel
     ↓
┌──────────────────┐
│    Channel       │
└────┬─────────────┘
     │ 15. Send to user
     ↓
┌──────────┐
│   User   │
└──────────┘
```

### Scheduled Task Architecture

```
┌─────────────────────────────────────────────┐
│              Trigger Methods                 │
│  ┌────────────┐ ┌────────────┐ ┌──────────┐│
│  │  Periodic  │ │    Cron    │ │ One-time ││
│  │every_seconds│ │ cron_expr  │ │ at/delay ││
│  └─────┬──────┘ └─────┬──────┘ └─────┬─────┘│
└────────│──────────────│──────────────│───────┘
         │              │              │
         └──────────────┼──────────────┘
                        ↓
         ┌──────────────────────────┐
         │  QuartzAgentScheduler    │
         │       (Scheduler)        │
         └────────────┬─────────────┘
                      │
                      ↓
┌─────────────────────────────────────────────┐
│                Execution                     │
│  ┌──────────────────────────┐              │
│  │  ScheduleAgentTask       │              │
│  └────────────┬─────────────┘              │
│               ↓                             │
│  ┌──────────────────────────┐              │
│  │  Independent Agent       │              │
│  └────────────┬─────────────┘              │
│               ↓                             │
│  ┌──────────────────────────┐              │
│  │  DeliverToUserHook       │              │
│  │  (Result Delivery Hook)  │              │
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
     │       User       │
     └──────────────────┘
```

### Channel Extension Mechanism

```
┌─────────────────────────────────────────────────────────────┐
│                     <<abstract>>                            │
│                      BaseChannel                            │
├─────────────────────────────────────────────────────────────┤
│  + ChannelType channelType                                  │
│  + AgentMessageQueue agentMessageQueue                      │
│  + Set allowFrom                                            │
├─────────────────────────────────────────────────────────────┤
│  + process(userId, sessionId, text)                         │
│  + isAllowed(userId): boolean                               │
│  + send(response): CompletableFuture                        │
│  + start()                                                  │
│  + stop()                                                   │
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

## Channel Configuration

### Stdio Channel

Standard input/output, suitable for command-line interaction.

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

### Web Channel

Provides HTTP REST API and SSE push.

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

**API Endpoints**:

- `POST /api/chat` - Send message, return complete response
- `POST /api/chat/stream` - Send message, stream response
- `GET /api/sse/{sessionId}` - SSE connection, receive pushed messages
- `GET /api/health` - Health check

### Feishu Channel

Feishu bot, supports private chat and group chat (@bot to trigger).

```json
{
  "channels": {
    "feishu": {
      "appId": "${FEISHU_APP_ID}",
      "appSecret": "${FEISHU_APP_SECRET}",
      "botOpenId": "ou_xxx",
      "allowFrom": ["ou_xxx"],
      "denyMessage": "Sorry, you don't have permission to use this bot"
    }
  }
}
```

## Tools

mini-claw has the following built-in tools:

| Tool | Function |
|------|----------|
| `readFile` | Read file, supports line range |
| `writeFile` | Create or overwrite file |
| `editFile` | Find and replace file content |
| `appendFile` | Append content to file |
| `searchFiles` | Search file content |
| `listFiles` | List directory files |
| `executeShell` | Execute Shell command |
| `currentDate` | Get current date time |
| `cron` | Manage scheduled tasks |


## Custom Agent Support

You can add custom Agents in the configuration:

```json
{
  "agent": [
    {
      "name": "CodeReviewer",
      "description": "Code review assistant",
      "prompt": "You are a professional code review assistant...",
      "tools": ["readFile", "searchFiles"],
      "skillPath": ["~/skills/code-review"]
    }
  ]
}
```

<!-- readme-gen:start:tree -->
## Project Structure

```
📦 mini-claw
├── 📂 src/main/java/com/miniclaw/
│   ├── 📄 Main.java                    # Entry point
│   ├── 📂 runner/
│   │   └── 📄 AgentRunner.java         # Agent runner
│   ├── 📂 channel/
│   │   ├── 📄 BaseChannel.java         # Channel base class
│   │   ├── 📄 ChannelType.java         # Channel type enum
│   │   ├── 📂 stdio/StdChannel.java    # Stdio channel
│   │   ├── 📂 web/WebChannel.java      # Web channel
│   │   └── 📂 feishu/FeishuChannel.java # Feishu channel
│   ├── 📂 tool/
│   │   ├── 📄 ToolManager.java         # Tool manager
│   │   ├── 📂 file/FileIOTools.java    # File tools
│   │   ├── 📂 shell/ShellTools.java    # Shell tools
│   │   └── 📂 cron/CornTools.java      # Scheduled task tools
│   ├── 📂 cron/
│   │   └── 📂 schedule/
│   │       └── 📄 SchedulerService.java # Scheduler service
│   ├── 📂 config/
│   │   ├── 📄 Config.java              # Config class
│   │   ├── 📄 ConfigLoader.java        # Config loader
│   │   └── 📄 CustomerAgentConfig.java # Custom Agent config
│   ├── 📂 skill/
│   │   └── 📄 SkillManager.java        # Skill manager
│   └── 📂 bus/
│       └── 📄 AgentMessageQueue.java   # Message queue
├── 📄 pom.xml
└── 📄 README.md
```
<!-- readme-gen:end:tree -->



## Tech Stack

- **agentscope-java** - Agent framework core
- **Spring WebFlux** - Web channel server
- **Reactor Netty** - HTTP server
- **Quartz** - Scheduled task management
- **Lark SDK** - Feishu bot integration
- **Jackson** - JSON serialization
- **Lombok** - Code simplification


## Contributing

Contributions, bug reports, and suggestions are welcome!

## Disclaimer

**Note: This project has not undergone production-level testing and is intended for learning and research purposes only!**

## License

This project is licensed under the MIT License - see [LICENSE](LICENSE) for details

<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=gradient&customColorList=0,1&height=100&section=footer" width="100%" />

**Built with ❤️ by [memglongdeqiangse](https://github.com/memglongdeqiangse)**

</div>
