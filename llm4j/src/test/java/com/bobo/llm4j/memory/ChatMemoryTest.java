package com.bobo.llm4j.memory;

import com.bobo.llm4j.listener.StreamingResponseHandler;
import com.bobo.llm4j.platform.openai.chat.entity.ChatResponse;
import com.bobo.llm4j.platform.openai.chat.entity.Generation;
import com.bobo.llm4j.platform.openai.chat.entity.Message;
import com.bobo.llm4j.platform.openai.chat.entity.Prompt;
import com.bobo.llm4j.platform.openai.chat.enums.MessageType;
import com.bobo.llm4j.service.ChatModel;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 会话记忆功能测试
 */
public class ChatMemoryTest {

    @Test
    public void testInMemoryChatMemoryAddGetClear() {
        ChatMemory memory = new InMemoryChatMemory();
        String conversationId = "c1";

        memory.add(conversationId, Arrays.asList(
                Message.withUser("hello"),
                Message.withAssistant("hi")
        ));

        List<Message> messages = memory.get(conversationId);
        Assert.assertEquals(2, messages.size());

        memory.clear(conversationId);
        Assert.assertTrue(memory.get(conversationId).isEmpty());
    }

    @Test
    public void testMessageWindowChatMemoryLimit() {
        MessageWindowChatMemory memory = new MessageWindowChatMemory(3);
        String conversationId = "c2";

        memory.add(conversationId, Arrays.asList(
                Message.withUser("1"),
                Message.withUser("2"),
                Message.withUser("3")
        ));
        memory.add(conversationId, Collections.singletonList(Message.withUser("4")));

        List<Message> messages = memory.get(conversationId);
        Assert.assertEquals(3, messages.size());
        Assert.assertEquals("2", messages.get(0).getContent().getText());
        Assert.assertEquals("4", messages.get(2).getContent().getText());
    }

    @Test
    public void testChatMemoryChatModelStoresMessages() throws Exception {
        RecordingChatModel delegate = new RecordingChatModel();
        ChatMemory memory = new InMemoryChatMemory();
        ChatModel chatModel = new ChatMemoryChatModel(delegate, memory, "c3");

        Prompt firstPrompt = Prompt.builder()
                .model("test-model")
                .message(Message.withUser("你好"))
                .build();

        chatModel.call(firstPrompt);

        List<Message> storedFirst = memory.get("c3");
        Assert.assertEquals(2, storedFirst.size());
        Assert.assertEquals(MessageType.USER.getRole(), storedFirst.get(0).getRole());
        Assert.assertEquals(MessageType.ASSISTANT.getRole(), storedFirst.get(1).getRole());

        Prompt secondPrompt = Prompt.builder()
                .model("test-model")
                .message(Message.withUser("你是谁"))
                .build();

        chatModel.call(secondPrompt);

        List<Message> storedSecond = memory.get("c3");
        Assert.assertEquals(4, storedSecond.size());
        Assert.assertEquals(3, delegate.getLastPrompt().getMessages().size());
    }

    private static class RecordingChatModel implements ChatModel {
        private Prompt lastPrompt;

        @Override
        public ChatResponse call(String baseUrl, String apiKey, Prompt prompt) {
            return respond(prompt);
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            return respond(prompt);
        }

        @Override
        public void stream(String baseUrl, String apiKey, Prompt prompt, StreamingResponseHandler handler) {
            this.lastPrompt = prompt;
        }

        @Override
        public void stream(Prompt prompt, StreamingResponseHandler handler) {
            this.lastPrompt = prompt;
        }

        private ChatResponse respond(Prompt prompt) {
            this.lastPrompt = prompt;
            Message assistant = Message.withAssistant("ok");
            Generation generation = new Generation();
            generation.setMessage(assistant);
            ChatResponse response = new ChatResponse();
            response.setGenerations(Collections.singletonList(generation));
            return response;
        }

        public Prompt getLastPrompt() {
            return lastPrompt;
        }
    }
}
