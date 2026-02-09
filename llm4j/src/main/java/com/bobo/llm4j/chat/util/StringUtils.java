package com.bobo.llm4j.chat.util;

import java.util.Collection;

/**
 * String utility methods
 */
public class StringUtils {
    
    public static boolean hasText(String str) {
        return str != null && !str.trim().isEmpty();
    }
    
    public static boolean hasLength(String str) {
        return str != null && !str.isEmpty();
    }
    
    public static String[] toStringArray(Collection<String> collection) {
        if (collection == null) {
            return new String[0];
        }
        return collection.toArray(new String[0]);
    }
}
