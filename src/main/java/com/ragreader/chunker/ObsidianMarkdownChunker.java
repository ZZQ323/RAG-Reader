package com.ragreader.chunker;

import com.ragreader.model.NoteChunk;
import com.ragreader.model.NoteMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Obsidian-aware Markdown chunker
 * 
 * Splits markdown files by heading hierarchy, preserving:
 * - frontmatter (type/tags/source)
 * - wikilinks ([[原文]])
 * - heading hierarchy
 * - code blocks and LaTeX
 */
public class ObsidianMarkdownChunker {
    private static final Logger logger = LoggerFactory.getLogger(ObsidianMarkdownChunker.class);
    
    private static final Pattern FRONTMATTER_PATTERN = Pattern.compile("^---\\s*\\n(.*?)\\n---\\s*\\n", Pattern.DOTALL);
    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);
    private static final Pattern WIKILINK_PATTERN = Pattern.compile("\\[\\[([^\\]]+)\\]\\]");
    
    private final int chunkSize;
    private final int chunkOverlap;
    
    public ObsidianMarkdownChunker(int chunkSize, int chunkOverlap) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }
    
    /**
     * Chunk a single markdown file
     */
    public List<NoteChunk> chunkFile(Path filePath) throws IOException {
        String content = Files.readString(filePath);
        Instant modifiedAt = Files.getLastModifiedTime(filePath).toInstant();
        String contentHash = sha256(content);
        
        // Extract frontmatter
        NoteMetadata metadata = extractFrontmatter(content);
        String body = removeFrontmatter(content);
        
        // Split by headings
        List<Section> sections = splitByHeadings(body);
        
        // Generate chunks
        List<NoteChunk> chunks = new ArrayList<>();
        int chunkIndex = 0;
        
        for (Section section : sections) {
            // If section is small enough, keep it as one chunk
            if (section.content.length() <= chunkSize) {
                NoteChunk chunk = createChunk(filePath, section, metadata, modifiedAt, contentHash, chunkIndex++);
                chunks.add(chunk);
            } else {
                // Split large section by paragraphs with overlap
                List<String> paragraphs = splitByParagraphs(section.content);
                StringBuilder current = new StringBuilder();
                
                for (String para : paragraphs) {
                    if (current.length() + para.length() > chunkSize && current.length() > 0) {
                        // Emit current chunk
                        NoteChunk chunk = createChunk(filePath, section, metadata, modifiedAt, contentHash, chunkIndex++);
                        chunk.setContent(current.toString());
                        chunks.add(chunk);
                        
                        // Keep overlap
                        String text = current.toString();
                        int overlapStart = Math.max(0, text.length() - chunkOverlap);
                        current = new StringBuilder(text.substring(overlapStart));
                    }
                    current.append(para).append("\n\n");
                }
                
                // Emit remaining
                if (current.length() > 0) {
                    NoteChunk chunk = createChunk(filePath, section, metadata, modifiedAt, contentHash, chunkIndex++);
                    chunk.setContent(current.toString());
                    chunks.add(chunk);
                }
            }
        }
        
        return chunks;
    }
    
    private NoteChunk createChunk(Path filePath, Section section, NoteMetadata metadata, 
                                   Instant modifiedAt, String contentHash, int index) {
        NoteChunk chunk = new NoteChunk();
        chunk.setId(filePath.toString() + "#" + index);
        chunk.setNotePath(filePath);
        chunk.setHeading(section.heading);
        chunk.setContent(section.content);
        chunk.setMetadata(metadata);
        chunk.setWikilinks(extractWikilinks(section.content));
        chunk.setFileModifiedAt(modifiedAt);
        chunk.setContentHash(contentHash);
        return chunk;
    }
    
    private NoteMetadata extractFrontmatter(String content) {
        NoteMetadata metadata = new NoteMetadata();
        
        Matcher matcher = FRONTMATTER_PATTERN.matcher(content);
        if (matcher.find()) {
            String yaml = matcher.group(1);
            try {
                Yaml yamlParser = new Yaml();
                Map<String, Object> map = yamlParser.load(yaml);
                
                if (map.containsKey("type")) {
                    metadata.setType((String) map.get("type"));
                }
                if (map.containsKey("tags")) {
                    Object tags = map.get("tags");
                    if (tags instanceof List) {
                        metadata.setTags((List<String>) tags);
                    } else if (tags instanceof String) {
                        metadata.setTags(Arrays.asList(((String) tags).split(",")));
                    }
                }
                if (map.containsKey("source")) {
                    metadata.setSource((String) map.get("source"));
                }
                if (map.containsKey("created_at")) {
                    Object date = map.get("created_at");
                    if (date instanceof java.util.Date) {
                        metadata.setCreatedAt(((java.util.Date) date).toInstant());
                    }
                }
                
                // Store extra fields
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    String key = entry.getKey();
                    if (!key.equals("type") && !key.equals("tags") && !key.equals("source") && !key.equals("created_at")) {
                        metadata.putExtra(key, entry.getValue());
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to parse frontmatter: {}", e.getMessage());
            }
        }
        
        return metadata;
    }
    
    private String removeFrontmatter(String content) {
        return FRONTMATTER_PATTERN.matcher(content).replaceFirst("");
    }
    
    private List<Section> splitByHeadings(String content) {
        List<Section> sections = new ArrayList<>();
        Matcher matcher = HEADING_PATTERN.matcher(content);
        
        int lastEnd = 0;
        String currentHeading = "";
        
        while (matcher.find()) {
            // Add previous section
            if (matcher.start() > lastEnd) {
                String sectionContent = content.substring(lastEnd, matcher.start()).trim();
                if (!sectionContent.isEmpty()) {
                    sections.add(new Section(currentHeading, sectionContent));
                }
            }
            
            // Start new section
            currentHeading = matcher.group(0); // full heading line
            lastEnd = matcher.end();
        }
        
        // Add final section
        if (lastEnd < content.length()) {
            String sectionContent = content.substring(lastEnd).trim();
            if (!sectionContent.isEmpty()) {
                sections.add(new Section(currentHeading, sectionContent));
            }
        }
        
        // If no headings found, treat entire content as one section
        if (sections.isEmpty() && !content.trim().isEmpty()) {
            sections.add(new Section("", content.trim()));
        }
        
        return sections;
    }
    
    private List<String> splitByParagraphs(String content) {
        List<String> paragraphs = new ArrayList<>();
        String[] parts = content.split("\n\n+");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                paragraphs.add(trimmed + "\n\n");
            }
        }
        return paragraphs;
    }
    
    private List<String> extractWikilinks(String content) {
        List<String> links = new ArrayList<>();
        Matcher matcher = WIKILINK_PATTERN.matcher(content);
        while (matcher.find()) {
            links.add(matcher.group(1));
        }
        return links;
    }
    
    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
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
    
    private static class Section {
        String heading;
        String content;
        
        Section(String heading, String content) {
            this.heading = heading;
            this.content = content;
        }
    }
}
