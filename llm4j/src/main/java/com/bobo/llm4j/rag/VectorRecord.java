package com.bobo.llm4j.rag;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * VectorRecord - 向量记录
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VectorRecord {
    private String id;
    private String content;
    private List<Float> vector;
    private Map<String, Object> metadata;

    public Document toDocument() {
        return new Document(id, content, metadata);
    }
}
