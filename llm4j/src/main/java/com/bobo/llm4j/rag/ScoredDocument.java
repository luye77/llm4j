package com.bobo.llm4j.rag;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ScoredDocument - 带相似度分数的文档
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoredDocument {
    private Document document;
    private double score;
}
