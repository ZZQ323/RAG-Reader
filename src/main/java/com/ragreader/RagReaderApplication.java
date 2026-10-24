package com.ragreader;

import com.ragreader.config.RagReaderConfig;
import com.ragreader.ingestor.VaultIngestor;
import com.ragreader.mcp.McpServer;
import com.ragreader.store.NoteEmbeddingStore;
import com.ragreader.watcher.VaultWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Main application entry point
 */
public class RagReaderApplication {
    private static final Logger logger = LoggerFactory.getLogger(RagReaderApplication.class);
    
    private final RagReaderConfig config;
    private final NoteEmbeddingStore store;
    private final VaultIngestor ingestor;
    private final VaultWatcher watcher;
    private final McpServer mcpServer;
    
    public RagReaderApplication(RagReaderConfig config) {
        this.config = config;
        this.store = new NoteEmbeddingStore();
        this.ingestor = new VaultIngestor(
                config.getVaultPathAsPath(),
                config.getChunkSize(),
                config.getChunkOverlap(),
                store
        );
        this.watcher = new VaultWatcher(config.getVaultPathAsPath(), ingestor);
        this.mcpServer = new McpServer(
                config.getMcpHost(),
                config.getMcpPort(),
                store
        );
    }
    
    public void start() throws IOException {
        logger.info("Starting RAG-Reader...");
        logger.info("Vault path: {}", config.getVaultPath());
        logger.info("MCP endpoint: http://{}:{}/mcp", config.getMcpHost(), config.getMcpPort());
        
        // Initial indexing
        logger.info("Performing initial indexing...");
        ingestor.indexAll();
        
        // Start file watcher
        watcher.start();
        
        // Start MCP server
        mcpServer.start();
        
        logger.info("RAG-Reader started successfully");
        logger.info("Press Ctrl+C to stop");
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down...");
            stop();
        }));
    }
    
    public void stop() {
        mcpServer.stop();
        watcher.stop();
        logger.info("RAG-Reader stopped");
    }
    
    public static void main(String[] args) {
        try {
            RagReaderConfig config = new RagReaderConfig();
            
            // Load config from file if provided
            if (args.length > 0) {
                config.loadFromFile(args[0]);
            } else {
                // Try default location
                String defaultConfig = System.getProperty("user.home") + "/.rag-reader/config.yaml";
                if (new java.io.File(defaultConfig).exists()) {
                    config.loadFromFile(defaultConfig);
                }
            }
            
            // Override vault path from command line if provided
            if (args.length > 1) {
                // args[1] = vault path
                config = new RagReaderConfig() {
                    @Override
                    public String getVaultPath() {
                        return args[1];
                    }
                    @Override
                    public Path getVaultPathAsPath() {
                        return Path.of(args[1]);
                    }
                };
            }
            
            RagReaderApplication app = new RagReaderApplication(config);
            app.start();
            
            // Keep main thread alive
            Thread.currentThread().join();
            
        } catch (Exception e) {
            logger.error("Failed to start RAG-Reader", e);
            System.exit(1);
        }
    }
}
