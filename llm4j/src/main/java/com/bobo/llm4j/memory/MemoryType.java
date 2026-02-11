package com.bobo.llm4j.memory;

/**
 * 长期记忆类型枚举
 *
 * @author bobo
 * @since 1.0.0
 */
public enum MemoryType {
    
    /**
     * 用户偏好：编程语言、代码风格、工具选择等
     */
    USER_PREFERENCE("用户偏好"),
    
    /**
     * 常见上下文：项目背景、技术栈、团队规范等
     */
    RECURRING_CONTEXT("常见上下文"),
    
    /**
     * 关键决策：架构选择、技术方案、重要变更等
     */
    KEY_DECISION("关键决策"),
    
    /**
     * 经验教训：踩过的坑、解决方案、最佳实践等
     */
    LESSON_LEARNED("经验教训"),
    
    /**
     * 自定义记忆
     */
    CUSTOM("自定义");
    
    private final String description;
    
    MemoryType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
