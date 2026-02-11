package com.bobo.llm4j.memory;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 基于内存的日记存储实现（默认实现）
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
public final class InMemoryDailyNotesStore implements DailyNotesStore {
    
    // conversationId -> (date -> DailyNote)
    private final Map<String, Map<LocalDate, DailyNote>> store = new ConcurrentHashMap<>();
    
    @Override
    public Optional<DailyNote> findByDate(String conversationId, LocalDate date) {
        validateConversationId(conversationId);
        validateDate(date);
        
        Map<LocalDate, DailyNote> conversationStore = store.get(conversationId);
        if (conversationStore == null) {
            return Optional.empty();
        }
        
        DailyNote note = conversationStore.get(date);
        return Optional.ofNullable(note);
    }
    
    @Override
    public List<DailyNote> findRecent(String conversationId, int days) {
        validateConversationId(conversationId);
        if (days <= 0) {
            throw new IllegalArgumentException("days must be greater than 0");
        }
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        
        List<DailyNote> notes = findByDateRange(conversationId, startDate, endDate);
        
        // 倒序排列（最新的在前）
        notes.sort(Comparator.comparing(DailyNote::getDate).reversed());
        
        return notes;
    }
    
    @Override
    public List<DailyNote> findByDateRange(String conversationId, LocalDate startDate, LocalDate endDate) {
        validateConversationId(conversationId);
        validateDate(startDate);
        validateDate(endDate);
        
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate cannot be after endDate");
        }
        
        Map<LocalDate, DailyNote> conversationStore = store.get(conversationId);
        if (conversationStore == null) {
            return Collections.emptyList();
        }
        
        return conversationStore.entrySet().stream()
                .filter(entry -> !entry.getKey().isBefore(startDate) && !entry.getKey().isAfter(endDate))
                .map(Map.Entry::getValue)
                .sorted(Comparator.comparing(DailyNote::getDate))
                .collect(Collectors.toList());
    }
    
    @Override
    public void save(String conversationId, DailyNote dailyNote) {
        validateConversationId(conversationId);
        if (dailyNote == null) {
            throw new IllegalArgumentException("dailyNote cannot be null");
        }
        
        store.computeIfAbsent(conversationId, k -> new ConcurrentHashMap<>())
             .put(dailyNote.getDate(), dailyNote);
    }
    
    @Override
    public void delete(String conversationId, LocalDate date) {
        validateConversationId(conversationId);
        validateDate(date);
        
        Map<LocalDate, DailyNote> conversationStore = store.get(conversationId);
        if (conversationStore != null) {
            conversationStore.remove(date);
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
    
    private void validateDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("date cannot be null");
        }
    }
}
