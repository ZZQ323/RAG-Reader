package com.ragreader.watcher;

import com.ragreader.ingestor.VaultIngestor;
import io.methvin.watcher.DirectoryWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * File system watcher for vault directory
 * Automatically re-indexes files when they change
 */
public class VaultWatcher {
    private static final Logger logger = LoggerFactory.getLogger(VaultWatcher.class);
    
    private final Path vaultPath;
    private final VaultIngestor ingestor;
    private DirectoryWatcher watcher;
    private ExecutorService executor;
    
    public VaultWatcher(Path vaultPath, VaultIngestor ingestor) {
        this.vaultPath = vaultPath;
        this.ingestor = ingestor;
    }
    
    public void start() throws IOException {
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "vault-watcher");
            t.setDaemon(true);
            return t;
        });
        
        watcher = DirectoryWatcher.builder()
                .path(vaultPath)
                .listener(event -> {
                    Path path = event.path();
                    
                    // Only process markdown files
                    if (!path.toString().endsWith(".md") && !path.toString().endsWith(".markdown")) {
                        return;
                    }
                    
                    logger.info("File changed: {} ({})", path, event.eventType());
                    
                    try {
                        switch (event.eventType()) {
                            case CREATE:
                            case MODIFY:
                                ingestor.reindexFile(path);
                                break;
                            case DELETE:
                                // Could remove from index, but for now just log
                                logger.info("File deleted: {}", path);
                                break;
                        }
                    } catch (Exception e) {
                        logger.error("Error processing file change: {}", path, e);
                    }
                })
                .build();
        
        executor.submit(() -> {
            try {
                watcher.watch();
            } catch (Exception e) {
                logger.error("Watcher error", e);
            }
        });
        
        logger.info("Vault watcher started for: {}", vaultPath);
    }
    
    public void stop() {
        if (watcher != null) {
            watcher.close();
        }
        if (executor != null) {
            executor.shutdown();
        }
        logger.info("Vault watcher stopped");
    }
}
