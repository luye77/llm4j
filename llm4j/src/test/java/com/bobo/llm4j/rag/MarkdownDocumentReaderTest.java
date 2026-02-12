package com.bobo.llm4j.rag;

import com.bobo.llm4j.rag.document.RagDocument;
import com.bobo.llm4j.rag.reader.markdown.MarkdownDocumentReader;
import com.bobo.llm4j.rag.reader.markdown.MarkdownDocumentReaderConfig;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class MarkdownDocumentReaderTest {

    @Test
    public void testReadShouldParseMarkdownIntoDocuments() throws Exception {
        Path tempFile = Files.createTempFile("rag-md-", ".md");
        String markdown = "# Java\n" +
                "Java 是一种语言。\n\n" +
                "## Spring\n" +
                "Spring 是一个框架。\n" +
                "---\n" +
                "后续内容。\n";
        Files.write(tempFile, markdown.getBytes(StandardCharsets.UTF_8));

        MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.defaultConfig();
        MarkdownDocumentReader reader = new MarkdownDocumentReader(Arrays.asList(tempFile), config);

        List<RagDocument> docs = reader.read();
        Assert.assertFalse(docs.isEmpty());
        Assert.assertEquals(tempFile.toString(), docs.get(0).getMetadata().get("source"));
        Assert.assertTrue(docs.get(0).getMetadata().containsKey("chunk_index"));
        Assert.assertNotNull(docs.get(0).getText());
    }
}

