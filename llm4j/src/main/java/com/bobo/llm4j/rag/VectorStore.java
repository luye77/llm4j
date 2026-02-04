package com.bobo.llm4j.rag;

import java.util.List;

/**
 * VectorStore - 向量存储接口
 */
public interface VectorStore {
    void upsert(List<VectorRecord> records);

    List<ScoredDocument> query(List<Float> vector, int topK);
}
