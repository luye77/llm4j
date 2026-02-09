package com.bobo.llm4j.template;

import java.util.Map;

/**
 * Template renderer for rendering text templates with parameters
 */
public interface TemplateRenderer {
    
    /**
     * Render a template with parameters
     */
    String render(String template, Map<String, Object> params);
}
