package com.ragreader.store;

import com.ragreader.model.NoteChunk;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Vector store for note chunks
 * Uses InMemoryEmbeddingStore for simplicity, can be swapped to Milvus later
 */
public class NoteEmbeddingStore {
    private static final Logger logger = LoggerFactory.getLogger(NoteEmbeddingStore.class);
    
    private final EmbeddingStore<TextSegment> store;
    private final EmbeddingModel embeddingModel;
    private final Map<String, NoteChunk> chunkIndex; // id -> chunk
    private final Map<String, String> fileHashIndex; // filePath -> contentHash
    
    public NoteEmbeddingStore() {
        this.store = new InMemoryEmbeddingStore<>();
        this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        this.chunkIndex = new ConcurrentHashMap<>();
        this.fileHashIndex = new ConcurrentHashMap<>();
    }
    
    /**
     * Add chunks to the store
     */
    public void addChunks(List<NoteChunk> chunks) {
        if (chunks.isEmpty()) return;
        
        logger.info("Adding {} chunks to embedding store", chunks.size());
        
        // Convert to TextSegments
        List<TextSegment> segments = new ArrayList<>();
        for (NoteChunk chunk : chunks) {
            segments.add(chunk.toTextSegment());
        }
        
        // Embed all segments
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        
        // Add to store
        store.addAll(embeddings, segments);
        
        // Update indices
        for (int i = 0; i < chunks.size(); i++) {
            NoteChunk chunk = chunks.get(i);
            chunk.setEmbedding(embeddings.get(i));
            chunkIndex.put(chunk.getId(), chunk);
        }
        
        // Update file hash index
        for (NoteChunk chunk : chunks) {
            fileHashIndex.put(chunk.getNotePath().toString(), chunk.getContentHash());
        }
        
        logger.info("Added {} chunks, total chunks: {}", chunks.size(), chunkIndex.size());
    }
    
    /**
     * Remove chunks for a file
     */
    public void removeFileChunks(Path filePath) {
        String pathStr = filePath.toString();
        
        // Find all chunks for this file
        List<String> chunkIds = new ArrayList<>();
        for (Map.Entry<String, NoteChunk> entry : chunkIndex.entrySet()) {
            if (entry.getValue().getNotePath().toString().equals(pathStr)) {
                chunkIds.add(entry.getKey());
            }
        }
        
        // Remove from index
        for (String id : chunkIds) {
            chunkIndex.remove(id);
        }
        
        // Remove from file hash index
        fileHashIndex.remove(pathStr);
        
        logger.info("Removed {} chunks for file: {}", chunkIds.size(), filePath);
    }
    
    /**
     * Search for similar chunks
     */
    public List<SearchResult> search(String query, int maxResults, double minScore) {
        logger.debug("Searching for: {}", query);
        
        // Embed query
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        
        // Search
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(maxResults)
                .minScore(minScore)
                .build();
        
        List<EmbeddingMatch<TextSegment>> matches = store.search(request).matches();
        
        // Convert to SearchResult
        List<SearchResult> results = new ArrayList<>();
        for (EmbeddingMatch<TextSegment> match : matches) {
            String notePath = match.embedded().metadata().getString("notePath");
            NoteChunk chunk = chunkIndex.values().stream()
                    .filter(c -> c.getNotePath().toString().equals(notePath))
                    .findFirst()
                    .orElse(null);
            
            if (chunk != null) {
                results.add(new SearchResult(chunk, match.score()));
            }
        }
        
        logger.debug("Found {} results", results.size());
        return results;
    }
    
    /**
     * Check if a file has changed
     */
    public boolean hasFileChanged(Path filePath, String contentHash) {
        String storedHash = fileHashIndex.get(filePath.toString());
        return storedHash == null || !storedHash.equals(contentHash);
    }
    
    /**
     * Get all chunks for a file
     */
    public List<NoteChunk> getFileChunks(Path filePath) {
        List<NoteChunk> chunks = new ArrayList<>();
        for (NoteChunk chunk : chunkIndex.values()) {
            if (chunk.getNotePath().equals(filePath)) {
                chunks.add(chunk);
            }
        }
        return chunks;
    }
    
    /**
     * Get a specific chunk by ID
     */
    public NoteChunk getChunk(String chunkId) {
        return chunkIndex.get(chunkId);
    }
    
    /**
     * Get all indexed files
     */
    public Set<String> getIndexedFiles() {
        return new HashSet<>(fileHashIndex.keySet());
    }
    
    /**
     * Save store to disk
     */
    public void saveToDisk(Path dbPath) throws IOException {
        Files.createDirectories(dbPath.getParent());
        
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(dbPath))) {
            oos.writeObject(store);
            oos.writeObject(chunkIndex);
            oos.writeObject(fileHashIndex);
        }
        
        logger.info("Saved store to {}", dbPath);
    }
    
    /**
     * Load store from disk
     */
    @SuppressWarnings("unchecked")
    public void loadFromDisk(Path dbPath) throws IOException, ClassNotFoundException {
        if (!Files.exists(dbPath)) {
            logger.info("No existing store found at {}", dbPath);
            return;
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(dbPath))) {
            // Note: InMemoryEmbeddingStore is not serializable, so we need to rebuild
            // For now, just log that we can't load
            logger.warn("InMemoryEmbeddingStore is not serializable, starting fresh");
        }
    }
    
    /**
     * Search result wrapper
     */
    public static class SearchResult {
        private final NoteChunk chunk;
        private final double score;
        
        public SearchResult(NoteChunk chunk, double score) {
            this.chunk = chunk;
            this.score = score;
        }
        
        public NoteChunk getChunk() { return chunk; }
        public double getScore() { return score; }
    }
}
