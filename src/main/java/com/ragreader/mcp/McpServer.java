package com.ragreader.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ragreader.model.NoteChunk;
import com.ragreader.store.NoteEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

/**
 * MCP Server exposing search tools via HTTP JSON-RPC
 * Implements Model Context Protocol for AI client integration
 */
public class McpServer {
    private static final Logger logger = LoggerFactory.getLogger(McpServer.class);
    
    private final String host;
    private final int port;
    private final NoteEmbeddingStore store;
    private final ObjectMapper mapper;
    private HttpServer server;
    
    public McpServer(String host, int port, NoteEmbeddingStore store) {
        this.host = host;
        this.port = port;
        this.store = store;
        this.mapper = new ObjectMapper();
    }
    
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(host, port), 0);
        server.createContext("/mcp", new McpHandler());
        server.setExecutor(null);
        server.start();
        
        logger.info("MCP Server started at http://{}:{}/mcp", host, port);
    }
    
    public void stop() {
        if (server != null) {
            server.stop(0);
            logger.info("MCP Server stopped");
        }
    }
    
    private class McpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }
            
            try (InputStream is = exchange.getRequestBody()) {
                JsonNode request = mapper.readTree(is);
                JsonNode response = processRequest(request);
                
                String responseJson = mapper.writeValueAsString(response);
                byte[] responseBytes = responseJson.getBytes("UTF-8");
                
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, responseBytes.length);
                
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            } catch (Exception e) {
                logger.error("Error processing MCP request", e);
                sendError(exchange, 500, "Internal error: " + e.getMessage());
            }
        }
        
        private JsonNode processRequest(JsonNode request) {
            String method = request.path("method").asText();
            JsonNode params = request.path("params");
            String id = request.path("id").asText();
            
            ObjectNode response = mapper.createObjectNode();
            response.put("jsonrpc", "2.0");
            response.put("id", id);
            
            try {
                switch (method) {
                    case "tools/list":
                        response.set("result", listTools());
                        break;
                    case "tools/call":
                        response.set("result", callTool(params));
                        break;
                    default:
                        ObjectNode error = mapper.createObjectNode();
                        error.put("code", -32601);
                        error.put("message", "Method not found: " + method);
                        response.set("error", error);
                }
            } catch (Exception e) {
                logger.error("Error processing method: {}", method, e);
                ObjectNode error = mapper.createObjectNode();
                error.put("code", -32603);
                error.put("message", "Internal error: " + e.getMessage());
                response.set("error", error);
            }
            
            return response;
        }
        
        private JsonNode listTools() {
            ObjectNode result = mapper.createObjectNode();
            ArrayNode tools = mapper.createArrayNode();
            
            // search_notes tool
            ObjectNode searchTool = mapper.createObjectNode();
            searchTool.put("name", "search_notes");
            searchTool.put("description", "Semantic search over Obsidian vault notes");
            ObjectNode searchInput = mapper.createObjectNode();
            searchInput.put("type", "object");
            ObjectNode searchProps = mapper.createObjectNode();
            
            ObjectNode queryProp = mapper.createObjectNode();
            queryProp.put("type", "string");
            queryProp.put("description", "Search query");
            searchProps.set("query", queryProp);
            
            ObjectNode limitProp = mapper.createObjectNode();
            limitProp.put("type", "integer");
            limitProp.put("description", "Maximum number of results (default: 5)");
            searchProps.set("limit", limitProp);
            
            searchInput.set("properties", searchProps);
            searchInput.putArray("required").add("query");
            searchTool.set("inputSchema", searchInput);
            tools.add(searchTool);
            
            // get_note tool
            ObjectNode getNoteTool = mapper.createObjectNode();
            getNoteTool.put("name", "get_note");
            getNoteTool.put("description", "Get full content of a note by path");
            ObjectNode getNoteInput = mapper.createObjectNode();
            getNoteInput.put("type", "object");
            ObjectNode getNoteProps = mapper.createObjectNode();
            
            ObjectNode pathProp = mapper.createObjectNode();
            pathProp.put("type", "string");
            pathProp.put("description", "Path to the note file");
            getNoteProps.set("path", pathProp);
            
            getNoteInput.set("properties", getNoteProps);
            getNoteInput.putArray("required").add("path");
            getNoteTool.set("inputSchema", getNoteInput);
            tools.add(getNoteTool);
            
            // list_notes tool
            ObjectNode listNotesTool = mapper.createObjectNode();
            listNotesTool.put("name", "list_notes");
            listNotesTool.put("description", "List all indexed notes");
            ObjectNode listNotesInput = mapper.createObjectNode();
            listNotesInput.put("type", "object");
            listNotesTool.set("inputSchema", listNotesInput);
            tools.add(listNotesTool);
            
            result.set("tools", tools);
            return result;
        }
        
        private JsonNode callTool(JsonNode params) throws Exception {
            String toolName = params.path("name").asText();
            JsonNode arguments = params.path("arguments");
            
            ObjectNode result = mapper.createObjectNode();
            ArrayNode content = mapper.createArrayNode();
            
            switch (toolName) {
                case "search_notes":
                    content.add(searchNotes(arguments));
                    break;
                case "get_note":
                    content.add(getNote(arguments));
                    break;
                case "list_notes":
                    content.add(listNotes());
                    break;
                default:
                    throw new IllegalArgumentException("Unknown tool: " + toolName);
            }
            
            result.set("content", content);
            return result;
        }
        
        private JsonNode searchNotes(JsonNode arguments) throws Exception {
            String query = arguments.path("query").asText();
            int limit = arguments.path("limit").asInt(5);
            
            List<NoteEmbeddingStore.SearchResult> results = store.search(query, limit, 0.5);
            
            StringBuilder sb = new StringBuilder();
            sb.append("Found ").append(results.size()).append(" results for: ").append(query).append("\n\n");
            
            for (int i = 0; i < results.size(); i++) {
                NoteEmbeddingStore.SearchResult result = results.get(i);
                NoteChunk chunk = result.getChunk();
                
                sb.append("## Result ").append(i + 1).append(" (score: ")
                  .append(String.format("%.2f", result.getScore())).append(")\n");
                sb.append("**Path**: ").append(chunk.getNotePath()).append("\n");
                
                if (chunk.getHeading() != null && !chunk.getHeading().isEmpty()) {
                    sb.append("**Heading**: ").append(chunk.getHeading()).append("\n");
                }
                
                if (chunk.getMetadata() != null && chunk.getMetadata().getTags() != null) {
                    sb.append("**Tags**: ").append(String.join(", ", chunk.getMetadata().getTags())).append("\n");
                }
                
                sb.append("\n").append(chunk.getContent()).append("\n\n---\n\n");
            }
            
            ObjectNode node = mapper.createObjectNode();
            node.put("type", "text");
            node.put("text", sb.toString());
            return node;
        }
        
        private JsonNode getNote(JsonNode arguments) throws Exception {
            String pathStr = arguments.path("path").asText();
            Path path = Path.of(pathStr);
            
            if (!Files.exists(path)) {
                throw new IllegalArgumentException("File not found: " + pathStr);
            }
            
            String content = Files.readString(path);
            
            ObjectNode node = mapper.createObjectNode();
            node.put("type", "text");
            node.put("text", content);
            return node;
        }
        
        private JsonNode listNotes() {
            var indexedFiles = store.getIndexedFiles();
            
            StringBuilder sb = new StringBuilder();
            sb.append("Indexed notes (").append(indexedFiles.size()).append(" files):\n\n");
            
            for (String file : indexedFiles) {
                sb.append("- ").append(file).append("\n");
            }
            
            ObjectNode node = mapper.createObjectNode();
            node.put("type", "text");
            node.put("text", sb.toString());
            return node;
        }
        
        private void sendError(HttpExchange exchange, int code, String message) throws IOException {
            String response = "{\"error\": \"" + message + "\"}";
            byte[] bytes = response.getBytes("UTF-8");
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(code, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}
