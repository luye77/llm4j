package com.bobo.llm4j.embedding;

import com.bobo.llm4j.base.BaseTest;
import com.bobo.llm4j.platform.openai.embedding.entity.EmbeddingRequest;
import com.bobo.llm4j.platform.openai.embedding.entity.EmbeddingResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * 嵌入模型测试类
 * <p>
 * 测试文本嵌入功能，包括：
 * <ul>
 *     <li>单文本嵌入</li>
 *     <li>批量文本嵌入</li>
 *     <li>嵌入请求参数配置</li>
 * </ul>
 * </p>
 *
 * @author bobo
 * @since 1.4.3
 */
@Slf4j
public class EmbeddingModelTest extends BaseTest {

    /**
     * 测试单文本嵌入
     *
     * @throws Exception 调用异常
     */
    @Test
    public void testSingleTextEmbedding() throws Exception {
        log.info("=== 测试单文本嵌入 ===");

        String inputText = "The food was delicious and the waiter was friendly.";

        EmbeddingRequest request = EmbeddingRequest.builder()
                .input(inputText)
                .model(EMBEDDING_MODEL)
                .build();

        log.info("嵌入请求 - 输入文本: {}", inputText);

        EmbeddingResponse response = embeddingModel.call(request);

        log.info("嵌入响应 - 模型: {}", response.getModel());
        log.info("嵌入响应 - 向量维度: {}", response.getData().get(0).getEmbedding().size());
        log.info("嵌入响应 - Token使用: {}", response.getUsage());
    }

    /**
     * 测试批量文本嵌入
     *
     * @throws Exception 调用异常
     */
    @Test
    public void testBatchTextEmbedding() throws Exception {
        log.info("=== 测试批量文本嵌入 ===");

        List<String> inputTexts = Arrays.asList(
                "今天天气很好",
                "明天可能会下雨",
                "机器学习是人工智能的一个分支"
        );

        EmbeddingRequest request = EmbeddingRequest.builder()
                .input(inputTexts)
                .model(EMBEDDING_MODEL)
                .build();

        log.info("批量嵌入请求 - 文本数量: {}", inputTexts.size());

        EmbeddingResponse response = embeddingModel.call(request);

        log.info("批量嵌入响应 - 结果数量: {}", response.getData().size());
        response.getData().forEach(embedding ->
                log.info("索引: {}, 向量维度: {}", embedding.getIndex(), embedding.getEmbedding().size())
        );
    }

    /**
     * 测试使用自定义URL和Key调用嵌入
     *
     * @throws Exception 调用异常
     */
    @Test
    public void testEmbeddingWithCustomConfig() throws Exception {
        log.info("=== 测试自定义配置调用嵌入 ===");

        EmbeddingRequest request = EmbeddingRequest.builder()
                .input("测试文本")
                .model(EMBEDDING_MODEL)
                .build();

        // 使用默认配置调用
        EmbeddingResponse response = embeddingModel.call(null, null, request);

        log.info("自定义配置调用完成 - 使用默认配置");
        if (response != null) {
            log.info("响应模型: {}", response.getModel());
        }
    }
}
