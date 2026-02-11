package com.bobo.llm4j.rag;

import com.bobo.llm4j.chat.model.ChatModel;
import com.bobo.llm4j.config.Configuration;
import com.bobo.llm4j.rag.advisor.QuestionAnswerAdvisor;
import com.bobo.llm4j.rag.config.RagOptions;
import com.bobo.llm4j.rag.embedding.EmbeddingModel;
import com.bobo.llm4j.rag.embedding.EmbeddingModels;
import com.bobo.llm4j.rag.ingest.RagIngestionService;
import com.bobo.llm4j.rag.reader.markdown.MarkdownDocumentReader;
import com.bobo.llm4j.rag.reader.markdown.MarkdownDocumentReaderConfig;
import com.bobo.llm4j.rag.retrieval.VectorStoreDocumentRetriever;
import com.bobo.llm4j.rag.transformer.SummaryMetadataEnricher;
import com.bobo.llm4j.rag.transformer.TokenTextSplitter;
import com.bobo.llm4j.rag.vectorstore.InMemoryVectorStore;
import com.bobo.llm4j.rag.vectorstore.VectorStore;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory for assembling an in-memory markdown RAG pipeline.
 */
public class RagModuleFactory {

    public static RagModule createMarkdownInMemoryModule(Configuration configuration,
                                                         ChatModel chatModel,
                                                         List<Path> markdownFiles,
                                                         RagOptions options) {
        RagOptions ragOptions = options == null ? RagOptions.builder().build() : options;

        EmbeddingModel embeddingModel = EmbeddingModels.create(configuration,
                ragOptions.getEmbeddingProvider(),
                ragOptions.getQwenEmbeddingModel(),
                ragOptions.getOpenAiEmbeddingModel());

        VectorStore vectorStore = new InMemoryVectorStore();
        MarkdownDocumentReader reader = new MarkdownDocumentReader(markdownFiles, MarkdownDocumentReaderConfig.defaultConfig());

        List<com.bobo.llm4j.rag.etl.DocumentTransformer> transformers = new ArrayList<com.bobo.llm4j.rag.etl.DocumentTransformer>();
        transformers.add(new TokenTextSplitter());
        transformers.add(SummaryMetadataEnricher.builder()
                .chatModel(chatModel)
                .modelName("default")
                .enabled(ragOptions.isSummaryMetadataEnabled())
                .build());

        RagIngestionService ingestionService = new RagIngestionService(reader, transformers, embeddingModel, vectorStore);

        VectorStoreDocumentRetriever retriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .embeddingModel(embeddingModel)
                .topK(ragOptions.getTopK())
                .similarityThreshold(ragOptions.getSimilarityThreshold())
                .build();

        QuestionAnswerAdvisor advisor = QuestionAnswerAdvisor.builder(retriever).build();
        return new RagModule(ingestionService, advisor, retriever, vectorStore);
    }
}

