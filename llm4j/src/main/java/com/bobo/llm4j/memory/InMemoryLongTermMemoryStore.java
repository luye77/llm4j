package com.bobo.llm4j.memory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 基于内存的长期记忆存储实现（默认实现）
 * <p>
 * 特性：
 * <ul>
 *   <li>线程安全：使用 ConcurrentHashMap</li>
 *   <li>适合开发和测试</li>
 *   <li>重启后数据丢失</li>
 * </ul>
 *
 * @author bobo
 * @since 1.0.0
 */
public final class InMemoryLongTermMemoryStore implements LongTermMemoryStore {
    
    // conversationId -> (entryId -> MemoryEntry)
    private final Map<String, Map<String, MemoryEntry>> store = new ConcurrentHashMap<>();
    
    @Override
    public Optional<MemoryEntry> findById(String conversationId, String id) {
        validateConversationId(conversationId);
        validateId(id);
        
        Map<String, MemoryEntry> conversationStore = store.get(conversationId);
        if (conversationStore == null) {
            return Optional.empty();
        }
        
        MemoryEntry entry = conversationStore.get(id);
        return Optional.ofNullable(entry);
    }
    
    @Override
    public List<MemoryEntry> findAll(String conversationId) {
        validateConversationId(conversationId);
        
        Map<String, MemoryEntry> conversationStore = store.get(conversationId);
        if (conversationStore == null) {
            return Collections.emptyList();
        }
        
        return new ArrayList<>(conversationStore.values());
    }
    
    @Override
    public List<MemoryEntry> findByType(String conversationId, MemoryType type) {
        validateConversationId(conversationId);
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        
        Map<String, MemoryEntry> conversationStore = store.get(conversationId);
        if (conversationStore == null) {
            return Collections.emptyList();
        }
        
        return conversationStore.values().stream()
                .filter(entry -> entry.getType() == type)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<MemoryEntry> findByTag(String conversationId, String tag) {
        validateConversationId(conversationId);
        if (tag == null || tag.trim().isEmpty()) {
            throw new IllegalArgumentException("tag cannot be null or empty");
        }
        
        Map<String, MemoryEntry> conversationStore = store.get(conversationId);
        if (conversationStore == null) {
            return Collections.emptyList();
        }
        
        return conversationStore.values().stream()
                .filter(entry -> entry.getTags().contains(tag))
                .collect(Collectors.toList());
    }
    
    @Override
    public void save(String conversationId, MemoryEntry entry) {
        validateConversationId(conversationId);
        if (entry == null) {
            throw new IllegalArgumentException("entry cannot be null");
        }
        
        store.computeIfAbsent(conversationId, k -> new ConcurrentHashMap<>())
             .put(entry.getId(), entry);
    }
    
    @Override
    public void saveAll(String conversationId, List<MemoryEntry> entries) {
        validateConversationId(conversationId);
        if (entries == null) {
            throw new IllegalArgumentException("entries cannot be null");
        }
        
        for (MemoryEntry entry : entries) {
            if (entry == null) {
                throw new IllegalArgumentException("entries cannot contain null elements");
            }
        }
        
        Map<String, MemoryEntry> conversationStore = 
            store.computeIfAbsent(conversationId, k -> new ConcurrentHashMap<>());
        
        for (MemoryEntry entry : entries) {
            conversationStore.put(entry.getId(), entry);
        }
    }
    
    @Override
    public void delete(String conversationId, String id) {
        validateConversationId(conversationId);
        validateId(id);
        
        Map<String, MemoryEntry> conversationStore = store.get(conversationId);
        if (conversationStore != null) {
            conversationStore.remove(id);
        }
    }
    
    @Override
    public void deleteAll(String conversationId) {
        validateConversationId(conversationId);
        store.remove(conversationId);
    }
    
    @Override
    public List<String> findAllConversationIds() {
        return new ArrayList<>(store.keySet());
    }
    
    private void validateConversationId(String conversationId) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            throw new IllegalArgumentException("conversationId cannot be null or empty");
        }
    }
    
    private void validateId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("id cannot be null or empty");
        }
    }
}
