package com.bobo.llm4j.rag.transformer;

import com.bobo.llm4j.rag.document.RagDocument;
import com.bobo.llm4j.rag.etl.DocumentTransformer;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;
import lombok.Builder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Token-aware splitter for RAG chunking.
 */
public class TokenTextSplitter implements DocumentTransformer {

    private static final int DEFAULT_CHUNK_SIZE = 800;
    private static final int MIN_CHUNK_SIZE_CHARS = 350;
    private static final int MIN_CHUNK_LENGTH_TO_EMBED = 5;
    private static final int MAX_NUM_CHUNKS = 10000;
    private static final boolean KEEP_SEPARATOR = true;

    private final EncodingRegistry registry = Encodings.newLazyEncodingRegistry();
    private final Encoding encoding = this.registry.getEncoding(EncodingType.CL100K_BASE);

    private final int chunkSize;
    private final int minChunkSizeChars;
    private final int minChunkLengthToEmbed;
    private final int maxNumChunks;
    private final boolean keepSeparator;

    public TokenTextSplitter() {
        this(DEFAULT_CHUNK_SIZE, MIN_CHUNK_SIZE_CHARS, MIN_CHUNK_LENGTH_TO_EMBED, MAX_NUM_CHUNKS, KEEP_SEPARATOR);
    }

    @Builder
    public TokenTextSplitter(int chunkSize, int minChunkSizeChars, int minChunkLengthToEmbed, int maxNumChunks,
                             boolean keepSeparator) {
        this.chunkSize = chunkSize <= 0 ? DEFAULT_CHUNK_SIZE : chunkSize;
        this.minChunkSizeChars = minChunkSizeChars <= 0 ? MIN_CHUNK_SIZE_CHARS : minChunkSizeChars;
        this.minChunkLengthToEmbed = minChunkLengthToEmbed <= 0 ? MIN_CHUNK_LENGTH_TO_EMBED : minChunkLengthToEmbed;
        this.maxNumChunks = maxNumChunks <= 0 ? MAX_NUM_CHUNKS : maxNumChunks;
        this.keepSeparator = keepSeparator;
    }

    @Override
    public List<RagDocument> transform(List<RagDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return new ArrayList<RagDocument>();
        }
        List<RagDocument> output = new ArrayList<RagDocument>();
        for (RagDocument document : documents) {
            if (document == null || document.getText() == null) {
                continue;
            }
            List<String> chunks = doSplit(document.getText());
            int idx = 0;
            for (String chunk : chunks) {
                if (chunk == null || chunk.trim().isEmpty()) {
                    continue;
                }
                Map<String, Object> metadata = new LinkedHashMap<String, Object>();
                if (document.getMetadata() != null) {
                    metadata.putAll(document.getMetadata());
                }
                metadata.put("chunk_index", idx++);
                metadata.put("parent_document_id", document.getId());
                output.add(RagDocument.builder()
                        .id(UUID.randomUUID().toString())
                        .text(chunk)
                        .metadata(metadata)
                        .build());
            }
        }
        return output;
    }

    private List<String> doSplit(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<String>();
        }

        List<Integer> tokens = getEncodedTokens(text);
        List<String> chunks = new ArrayList<String>();
        int generated = 0;

        while (!tokens.isEmpty() && generated < this.maxNumChunks) {
            List<Integer> chunkTokens = tokens.subList(0, Math.min(this.chunkSize, tokens.size()));
            String chunkText = decodeTokens(chunkTokens);

            if (tokens.size() > this.chunkSize) {
                int end = lastPunctuation(chunkText);
                if (end != -1 && end > this.minChunkSizeChars) {
                    chunkText = chunkText.substring(0, end + 1);
                }
            }

            String toAppend = this.keepSeparator ? chunkText.trim() : chunkText.replace('\n', ' ').trim();
            if (toAppend.length() > this.minChunkLengthToEmbed) {
                chunks.add(toAppend);
            }

            int consumeSize = getEncodedTokens(chunkText).size();
            if (consumeSize <= 0) {
                break;
            }
            tokens = tokens.subList(Math.min(consumeSize, tokens.size()), tokens.size());
            generated++;
        }

        if (!tokens.isEmpty()) {
            String rest = decodeTokens(tokens).replace('\n', ' ').trim();
            if (rest.length() > this.minChunkLengthToEmbed) {
                chunks.add(rest);
            }
        }

        return chunks;
    }

    private int lastPunctuation(String text) {
        int p1 = text.lastIndexOf('.');
        int p2 = text.lastIndexOf('?');
        int p3 = text.lastIndexOf('!');
        int p4 = text.lastIndexOf('\n');
        return Math.max(Math.max(p1, p2), Math.max(p3, p4));
    }

    private List<Integer> getEncodedTokens(String text) {
        return this.encoding.encode(text).boxed();
    }

    private String decodeTokens(List<Integer> tokens) {
        IntArrayList ints = new IntArrayList(tokens.size());
        for (Integer token : tokens) {
            ints.add(token);
        }
        return this.encoding.decode(ints);
    }
}

