package com.bobo.llm4j.http;

/**
 * MIME type representation
 */
public class MimeType {
    
    private final String type;
    private final String subtype;
    
    public MimeType(String type, String subtype) {
        this.type = type;
        this.subtype = subtype;
    }
    
    public static MimeType valueOf(String mimeType) {
        String[] parts = mimeType.split("/");
        if (parts.length == 2) {
            return new MimeType(parts[0], parts[1]);
        }
        throw new IllegalArgumentException("Invalid MIME type: " + mimeType);
    }
    
    @Override
    public String toString() {
        return type + "/" + subtype;
    }
}
