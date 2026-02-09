package com.bobo.llm4j.template;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple template renderer using {key} syntax
 */
public class StTemplateRenderer implements TemplateRenderer {
    
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^}]+)}");
    
    public static Builder builder() {
        return new Builder();
    }
    
    @Override
    public String render(String template, Map<String, Object> params) {
        if (template == null || params == null || params.isEmpty()) {
            return template;
        }
        
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = params.get(key);
            matcher.appendReplacement(result, value != null ? Matcher.quoteReplacement(value.toString()) : "");
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    public static class Builder {
        
        public StTemplateRenderer build() {
            return new StTemplateRenderer();
        }
    }
}
