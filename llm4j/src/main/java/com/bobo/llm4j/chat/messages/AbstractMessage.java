package com.bobo.llm4j.chat.messages;


import com.bobo.llm4j.chat.entity.Media;
import com.bobo.llm4j.chat.entity.Message;

/**
 * Abstract base for messages
 */
public class AbstractMessage {
    
    public static String getText(Message message) {
        if (message == null) {
            return null;
        }
        Media content = message.getContent();
        if (content != null && content.getText() != null) {
            return content.getText();
        }
        return null;
    }
}
