# RAG-Reader 开发进度

> 最后更新：2026-10-24

## 总体进度

**Phase 1: Vault RAG 基础** - ✅ 代码完成，待验证

## Phase 1 详细进度

### 已完成

#### 1. 项目结构 ✅
- [x] Maven 项目配置（pom.xml）
- [x] 依赖管理（LangChain4j, directory-watcher, Jackson）
- [x] 包结构：config, model, chunker, store, ingestor, mcp, watcher

#### 2. 配置管理 ✅
- [x] `RagReaderConfig.java` - YAML 配置加载
- [x] `config.example.yaml` - 配置模板
- [x] 支持命令行参数覆盖

#### 3. 数据模型 ✅
- [x] `NoteMetadata.java` - frontmatter 元数据
- [x] `NoteChunk.java` - 笔记 chunk 模型

#### 4. Obsidian-aware 切分器 ✅
- [x] `ObsidianMarkdownChunker.java`
  - frontmatter 解析（type/tags/source/created_at）
  - 按 heading 层级切分
  - wikilink 提取（[[原文]]）
  - 大文件按段落切分（带 overlap）
  - content hash 计算（SHA-256）

#### 5. 向量存储 ✅
- [x] `NoteEmbeddingStore.java`
  - InMemoryEmbeddingStore（可切换 Milvus）
  - AllMiniLmL6V2EmbeddingModel（本地 ONNX，384维）
  - 文件 hash 索引（增量检测）
  - chunk 索引（快速查找）
  - 语义搜索（cosine similarity）

#### 6. 摄入管道 ✅
- [x] `VaultIngestor.java`
  - 扫描 vault 目录（递归）
  - 增量索引（基于 content hash）
  - 文件级重新索引
  - 跳过隐藏目录

#### 7. MCP Server ✅
- [x] `McpServer.java`
  - HTTP JSON-RPC 协议
  - tools/list - 列出可用工具
  - tools/call - 调用工具
  - search_notes - 语义搜索
  - get_note - 读取笔记
  - list_notes - 列出笔记

#### 8. 文件监听 ✅
- [x] `VaultWatcher.java`
  - directory-watcher 库
  - 监听 CREATE/MODIFY/DELETE 事件
  - 自动触发重新索引
  - 守护线程运行

#### 9. 主应用 ✅
- [x] `RagReaderApplication.java`
  - 启动流程：配置加载 → 初始索引 → 启动 watcher → 启动 MCP
  - 关闭钩子（shutdown hook）
  - 命令行参数处理

#### 10. 文档 ✅
- [x] README.md - 使用说明
- [x] config.example.yaml - 配置示例
- [x] 架构图
- [x] MCP 工具说明

### 待验证

#### 编译测试
```bash
mvn clean package
```

#### 功能测试
1. 创建测试 vault
2. 添加几个 markdown 文件（含 frontmatter）
3. 运行应用
4. 验证索引是否成功
5. 测试 MCP 工具调用

#### AI 客户端集成
1. Claude Desktop 配置
2. Cursor 配置
3. 测试语义搜索

## 技术决策记录

### 1. 向量库选择
**决策**：先用 InMemoryEmbeddingStore，后续可切换 Milvus

**理由**：
- 快速跑通原型
- 无需额外部署
- LangChain4j 接口统一，切换成本低

**迁移路径**：
```java
// 当前
EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();

// 切换到 Milvus
EmbeddingStore<TextSegment> store = MilvusEmbeddingStore.builder()
    .uri("http://localhost:19530")
    .collectionName("notes")
    .dimension(384)
    .build();
```

### 2. Embedding 模型选择
**决策**：AllMiniLmL6V2（本地 ONNX）

**理由**：
- 完全本地，无需外部 API
- 384 维，平衡性能和效果
- LangChain4j 原生支持

**备选**：
- Ollama nomic-embed-text（768维，更强但更慢）
- OpenAI text-embedding-3-small（云端，需 API key）

### 3. MCP 协议实现
**决策**：HTTP JSON-RPC，自行实现

**理由**：
- LangChain4j MCP 模块主要是客户端
- HTTP 方式便于调试和集成
- 标准 JSON-RPC 2.0 协议

**工具定义**：
- search_notes(query, limit)
- get_note(path)
- list_notes()

### 4. 切分策略
**决策**：按 heading 层级切分

**理由**：
- 符合 Obsidian 笔记结构
- 保留语义完整性
- 便于上下文理解

**参数**：
- chunkSize: 500 字符
- chunkOverlap: 50 字符

## 已知问题

### 1. InMemoryEmbeddingStore 不持久化
**问题**：重启后索引丢失

**解决**：
- 启动时重新扫描 vault
- 基于 content hash 增量索引
- 后续可实现序列化或切换 Milvus

### 2. 大文件处理
**问题**：单个 heading 下内容过长

**解决**：
- 按段落切分
- 带 overlap 保持上下文
- 可调整 chunkSize 参数

### 3. 非 Markdown 文件
**问题**：当前只处理 .md 文件

**解决**：
- Phase 2 可集成 Code-RAG 处理代码
- 可扩展支持 PDF、HTML 等

## 下一步

### Phase 2: Code-RAG 集成
- [ ] 部署 Code-RAG（Java + pgvector）
- [ ] 验证符号级切分
- [ ] 统一 MCP 入口

### Phase 3: 主动推送集成
- [ ] 安装 ProactiveAgent
- [ ] 配置克制参数
- [ ] 测试主动建议

### Phase 4: 小红书同步
- [ ] 安装 Obsidian 插件
- [ ] 跑通全量导出
- [ ] 配置增量同步

## 参考资源

### 项目文档
- [产品提案](./app-proposal.md)
- [批判性分析](./critique-revised-proposal.md)
- [GitHub 项目调研](./research-github-projects.md)

### 参考项目
- Code-RAG: Java 代码 RAG，符号级切分
- cortex: Python Obsidian vault RAG
- vault-search: Python 混合检索
- langchain4j-examples: LangChain4j 示例集合

### 技术文档
- LangChain4j: https://langchain4j.dev/
- MCP 协议: https://modelcontextprotocol.io/
- directory-watcher: https://github.com/gmethvin/directory-watcher
