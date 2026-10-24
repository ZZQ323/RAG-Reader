package com.ragreader.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Metadata extracted from Obsidian note frontmatter
 */
public class NoteMetadata {
    private String type;           // fragment, book, method, log, etc.
    private List<String> tags;
    private String source;         // [[原文]] link
    private Instant createdAt;
    private Map<String, Object> extra; // other frontmatter fields
    
    public NoteMetadata() {
        this.extra = new HashMap<>();
    }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Map<String, Object> getExtra() { return extra; }
    public void setExtra(Map<String, Object> extra) { this.extra = extra; }
    
    public void putExtra(String key, Object value) {
        this.extra.put(key, value);
    }
    
    public Object getExtra(String key) {
        return this.extra.get(key);
    }
}
