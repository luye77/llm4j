package com.bobo.llm4j.rag;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Document - RAG 文档实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Document {
    private String id;
    private String content;
    private Map<String, Object> metadata;
}
