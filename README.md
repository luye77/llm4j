# llm4j - Java AI SDK

一个参考 Spring AI 设计的轻量级 Java AI SDK，支持多种 AI 模型提供商。

## 特性

- 🚀 **统一接口**: 一致的 API 设计，轻松切换不同 AI 模型
- 🎯 **Spring AI 兼容**: 参考 Spring AI 的设计理念
- 🔄 **响应式支持**: 支持流式处理和响应式编程
- 🛠️ **工具调用**: 自动处理 Function Calling
- 💬 **对话记忆**: 内置对话历史管理
- 📚 **RAG 支持**: 检索增强生成功能
- 🌐 **多模型支持**: OpenAI、通义千问等

## 在 Spring Boot 项目中使用

### 1. 添加依赖

发布仓库为 **GitHub Packages（Maven）**，仓库地址：`https://maven.pkg.github.com/luye77/llm4j`。

**坐标**（版本以仓库 `pom.xml` 为准，示例为 `1.0.2`）：

| 说明 | 值 |
|------|-----|
| `groupId` | `io.github.luye77` |
| 核心库 `artifactId` | `llm4j` |
| Spring Starter `artifactId` | `llm4j-spring-boot-starter` |

Spring Boot 项目一般只需引入 **`llm4j-spring-boot-starter`**（已传递依赖核心 `llm4j`）。

#### Maven（`pom.xml`）

在 `<repositories>` 中声明 GitHub Packages（`<id>` 需与 `settings.xml` 中 `<server>` 的 `id` 一致，例如 `github`）：

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/luye77/llm4j</url>
        <releases><enabled>true</enabled></releases>
        <snapshots><enabled>true</enabled></snapshots>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>io.github.luye77</groupId>
        <artifactId>llm4j-spring-boot-starter</artifactId>
        <version>1.0.2</version>
    </dependency>
</dependencies>
```

在 **`~/.m2/settings.xml`**（或 CI 中注入的同名配置）中配置拉取凭证，**不要**把 Token 写进业务 `pom.xml`：

```xml
<settings>
    <servers>
        <server>
            <id>github</id>
            <username>你的 GitHub 用户名</username>
            <password>具有 read:packages 权限的 Personal Access Token</password>
        </server>
    </servers>
</settings>
```

#### Gradle（`build.gradle` / `build.gradle.kts`）

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/luye77/llm4j")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("io.github.luye77:llm4j-spring-boot-starter:1.0.2")
}
```

可在 `gradle.properties` 或环境变量中配置 `gpr.user` / `gpr.key`，**勿将 Token 提交到仓库**。

若依赖已发布到 **Maven Central**，通常只需在依赖中写坐标，**无需**再配置上述 GitHub 仓库（以实际发布渠道为准）。

---

### 2. 配置文件（`application.yml` / `application.properties`）

Starter 通过配置前缀绑定三类属性：

| 前缀 | 作用 |
|------|------|
| `ai.okhttp` | OkHttp：超时、日志级别、代理、是否忽略 SSL 等 |
| `ai.openai` | OpenAI 兼容：`apiHost`、`apiKey`、接口路径等 |
| `ai.qwen` | 通义千问：`apiHost`、`apiKey`、兼容模式及相关 URL 等 |

**`application.yml` 示例**：

```yaml
ai:
  okhttp:
    log: BASIC
    connect-timeout: 300
    read-timeout: 300
    write-timeout: 300
    time-unit: SECONDS
    proxy-url: ""
    proxy-port: 0
    ignore-ssl: true

  openai:
    api-host: "https://api.openai.com/"
    api-key: "${OPENAI_API_KEY:}"   # 建议用环境变量注入，勿将密钥提交到仓库
    chat-completion-url: "v1/chat/completions"
    embedding-url: "v1/embeddings"

  qwen:
    api-host: "https://dashscope.aliyuncs.com/"
    api-key: "${DASHSCOPE_API_KEY:}"
    chat-completion-url: "compatible-mode/v1/chat/completions"
    embedding-url: "compatible-mode/v1/embeddings"
    compatible-mode: true
```

---

### 3. 自动配置的 Bean

Starter 会注册：

| Bean 名称 | 类型 | 说明 |
|-----------|------|------|
| `llm4jConfiguration` | `com.bobo.llm4j.config.Configuration` | 全局配置与 OkHttp 等 |
| `openAiChatModel` | `com.bobo.llm4j.chat.model.ChatModel` | OpenAI 对话模型 |
| `qwenChatModel` | `com.bobo.llm4j.chat.model.ChatModel` | 通义千问对话模型 |

存在 **两个** `ChatModel` Bean，注入时必须使用 **`@Qualifier`**，不能单独使用 `@Autowired ChatModel`。

---

### 4. 业务代码示例

推荐使用 **`ChatClient`** 流式 API（`ChatClient` 不是 Starter 自动注册的 Bean，通过 `ChatClient.builder(chatModel)` 创建）：

```java
import com.bobo.llm4j.chat.client.ChatClient;
import com.bobo.llm4j.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class AiService {

    private final ChatClient chatClient;

    public AiService(@Qualifier("qwenChatModel") ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("你是一个有帮助的助手。")
                .build();
    }

    public String ask(String userMessage) throws Exception {
        return chatClient.prompt()
                .user(userMessage)
                .call()
                .content();
    }
}
```

使用 OpenAI 时，将 `@Qualifier("qwenChatModel")` 改为 `@Qualifier("openAiChatModel")`。

也可直接调用 `ChatModel#call(Prompt)`，或按需注入 `llm4jConfiguration` 使用底层 API。

---

### 5. 版本兼容说明

当前 `llm4j-spring-boot-starter` 依赖的 **Spring Boot 版本为 2.3.x**。若你的应用为 **Spring Boot 3.x**，请自行验证兼容性（如 Jakarta 命名空间等）。

## 快速开始

### 1. 创建 ChatModel

```java
import com.bobo.llm4j.chat.model.ChatModel;
import com.bobo.llm4j.platform.openai.chat.OpenAiChatModel;
import com.bobo.llm4j.config.Configuration;
import com.bobo.llm4j.config.OpenAiConfig;

// 配置
OpenAiConfig config = new OpenAiConfig();
config.setApiHost("https://api.openai.com/");
config.setApiKey("your-api-key");

Configuration configuration = new Configuration();
configuration.setOpenAiConfig(config);

// 创建 ChatModel
ChatModel chatModel = new OpenAiChatModel(configuration);
```

### 2. 同步调用

```java
import com.bobo.llm4j.chat.entity.Prompt;
import com.bobo.llm4j.chat.entity.Message;

Prompt prompt = Prompt.builder()
    .model("gpt-4o-mini")
    .message(Message.withUser("你好！"))
    .build();

ChatResponse response = chatModel.call(prompt);
String content = response.getGenerations().get(0)
    .getMessage().getContent().getText();
```

### 3. 流式调用

```java
Flux<ChatResponse> flux = chatModel.stream(prompt);
flux.subscribe(response -> {
    // 处理每个响应片段
});
```

## 支持的模型

- ✅ OpenAI (GPT-4, GPT-3.5等)
- 🔜 更多模型即将支持

## 项目结构

```
llm4j/
├── src/main/java/com/bobo/llm4j/
│   ├── chat/
│   │   ├── model/          # ChatModel 接口
│   │   ├── client/         # ChatClient 实现
│   │   ├── entity/         # 实体类
│   │   └── prompt/         # Prompt 相关
│   ├── platform/
│   │   ├── openai/         # OpenAI 实现
│   │   └── qwen/           # 通义千问实现
│   ├── rag/                # RAG 功能
│   ├── memory/             # 对话记忆
│   └── config/             # 配置类
└── src/test/               # 测试和示例
```


## 示例

查看 `src/test` 目录下的示例：

- `ChatClientTest.java` - ChatClient 使用示例
- `RagServiceTest.java` - RAG 功能示例
- `ChatMemoryUsageExample.java` - 对话记忆示例

## 贡献

欢迎提交 Issue 和 Pull Request！

## 联系方式

- **Email**: 1554612491@qq.com

## 许可证

本项目基于 Apache License 2.0 开源。

---

**注意**: 如果您正在使用旧版本的 `com.bobo.llm4j.service.ChatModel` 接口，请参考[迁移指南](docs/CHATMODEL_MIGRATION_GUIDE.md)进行升级。
