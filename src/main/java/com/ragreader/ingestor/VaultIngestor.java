package com.ragreader.ingestor;

import com.ragreader.chunker.ObsidianMarkdownChunker;
import com.ragreader.model.NoteChunk;
import com.ragreader.store.NoteEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Ingestor: scans vault directory, chunks files, and indexes them
 */
public class VaultIngestor {
    private static final Logger logger = LoggerFactory.getLogger(VaultIngestor.class);
    
    private final Path vaultPath;
    private final ObsidianMarkdownChunker chunker;
    private final NoteEmbeddingStore store;
    
    public VaultIngestor(Path vaultPath, int chunkSize, int chunkOverlap, NoteEmbeddingStore store) {
        this.vaultPath = vaultPath;
        this.chunker = new ObsidianMarkdownChunker(chunkSize, chunkOverlap);
        this.store = store;
    }
    
    /**
     * Scan vault and index all markdown files
     */
    public void indexAll() throws IOException {
        logger.info("Starting full index of vault: {}", vaultPath);
        
        if (!Files.exists(vaultPath)) {
            logger.error("Vault path does not exist: {}", vaultPath);
            return;
        }
        
        List<Path> markdownFiles = findMarkdownFiles(vaultPath);
        logger.info("Found {} markdown files", markdownFiles.size());
        
        int indexed = 0;
        int skipped = 0;
        
        for (Path file : markdownFiles) {
            try {
                if (indexFile(file)) {
                    indexed++;
                } else {
                    skipped++;
                }
            } catch (Exception e) {
                logger.error("Failed to index file: {}", file, e);
            }
        }
        
        logger.info("Indexing complete: {} files indexed, {} skipped", indexed, skipped);
    }
    
    /**
     * Index a single file (incremental)
     */
    public boolean indexFile(Path filePath) throws IOException {
        // Read file to compute hash
        String content = Files.readString(filePath);
        String contentHash = computeHash(content);
        
        // Check if file has changed
        if (!store.hasFileChanged(filePath, contentHash)) {
            logger.debug("File unchanged, skipping: {}", filePath);
            return false;
        }
        
        // Remove old chunks
        store.removeFileChunks(filePath);
        
        // Chunk file
        List<NoteChunk> chunks = chunker.chunkFile(filePath);
        
        if (chunks.isEmpty()) {
            logger.debug("No chunks generated for: {}", filePath);
            return false;
        }
        
        // Add to store
        store.addChunks(chunks);
        
        logger.debug("Indexed {}: {} chunks", filePath, chunks.size());
        return true;
    }
    
    /**
     * Re-index a specific file (force)
     */
    public void reindexFile(Path filePath) throws IOException {
        logger.info("Re-indexing file: {}", filePath);
        store.removeFileChunks(filePath);
        indexFile(filePath);
    }
    
    /**
     * Find all markdown files in vault
     */
    private List<Path> findMarkdownFiles(Path root) throws IOException {
        List<Path> files = new ArrayList<>();
        
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.toString().endsWith(".md") || file.toString().endsWith(".markdown")) {
                    files.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                // Skip hidden directories
                if (dir.getFileName().toString().startsWith(".")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }
        });
        
        return files;
    }
    
    private String computeHash(String content) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * Get statistics about indexed content
     */
    public IndexStats getStats() {
        Set<String> indexedFiles = store.getIndexedFiles();
        return new IndexStats(indexedFiles.size());
    }
    
    public static class IndexStats {
        private final int fileCount;
        
        public IndexStats(int fileCount) {
            this.fileCount = fileCount;
        }
        
        public int getFileCount() { return fileCount; }
    }
}
