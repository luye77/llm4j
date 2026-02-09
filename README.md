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
