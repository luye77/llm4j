package com.bobo.llm4j.memory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 日记 - 单日的对话流水账（append-only）
 *
 * @author bobo
 * @since 1.0.0
 */
public final class DailyNote {
    
    private final LocalDate date;
    private final List<DailyEntry> entries;
    
    public DailyNote(LocalDate date) {
        this(date, new ArrayList<>());
    }
    
    public DailyNote(LocalDate date, List<DailyEntry> entries) {
        if (date == null) {
            throw new IllegalArgumentException("date cannot be null");
        }
        if (entries == null) {
            throw new IllegalArgumentException("entries cannot be null");
        }
        this.date = date;
        this.entries = new ArrayList<>(entries);
    }
    
    /**
     * 追加条目（append-only）
     */
    public void append(DailyEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("entry cannot be null");
        }
        this.entries.add(entry);
    }
    
    /**
     * 批量追加条目
     */
    public void appendAll(List<DailyEntry> entriesToAdd) {
        if (entriesToAdd == null) {
            throw new IllegalArgumentException("entriesToAdd cannot be null");
        }
        for (DailyEntry entry : entriesToAdd) {
            if (entry == null) {
                throw new IllegalArgumentException("entries cannot contain null elements");
            }
        }
        this.entries.addAll(entriesToAdd);
    }
    
    public LocalDate getDate() {
        return date;
    }
    
    /**
     * 获取所有条目（不可修改）
     */
    public List<DailyEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }
    
    /**
     * 获取条目数量
     */
    public int size() {
        return entries.size();
    }
    
    /**
     * 是否为空
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DailyNote dailyNote = (DailyNote) o;
        return Objects.equals(date, dailyNote.date);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(date);
    }
    
    @Override
    public String toString() {
        return "DailyNote{" +
               "date=" + date +
               ", entriesCount=" + entries.size() +
               '}';
    }
}
