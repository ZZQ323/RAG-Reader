# RAG-Reader

个人知识管理系统，让 AI 像老师一样在你需要时提醒你读过的东西。

## 功能特性

- **Obsidian-aware 切分器**：按 heading 层级切分，保留 frontmatter、wikilink、代码块
- **增量索引**：基于内容 hash，只索引变化的文件
- **语义搜索**：本地 embedding 模型（AllMiniLmL6V2），无需外部 API
- **MCP Server**：暴露 search_notes、get_note、list_notes 工具，可被 AI 客户端调用
- **文件监听**：自动检测 vault 变化，实时重新索引

## 快速开始

### 1. 编译

```bash
mvn clean package
```

### 2. 配置

复制配置文件并修改：

```bash
cp config.example.yaml ~/.rag-reader/config.yaml
# 编辑 ~/.rag-reader/config.yaml，设置 vaultPath
```

### 3. 运行

```bash
java -jar target/rag-reader-1.0.0-SNAPSHOT.jar ~/.rag-reader/config.yaml
```

或指定 vault 路径：

```bash
java -jar target/rag-reader-1.0.0-SNAPSHOT.jar ~/.rag-reader/config.yaml /path/to/vault
```

### 4. 在 AI 客户端中配置 MCP

在 Claude Desktop 的 `claude_desktop_config.json` 中添加：

```json
{
  "mcpServers": {
    "rag-reader": {
      "command": "java",
      "args": ["-jar", "/path/to/rag-reader-1.0.0-SNAPSHOT.jar", "~/.rag-reader/config.yaml"]
    }
  }
}
```

或在 Cursor 中配置 HTTP MCP：

```json
{
  "mcpServers": {
    "rag-reader": {
      "url": "http://127.0.0.1:8080/mcp"
    }
  }
}
```

## MCP 工具

### search_notes

语义搜索笔记。

**参数**：
- `query` (string, required): 搜索查询
- `limit` (integer, optional): 最大结果数（默认 5）

**示例**：
```
search_notes(query="Agent 设计模式", limit=5)
```

### get_note

获取笔记完整内容。

**参数**：
- `path` (string, required): 笔记文件路径

**示例**：
```
get_note(path="/path/to/vault/notes/agent-patterns.md")
```

### list_notes

列出所有已索引的笔记。

**示例**：
```
list_notes()
```

## 架构

```
┌─────────────────────────────────────────────────────────┐
│  AI 客户端 (Claude/Cursor)                               │
└────────────────┬────────────────────────────────────────┘
                 │ MCP (HTTP JSON-RPC)
                 ▼
┌─────────────────────────────────────────────────────────┐
│  MCP Server                                              │
│  - search_notes                                          │
│  - get_note                                              │
│  - list_notes                                            │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│  NoteEmbeddingStore                                      │
│  - InMemoryEmbeddingStore (可切换 Milvus)                 │
│  - AllMiniLmL6V2EmbeddingModel (本地 ONNX)               │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│  VaultIngestor                                           │
│  - ObsidianMarkdownChunker (按 heading 切分)             │
│  - 增量索引 (基于 content hash)                           │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│  VaultWatcher                                            │
│  - 监听 vault 目录变化                                    │
│  - 自动触发重新索引                                       │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│  Obsidian Vault (Markdown 文件)                          │
│  - frontmatter (type/tags/source)                        │
│  - wikilinks ([[原文]])                                  │
│  - heading 层级                                          │
└─────────────────────────────────────────────────────────┘
```

## 配置说明

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `vaultPath` | `~/obsidian-vault` | Obsidian vault 路径 |
| `dbPath` | `~/.rag-reader/index.db` | 索引数据库路径 |
| `chunkSize` | `500` | 每个 chunk 的最大字符数 |
| `chunkOverlap` | `50` | chunk 之间的重叠字符数 |
| `embeddingDimension` | `384` | Embedding 维度（AllMiniLmL6V2） |
| `mcpHost` | `127.0.0.1` | MCP Server 主机 |
| `mcpPort` | `8080` | MCP Server 端口 |

## 开发计划

### Phase 1: Vault RAG 基础 ✅

- [x] Obsidian-aware 切分器
- [x] 摄入管道（扫描、增量索引、embedding）
- [x] MCP Server（search_notes, get_note, list_notes）
- [x] 文件监听（自动触发增量索引）

### Phase 2: Code-RAG 集成

- [ ] 部署 Code-RAG（Java + pgvector）
- [ ] 验证符号级切分
- [ ] 接入 AI 客户端

### Phase 3: 主动推送集成

- [ ] 安装 ProactiveAgent
- [ ] 配置克制参数
- [ ] 测试主动建议

### Phase 4: 小红书同步

- [ ] 安装 Obsidian 插件
- [ ] 跑通全量导出
- [ ] 配置增量同步

## 技术栈

- **语言**: Java 21
- **RAG 框架**: LangChain4j
- **Embedding**: AllMiniLmL6V2 (本地 ONNX)
- **向量库**: InMemoryEmbeddingStore (可切换 Milvus)
- **文件监听**: directory-watcher
- **MCP**: HTTP JSON-RPC

## License

MIT
