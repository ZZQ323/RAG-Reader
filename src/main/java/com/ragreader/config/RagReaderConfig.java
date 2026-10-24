package com.ragreader.config;

import org.yaml.snakeyaml.Yaml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Configuration loader for RAG-Reader
 */
public class RagReaderConfig {
    private static final Logger logger = LoggerFactory.getLogger(RagReaderConfig.class);
    
    private String vaultPath;
    private String dbPath;
    private int chunkSize;
    private int chunkOverlap;
    private int embeddingDimension;
    private String mcpHost;
    private int mcpPort;
    
    public RagReaderConfig() {
        // Defaults
        this.vaultPath = System.getProperty("user.home") + "/obsidian-vault";
        this.dbPath = System.getProperty("user.home") + "/.rag-reader/index.db";
        this.chunkSize = 500;
        this.chunkOverlap = 50;
        this.embeddingDimension = 384; // AllMiniLmL6V2
        this.mcpHost = "127.0.0.1";
        this.mcpPort = 8080;
    }
    
    public void loadFromFile(String configPath) {
        File configFile = new File(configPath);
        if (!configFile.exists()) {
            logger.warn("Config file not found: {}, using defaults", configPath);
            return;
        }
        
        try (InputStream input = new FileInputStream(configFile)) {
            Yaml yaml = new Yaml();
            Map<String, Object> config = yaml.load(input);
            
            if (config.containsKey("vaultPath")) {
                this.vaultPath = (String) config.get("vaultPath");
            }
            if (config.containsKey("dbPath")) {
                this.dbPath = (String) config.get("dbPath");
            }
            if (config.containsKey("chunkSize")) {
                this.chunkSize = (Integer) config.get("chunkSize");
            }
            if (config.containsKey("chunkOverlap")) {
                this.chunkOverlap = (Integer) config.get("chunkOverlap");
            }
            if (config.containsKey("embeddingDimension")) {
                this.embeddingDimension = (Integer) config.get("embeddingDimension");
            }
            if (config.containsKey("mcpHost")) {
                this.mcpHost = (String) config.get("mcpHost");
            }
            if (config.containsKey("mcpPort")) {
                this.mcpPort = (Integer) config.get("mcpPort");
            }
            
            logger.info("Loaded config from {}", configPath);
        } catch (Exception e) {
            logger.error("Failed to load config from {}", configPath, e);
        }
    }
    
    // Getters
    public String getVaultPath() { return vaultPath; }
    public String getDbPath() { return dbPath; }
    public int getChunkSize() { return chunkSize; }
    public int getChunkOverlap() { return chunkOverlap; }
    public int getEmbeddingDimension() { return embeddingDimension; }
    public String getMcpHost() { return mcpHost; }
    public int getMcpPort() { return mcpPort; }
    
    public Path getVaultPathAsPath() { return Paths.get(vaultPath); }
    public Path getDbPathAsPath() { return Paths.get(dbPath); }
}
