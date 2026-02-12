package com.bobo.llm4j.rag;

import com.bobo.llm4j.rag.document.RagDocument;
import com.bobo.llm4j.rag.transformer.TokenTextSplitter;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TokenTextSplitterTest {

    @Test
    public void testTransformShouldSplitAndKeepParentMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<String, Object>();
        metadata.put("source", "docs/demo.md");

        String text = "Java 是一种面向对象语言。它有丰富的生态系统。"
                + "Spring 是 Java 生态中的重要框架。RAG 可以帮助模型访问外部知识。";

        RagDocument source = RagDocument.builder()
                .id("doc-1")
                .text(text)
                .metadata(metadata)
                .build();

        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .chunkSize(20)
                .minChunkSizeChars(10)
                .minChunkLengthToEmbed(2)
                .maxNumChunks(20)
                .keepSeparator(true)
                .build();

        List<RagDocument> chunks = splitter.transform(Arrays.asList(source));
        Assert.assertFalse(chunks.isEmpty());
        Assert.assertEquals("doc-1", chunks.get(0).getMetadata().get("parent_document_id"));
        Assert.assertTrue(chunks.get(0).getMetadata().containsKey("chunk_index"));
        Assert.assertTrue(chunks.get(0).getText().length() > 0);
    }
}

