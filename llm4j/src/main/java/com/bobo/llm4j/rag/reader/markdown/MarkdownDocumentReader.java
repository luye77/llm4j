package com.bobo.llm4j.rag.reader.markdown;

import com.bobo.llm4j.rag.document.RagDocument;
import com.bobo.llm4j.rag.etl.DocumentReader;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.BlockQuote;
import org.commonmark.node.Code;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.Heading;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.ListItem;
import org.commonmark.node.Node;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.Text;
import org.commonmark.node.ThematicBreak;
import org.commonmark.parser.Parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Markdown reader aligned with Spring AI's markdown ETL flow.
 */
public class MarkdownDocumentReader implements DocumentReader {

    private final List<Path> markdownFiles;
    private final MarkdownDocumentReaderConfig config;
    private final Parser parser;

    public MarkdownDocumentReader(List<Path> markdownFiles, MarkdownDocumentReaderConfig config) {
        if (markdownFiles == null || markdownFiles.isEmpty()) {
            throw new IllegalArgumentException("markdownFiles cannot be empty");
        }
        this.markdownFiles = markdownFiles;
        this.config = config == null ? MarkdownDocumentReaderConfig.defaultConfig() : config;
        this.parser = Parser.builder().build();
    }

    @Override
    public List<RagDocument> read() {
        List<RagDocument> all = new ArrayList<RagDocument>();
        for (Path path : this.markdownFiles) {
            if (path == null || !Files.exists(path)) {
                continue;
            }
            try (InputStream inputStream = Files.newInputStream(path);
                 InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                Node node = parser.parseReader(reader);
                Visitor visitor = new Visitor(path.toString(), this.config);
                node.accept(visitor);
                all.addAll(visitor.documents());
            } catch (IOException e) {
                throw new RuntimeException("Failed to parse markdown file: " + path, e);
            }
        }
        return all;
    }

    private static class Visitor extends AbstractVisitor {
        private final String source;
        private final MarkdownDocumentReaderConfig config;
        private final List<RagDocument> docs = new ArrayList<RagDocument>();
        private final List<String> paragraphs = new ArrayList<String>();
        private String title;
        private String category;
        private int index;

        private Visitor(String source, MarkdownDocumentReaderConfig config) {
            this.source = source;
            this.config = config;
        }

        private List<RagDocument> documents() {
            flush();
            return this.docs;
        }

        @Override
        public void visit(Heading heading) {
            flush();
            super.visit(heading);
        }

        @Override
        public void visit(ThematicBreak thematicBreak) {
            if (this.config.isHorizontalRuleCreateDocument()) {
                flush();
            }
            super.visit(thematicBreak);
        }

        @Override
        public void visit(SoftLineBreak softLineBreak) {
            addSpace();
            super.visit(softLineBreak);
        }

        @Override
        public void visit(HardLineBreak hardLineBreak) {
            addSpace();
            super.visit(hardLineBreak);
        }

        @Override
        public void visit(ListItem listItem) {
            addSpace();
            super.visit(listItem);
        }

        @Override
        public void visit(BlockQuote blockQuote) {
            if (!config.isIncludeBlockquote()) {
                flush();
            }
            this.category = "blockquote";
            addSpace();
            super.visit(blockQuote);
        }

        @Override
        public void visit(Code code) {
            this.category = "code_inline";
            this.paragraphs.add(code.getLiteral());
            super.visit(code);
        }

        @Override
        public void visit(FencedCodeBlock fencedCodeBlock) {
            if (!config.isIncludeCodeBlock()) {
                flush();
            }
            this.category = "code_block";
            this.paragraphs.add(fencedCodeBlock.getLiteral());
            flushWithLang(fencedCodeBlock.getInfo());
            super.visit(fencedCodeBlock);
        }

        @Override
        public void visit(Text text) {
            if (text.getParent() instanceof Heading) {
                this.category = "header";
                this.title = text.getLiteral();
            } else {
                this.paragraphs.add(text.getLiteral());
            }
            super.visit(text);
        }

        private void flush() {
            flushWithLang(null);
        }

        private void flushWithLang(String lang) {
            if (this.paragraphs.isEmpty()) {
                this.category = null;
                return;
            }
            String content = join(this.paragraphs);
            if (content.trim().isEmpty()) {
                this.paragraphs.clear();
                this.category = null;
                return;
            }
            RagDocument.RagDocumentBuilder builder = RagDocument.builder().text(content);
            builder.metadata(withBaseMetadata(lang));
            this.docs.add(builder.build());
            this.index++;
            this.paragraphs.clear();
            this.category = null;
        }

        private Map<String, Object> withBaseMetadata(String lang) {
            Map<String, Object> metadata = new java.util.LinkedHashMap<String, Object>();
            metadata.put("source", source);
            metadata.put("chunk_index", index);
            if (title != null && !title.trim().isEmpty()) {
                metadata.put("title", title);
            }
            if (category != null) {
                metadata.put("category", category);
            }
            if (lang != null && !lang.trim().isEmpty()) {
                metadata.put("lang", lang);
            }
            metadata.putAll(config.getAdditionalMetadata());
            return metadata;
        }

        private static String join(List<String> parts) {
            StringBuilder sb = new StringBuilder();
            for (String part : parts) {
                if (part == null) {
                    continue;
                }
                sb.append(part);
            }
            return sb.toString();
        }

        private void addSpace() {
            if (!this.paragraphs.isEmpty()) {
                this.paragraphs.add(" ");
            }
        }
    }
}

