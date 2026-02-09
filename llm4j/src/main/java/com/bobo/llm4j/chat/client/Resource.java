package com.bobo.llm4j.chat.client;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Resource interface for reading content
 */
public interface Resource {
    
    /**
     * Get content as string
     */
    String getContentAsString(Charset charset) throws IOException;
}
