package com.ragreader.model;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.embedding.Embedding;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/**
 * A chunk of an Obsidian note, ready for embedding and indexing
 */
public class NoteChunk {
    private String id;
    private Path notePath;
    private String heading;        // heading hierarchy, e.g., "## Section > ### Subsection"
    private String content;
    private NoteMetadata metadata;
    private List<String> wikilinks; // [[links]] found in this chunk
    private Instant fileModifiedAt;
    private String contentHash;    // SHA-256 of content for incremental indexing
    
    // Embedding (populated after embedding)
    private Embedding embedding;
    
    public NoteChunk() {}
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public Path getNotePath() { return notePath; }
    public void setNotePath(Path notePath) { this.notePath = notePath; }
    
    public String getHeading() { return heading; }
    public void setHeading(String heading) { this.heading = heading; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public NoteMetadata getMetadata() { return metadata; }
    public void setMetadata(NoteMetadata metadata) { this.metadata = metadata; }
    
    public List<String> getWikilinks() { return wikilinks; }
    public void setWikilinks(List<String> wikilinks) { this.wikilinks = wikilinks; }
    
    public Instant getFileModifiedAt() { return fileModifiedAt; }
    public void setFileModifiedAt(Instant fileModifiedAt) { this.fileModifiedAt = fileModifiedAt; }
    
    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
    
    public Embedding getEmbedding() { return embedding; }
    public void setEmbedding(Embedding embedding) { this.embedding = embedding; }
    
    /**
     * Convert to LangChain4j TextSegment for embedding
     */
    public TextSegment toTextSegment() {
        // Build metadata map for filtering
        var metadataMap = new java.util.HashMap<String, String>();
        metadataMap.put("notePath", notePath.toString());
        metadataMap.put("heading", heading != null ? heading : "");
        
        if (metadata != null) {
            if (metadata.getType() != null) {
                metadataMap.put("type", metadata.getType());
            }
            if (metadata.getTags() != null && !metadata.getTags().isEmpty()) {
                metadataMap.put("tags", String.join(",", metadata.getTags()));
            }
            if (metadata.getSource() != null) {
                metadataMap.put("source", metadata.getSource());
            }
        }
        
        TextSegment segment = TextSegment.from(content, dev.langchain4j.data.document.Metadata.from(metadataMap));
        return segment;
    }
}
