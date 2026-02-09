# 会话记忆功能重构总结

## 概述

本次重构参考 **Spring AI** 的设计理念，对 llm4j 项目的会话记忆功能进行了全面升级，采用了三层架构和 Advisor 模式，使代码更加模块化、可扩展和易于测试。

## 重构内容

### 1. 新增核心接口和类

#### 1.1 `ChatMemoryRepository` 接口（存储层）
- **文件位置**: `src/main/java/com/bobo/llm4j/memory/ChatMemoryRepository.java`
- **设计参考**: Spring AI 的 `ChatMemoryRepository`
- **职责**: 定义消息的底层存储接口
- **核心方法**:
  ```java
  List<String> findConversationIds();
  List<Message> findByConversationId(String conversationId);
  void saveAll(String conversationId, List<Message> messages);
  void deleteByConversationId(String conversationId);
  ```

#### 1.2 `InMemoryChatMemoryRepository` 实现（内存存储）
- **文件位置**: `src/main/java/com/bobo/llm4j/memory/InMemoryChatMemoryRepository.java`
- **设计参考**: Spring AI 的 `InMemoryChatMemoryRepository`
- **特性**:
  - 使用 `ConcurrentHashMap` 保证线程安全
  - 替换式保存（非追加式）
  - 参数严格校验，防止 null 值
  - 支持多会话隔离

#### 1.3 `MessageWindowChatMemory` 重构（滑动窗口策略）
- **文件位置**: `src/main/java/com/bobo/llm4j/memory/MessageWindowChatMemory.java`
- **设计参考**: Spring AI 的 `MessageWindowChatMemory`
- **核心改进**:
  1. **采用 Repository 模式**: 解耦存储逻辑
  2. **SystemMessage 特殊处理**:
     - 新增 SystemMessage 时自动移除旧的 SystemMessage
     - 窗口限制时优先保留 SystemMessage
  3. **滑动窗口策略**: 
     - 默认保留 20 条消息
     - 超限时删除最旧的非 SystemMessage
  4. **Builder 模式**: 支持灵活配置

**核心算法**:
```java
private List<Message> process(List<Message> memoryMessages, List<Message> newMessages) {
    // 1. 如果有新的 SystemMessage，移除所有旧的 SystemMessage
    // 2. 合并新旧消息
    // 3. 应用窗口限制（保留 SystemMessage，删除其他旧消息）
}
```

#### 1.4 `MessageChatMemoryAdvisor` 新增（Advisor 模式）
- **文件位置**: `src/main/java/com/bobo/llm4j/memory/MessageChatMemoryAdvisor.java`
- **设计参考**: Spring AI 的 `MessageChatMemoryAdvisor`
- **工作原理**:

**请求前处理 (before)**:
```java
1. 从 ChatMemory 获取会话历史
2. 合并历史消息和当前请求
3. 确保 SystemMessage 在第一位（符合 OpenAI 规范）
4. 保存当前用户消息
5. 返回处理后的请求
```

**响应后处理 (after)**:
```java
1. 提取 AI 的回复消息
2. 保存到 ChatMemory
3. 返回原始响应
```

**特性**:
- 支持自定义会话ID（通过上下文参数）
- 支持执行顺序配置
- Builder 模式构建
- 与 ChatClient 无缝集成

### 2. 更新的接口

#### 2.1 `ChatMemory` 接口增强
- **新增常量**:
  ```java
  String DEFAULT_CONVERSATION_ID = "default";
  String CONVERSATION_ID = "chat_memory_conversation_id";
  ```
- **新增方法**: `add(String conversationId, Message message)` - 单条消息添加
- **增强文档**: 详细的 JavaDoc 注释

### 3. 标记过时的类

#### 3.1 `ChatMemoryChatModel` 标记为 @Deprecated
- 推荐使用 `MessageChatMemoryAdvisor` 代替
- 保留用于向后兼容

#### 3.2 删除的类
- `InMemoryChatMemory` - 被新的 Repository 模式替代

## 架构设计

### 三层架构

```
┌─────────────────────────────────────────┐
│          Advisor 层                      │
│  MessageChatMemoryAdvisor               │
│  (拦截请求/响应，自动管理会话记忆)       │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│          ChatMemory 层                   │
│  MessageWindowChatMemory                │
│  (业务逻辑：窗口管理、SystemMessage处理)│
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│       ChatMemoryRepository 层            │
│  InMemoryChatMemoryRepository           │
│  (底层存储：ConcurrentHashMap)          │
└─────────────────────────────────────────┘
```

### 工作流程

```
用户发起请求
    ↓
【MessageChatMemoryAdvisor.before()】
    ├─ 1. 获取 conversationId
    ├─ 2. 从 ChatMemory 获取历史消息
    ├─ 3. 合并历史消息和当前请求
    ├─ 4. 调整消息顺序（SystemMessage 置顶）
    └─ 5. 保存当前用户消息
    ↓
【调用 LLM 模型】
    ↓
【MessageChatMemoryAdvisor.after()】
    ├─ 1. 提取 AI 回复
    └─ 2. 保存 AI 回复到 ChatMemory
    ↓
【MessageWindowChatMemory.add()】
    ├─ 1. 从 Repository 获取现有消息
    ├─ 2. 处理新旧消息合并
    ├─ 3. 应用滑动窗口策略
    └─ 4. 保存到 Repository
    ↓
【InMemoryChatMemoryRepository.saveAll()】
    └─ 存储到 ConcurrentHashMap
```

## 测试覆盖

### 测试文件

#### 1. `ChatMemoryRefactoredTest.java` - 基础功能测试
- **测试内容**:
  - Repository 层：保存、查找、删除、并发安全
  - ChatMemory 层：基础 CRUD 操作
  - MessageWindowChatMemory：窗口限制、SystemMessage 处理
  - Builder 模式测试
  - 边界条件测试
  - 实际使用场景测试

- **测试用例数**: 30+

#### 2. `MessageChatMemoryAdvisorTest.java` - Advisor 模式测试
- **测试内容**:
  - Advisor 基础功能（名称、顺序）
  - before() 方法：历史消息合并、SystemMessage 处理
  - after() 方法：AI 回复保存
  - 完整流程测试（多轮对话）
  - 会话ID处理（默认ID、上下文ID）
  - 边界情况测试

- **测试用例数**: 20+

### 测试覆盖的关键场景

1. ✅ **基础存储功能**: 保存、查找、删除
2. ✅ **窗口限制**: 超过最大消息数时正确删除旧消息
3. ✅ **SystemMessage 特殊处理**:
   - 新 SystemMessage 替换旧的
   - 窗口限制时 SystemMessage 优先保留
   - SystemMessage 始终在第一位
4. ✅ **多轮对话**: 历史消息正确累积
5. ✅ **并发安全**: 多线程访问测试
6. ✅ **会话隔离**: 不同会话ID的消息互不干扰
7. ✅ **Advisor 集成**: before/after 方法正确工作
8. ✅ **边界条件**: null 值、空列表、非法参数

## 使用示例

### 方式一：使用 MessageChatMemoryAdvisor（推荐）

```java
// 1. 创建 ChatMemory
ChatMemory chatMemory = MessageWindowChatMemory.builder()
    .maxMessages(10)
    .build();

// 2. 创建 Advisor
MessageChatMemoryAdvisor advisor = MessageChatMemoryAdvisor.builder(chatMemory)
    .conversationId("user-123")
    .build();

// 3. 配置 ChatClient（伪代码，需要集成到实际的 ChatClient）
ChatClient chatClient = ChatClient.builder(chatModel)
    .defaultAdvisors(advisor)
    .build();

// 4. 使用（会话记忆自动管理）
String response1 = chatClient.prompt()
    .user("我叫张三")
    .call()
    .content();

// 后续请求会自动包含历史上下文
String response2 = chatClient.prompt()
    .user("我叫什么名字？")
    .call()
    .content();
// 响应：您叫张三
```

### 方式二：直接使用 ChatMemory

```java
// 1. 创建 ChatMemory
ChatMemory chatMemory = MessageWindowChatMemory.builder()
    .maxMessages(10)
    .build();

String conversationId = "conv-001";

// 2. 手动管理会话
chatMemory.add(conversationId, Message.withUser("Hello"));
chatMemory.add(conversationId, Message.withAssistant("Hi there!"));

// 3. 获取历史
List<Message> history = chatMemory.get(conversationId);

// 4. 清空会话
chatMemory.clear(conversationId);
```

### 方式三：使用自定义 Repository

```java
// 实现自己的 Repository（例如：Redis、数据库）
public class RedisChatMemoryRepository implements ChatMemoryRepository {
    @Override
    public List<Message> findByConversationId(String conversationId) {
        // 从 Redis 获取
    }
    
    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        // 保存到 Redis
    }
    
    // ... 其他方法
}

// 使用自定义 Repository
ChatMemory chatMemory = MessageWindowChatMemory.builder()
    .chatMemoryRepository(new RedisChatMemoryRepository())
    .maxMessages(20)
    .build();
```

## 与 Spring AI 的对比

| 特性 | Spring AI | llm4j (重构后) |
|------|-----------|----------------|
| Repository 模式 | ✅ | ✅ |
| InMemory 存储 | ✅ | ✅ |
| 滑动窗口策略 | ✅ | ✅ |
| SystemMessage 特殊处理 | ✅ | ✅ |
| Advisor 模式 | ✅ | ✅ |
| Builder 模式 | ✅ | ✅ |
| 线程安全 | ✅ | ✅ |
| JDBC 支持 | ✅ | ❌ (可扩展) |
| Cassandra 支持 | ✅ | ❌ (可扩展) |
| Neo4j 支持 | ✅ | ❌ (可扩展) |

## 重构优势

### 1. **架构清晰**
- 分层设计，职责明确
- Repository 模式解耦存储逻辑
- 易于扩展新的存储实现

### 2. **符合规范**
- 参考 Spring AI 的成熟设计
- 遵循 OpenAI 消息格式规范
- SystemMessage 处理符合最佳实践

### 3. **易于使用**
- Advisor 模式自动管理会话记忆
- Builder 模式提供灵活配置
- 无需手动处理历史消息

### 4. **高度可测试**
- 接口清晰，易于 Mock
- 独立的单元测试
- 完整的集成测试

### 5. **可扩展性强**
- 支持自定义 Repository
- 支持自定义窗口策略
- 易于添加新功能

### 6. **线程安全**
- ConcurrentHashMap 保证并发安全
- 无需额外的同步控制

## 向后兼容

- ✅ 保留了 `ChatMemoryChatModel` 类（标记为 @Deprecated）
- ✅ `ChatMemory` 接口保持兼容
- ✅ 旧的测试可以继续运行

## 运行测试

### 运行所有会话记忆测试
```bash
mvn test -Dtest=ChatMemory*
```

### 运行特定测试
```bash
# 重构后的基础测试
mvn test -Dtest=ChatMemoryRefactoredTest

# Advisor 测试
mvn test -Dtest=MessageChatMemoryAdvisorTest

# 旧的测试（向后兼容）
mvn test -Dtest=ChatMemoryTest
```

## 后续改进建议

1. **持久化支持**:
   - 实现 `JdbcChatMemoryRepository`
   - 实现 `RedisChatMemoryRepository`

2. **ChatClient 集成**:
   - 完善 ChatClient 的 Advisor 链
   - 支持多个 Advisor 的组合使用

3. **功能增强**:
   - 支持消息过期时间（TTL）
   - 支持基于 Token 数量的窗口限制
   - 支持压缩/摘要策略

4. **监控和日志**:
   - 添加会话记忆使用统计
   - 添加性能监控

5. **文档完善**:
   - 添加更多使用示例
   - 添加集成指南

## 总结

本次重构成功将 llm4j 的会话记忆功能提升到与 Spring AI 相当的水平：

- ✅ **架构升级**: 从简单实现升级为三层架构
- ✅ **模式改进**: 引入 Repository 和 Advisor 模式
- ✅ **功能增强**: SystemMessage 特殊处理、滑动窗口优化
- ✅ **测试完善**: 50+ 测试用例，覆盖各种场景
- ✅ **代码质量**: 清晰的文档、参数校验、线程安全

重构后的代码更加健壮、易于维护和扩展，为后续功能开发奠定了坚实的基础。
