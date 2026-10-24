# GitHub 现成项目调研

> 调研日期：2026-10-23
> 目标：寻找可直接复用或参考的开源项目，覆盖三个场景
> 场景：① 读书笔记 comment ② 读项目代码的 comment ③ 速成学科笔记
> 主题：金融、软工、代码原理 等

---

## 一、代码阅读 + RAG（场景② 最相关）

### 1.1 blakemcbride/Code-RAG ⭐⭐⭐⭐⭐ 强烈推荐

| 项 | 值 |
|---|---|
| URL | https://github.com/blakemcbride/Code-RAG |
| 语言 | **Java 21+** |
| 许可 | BSD-2-Clause |
| 创建 | 2026-05 |
| 核心 | 本地 RAG + MCP Server，为 Claude Code / Codex 设计 |

**为什么对口**：
- **Java 技术栈**，和你的选型完全一致
- 符号级切分（symbol-aware chunking），支持 Java/Groovy/JS/TS/Kotlin/Scala/C#/Swift/Dart/C/Python/Ruby/PHP/Rust/Go/Elixir/Lisp
- pgvector + HNSW 做余弦搜索
- 多项目隔离（一个 PostgreSQL schema 对应一个项目）
- Cron sweep 增量索引
- 通过 MCP 暴露给 AI 客户端

**可复用点**：
- 符号级切分策略（比固定窗口切分好得多）
- MCP Server 的 Java 实现
- 多项目隔离的 schema 设计

**不足**：
- 需要 PostgreSQL 17+ + pgvector（部署门槛稍高）
- 不含 VSCode 插件（纯 CLI + MCP）

---

### 1.2 ami3g/GitLore ⭐⭐⭐⭐

| 项 | 值 |
|---|---|
| URL | https://github.com/ami3g/GitLore |
| 语言 | TypeScript + Electron |
| 许可 | MIT |
| 核心 | 代码 + commit + PR 统一索引，LanceDB，**有 VSCode 扩展** |

**亮点**：
- 10 阶段检索管线（意图分类 → 三路搜索 → 重排 → 蒸馏）
- AST 切分（Tree-sitter 支持 9 种语言，含 Java）
- VSCode 扩展：右键行 → blame → RAG 查询
- 增量索引（SHA-256 content hash）
- 隐私优先：embedding 全本地

**可复用点**：
- VSCode 扩展的交互模式（右键 blame → RAG）
- 10 阶段检索管线设计
- commit + PR 索引（理解"为什么这样写"）

---

### 1.3 kirtijha/code-historian ⭐⭐⭐⭐

| 项 | 值 |
|---|---|
| URL | https://github.com/kirtijha/code-historian |
| 语言 | TypeScript |
| 许可 | MIT |
| 核心 | VSCode 扩展，代码变更实时追踪 + RAG 语义搜索，**已上架 Marketplace** |

**亮点**：
- 实时捕获代码变更（create/modify/delete/rename）
- 混合搜索：向量相似度 + BM25 关键词
- VSCode Chat 集成（`@historian` 命令）
- 支持 HuggingFace / Ollama / OpenAI 三种 embedding
- 已上架 VSCode Marketplace，可直接安装体验

**可复用点**：
- 代码变更实时捕获机制
- VSCode Chat 集成方式
- 混合搜索管线

---

### 1.4 FrankC7/Annotate.ai ⭐⭐⭐

| 项 | 值 |
|---|---|
| URL | https://github.com/FrankC7/Annotate.ai |
| 语言 | TypeScript（VSCode 扩展） |
| 核心 | AI 批注 + RAG（Astra DB），代码悬停解释 |

**亮点**：
- 选中文本 → AI 批注
- 悬停解释（hover explainer）：鼠标悬停自动解释代码行
- 用 RAG 索引整个 workspace
- commit 历史 RAG（学习你的 commit 风格）

**不足**：
- 依赖 Astra DB（云向量库），不够本地化
- 依赖 Groq（云端 LLM）

---

### 1.5 Bonus-Hunters/Mind-Trace ⭐⭐⭐

| 项 | 值 |
|---|---|
| URL | https://github.com/Bonus-Hunters/Mind-Trace |
| 语言 | TypeScript（VSCode）+ Python（FastAPI 后端） |
| 核心 | 代码批注 + 会议笔记，PostgreSQL + pgvector + Ollama |

**亮点**：
- 三层架构：VSCode 扩展 → Webview UI → FastAPI 后端
- 选中文本 → 捕获函数/文件/行号 → 快速笔记
- 混合搜索：向量 + 全文 + trigram
- 全本地（PostgreSQL + Ollama）

**可复用点**：
- 三层架构设计（前端/后端/数据库分离）
- 代码上下文自动捕获（函数名、文件、行号）

---

### 1.6 vitali87/code-graph-rag ⭐⭐⭐⭐

| 项 | 值 |
|---|---|
| URL | https://github.com/vitali87/code-graph-rag |
| 语言 | Python |
| 核心 | Tree-sitter + Memgraph 图数据库，知识图谱，多语言 |

**亮点**：
- 不只是向量搜索，而是构建**代码知识图谱**
- 函数/类/方法/模块 → 图节点，调用关系 → 图边
- 自然语言 → Cypher 查询
- 支持代码编辑（AST-based surgical patching）
- 支持 16+ 种语言

**可复用点**：
- 知识图谱思路（比纯向量搜索更适合代码理解）
- Tree-sitter 多语言解析

---

## 二、Obsidian Vault RAG（场景①③ 最相关）

### 2.1 Gustav-Proxi/cortex ⭐⭐⭐⭐⭐ 最全面

| 项 | 值 |
|---|---|
| URL | https://github.com/Gustav-Proxi/cortex |
| 语言 | Python |
| 许可 | Apache 2.0 |
| 核心 | **33 个 MCP 工具**，搜索/读写/图智能/模板/研究 |

**为什么最全面**：
- 搜索：语义/词法/混合/frontmatter 过滤/wikilink 图
- 读写：完整 CRUD + frontmatter + 模板 + daily notes
- 图智能：最短路径、hub 节点、社区检测、语义桥
- 研究：DOI/arXiv → APA + BibTeX、deadline 提取
- 超越 Markdown：可索引 PDF 和源代码
- watcher 2 秒内重索引

**可复用点**：
- MCP 工具设计（33 个工具的完整 API 设计）
- 图智能（语义桥检测——两篇笔记语义相关但没有 wikilink）
- 超越 Markdown 的索引（PDF + 代码）

---

### 2.2 drpwchen/vault-search ⭐⭐⭐⭐⭐

| 项 | 值 |
|---|---|
| URL | https://github.com/drpwchen/vault-search |
| 语言 | Python + Obsidian 插件 |
| 核心 | LanceDB + Ollama，**Personalized PageRank** 图检索 |

**亮点**：
- 三合一：Obsidian 插件 + MCP Server + 共享索引
- Personalized PageRank 在 wikilink 图上做随机游走
- 路径权重（信任某些文件夹）、时间衰减、关系增强
- 多语言支持（bge-m3 embedding）
- 教材索引（parent-child chunking）

**可复用点**：
- Personalized PageRank 检索策略
- 路径权重 + 时间衰减 + 关系增强的综合排序
- 教材索引模式（适合"速成学科"场景）

---

### 2.3 flowing-abyss/obsidian-hybrid-search ⭐⭐⭐⭐

| 项 | 值 |
|---|---|
| URL | https://github.com/flowing-abyss/obsidian-hybrid-search |
| 语言 | TypeScript (npm) |
| 核心 | BM25 + 语义 + 模糊标题 + RRF，**跨编码器重排** |

**亮点**：
- 四种搜索模式：hybrid / semantic / fulltext / title
- 别名搜索（frontmatter aliases）
- 多查询扇出（多个 query 并行搜索，RRF 合并）
- 跨编码器重排（gte-multilingual-reranker-base）
- 增量索引 + 实时监听

**可复用点**：
- 多查询扇出 + RRF 合并
- 跨编码器重排（提升精度）
- 别名搜索

---

### 2.4 sweir1/obsidian-brain ⭐⭐⭐⭐

| 项 | 值 |
|---|---|
| URL | https://github.com/sweir1/obsidian-brain |
| 语言 | Node.js |
| 许可 | Apache 2.0 |
| 核心 | **PageRank + Louvain 社区检测**，18 个 MCP 工具 |

**亮点**：
- 唯一一个带图分析（PageRank、Louvain、主题聚类）的 Obsidian MCP
- heading-aware 递归切分（保留代码块 + LaTeX）
- RRF 混合检索（语义 + FTS5 BM25）
- 一键 `npx` 安装

---

### 2.5 junaidrahim/smolbren ⭐⭐⭐⭐

| 项 | 值 |
|---|---|
| URL | https://github.com/junaidrahim/smolbren |
| 语言 | （未详述） |
| 核心 | **本体优先**：frontmatter type → 节点类型，wikilink → 图边 |

**亮点**：
- 不从 embedding 出发，而是从你已有的 frontmatter 结构出发
- type 字段 → 节点类型，wikilink 字段 → 类型化边
- embedding 叠加在上层（EmbeddingGemma-300M via ONNX，全本地）
- LanceDB 存储，增量索引（blake3 hash）
- 命令行为 agent 优化（单行 JSON 输出）

**可复用点**：
- 本体优先的设计哲学（和你的 frontmatter 设计一致）
- agent-friendly 的 CLI 设计

---

### 2.6 sysangel/brain-mcp ⭐⭐⭐⭐

| 项 | 值 |
|---|---|
| URL | https://github.com/sysangel/brain-mcp |
| 语言 | Python |
| 许可 | MIT |
| 核心 | SQLite FTS5 + sqlite-vec，混合 RAG，**git-backed** |

**亮点**：
- 三类记忆：curated facts / session digests / episodes（Claude Code 转录）
- 索引是缓存，可随时重建
- 混合检索：FTS5/BM25 + sqlite-vec（256-d nomic-embed）
- RRF 融合 + 先验权重（curated > episode）+ 时间衰减
- git 自动提交，可推送到私有远程

**可复用点**：
- 三类记忆分层设计
- "索引是缓存"的哲学
- git-backed 持久化

---

### 2.7 mplind/second-reader ⭐⭐⭐

| 项 | 值 |
|---|---|
| URL | https://github.com/mplind/second-reader |
| 核心 | 读书 → vault，**校验原文**，去重去水 |

**亮点**：
- 给 5 本书 → 提取知识 → 去重去填充 → 校验原文 → 一个 vault
- 每个事实都引用出处
- 解释标为解释，事实标为事实
- 适合"速成学科"场景：读几本教材 → 生成统一知识库

---

### 2.8 ebarti/llm-kb ⭐⭐⭐

| 项 | 值 |
|---|---|
| URL | https://github.com/ebarti/llm-kb |
| 核心 | Claude 做研究 agent，自动维护 wiki |

**亮点**：
- 三个操作：research / ingest / ask
- research：一个 prompt → 10-15 个新 wiki 页面
- ingest：URL/arXiv/YouTube/GitHub/PDF/推文 → 清洗 + frontmatter + 索引
- ask：带引用的回答，可回填 wiki
- 原始数据不可变；wiki 是 LLM 的编译解释

---

## 三、Java RAG 框架（技术参考）

### 3.1 RAG4J/rag4j

| 项 | 值 |
|---|---|
| URL | https://github.com/RAG4J/rag4j |
| 许可 | Apache 2.0 |
| 核心 | Java RAG 教学框架，一小时读完 |

**模块**：Indexing / Embedding / Retrieval（TopN, Window, Hierarchical, Document）/ Generation / Store（Weaviate）/ Quality

**适合**：学习 RAG 各组件，快速实验不同策略

---

### 3.2 aaronshan/ragflow4j

| 项 | 值 |
|---|---|
| URL | https://github.com/aaronshan/ragflow4j |
| 许可 | MIT |
| 核心 | Java 版 RAG 引擎 |

---

### 3.3 Loom-AI-Labs/ai-fabric-framework

| 项 | 值 |
|---|---|
| URL | https://github.com/Loom-AI-Labs/ai-fabric-framework |
| 核心 | Spring AI + Spring Boot 4.1, Java 21 |

---

## 四、场景匹配矩阵

| 场景 | 最佳匹配 | 次选 |
|------|---------|------|
| ① 读书笔记 comment | cortex, second-reader, llm-kb | brain-mcp, smolbren |
| ② 读项目代码 comment | **Code-RAG (Java!)**, GitLore, code-historian | Annotate.ai, Mind-Trace, code-graph-rag |
| ③ 速成学科笔记 | vault-search (教材索引), second-reader, cortex | smolbren, brain.md |

---

## 五、关键发现

### 5.1 不需要从零造的部分

| 能力 | 现成方案 | 备注 |
|------|---------|------|
| 代码符号级切分 | Code-RAG (Java), Tree-sitter | 比固定窗口切分好得多 |
| Obsidian vault RAG | cortex (33 tools), vault-search, obsidian-brain | 选一个接入即可 |
| 混合检索 | RRF (Reciprocal Rank Fusion) 已成标配 | 语义 + BM25 融合 |
| 图感知检索 | PageRank / PPR / Louvain | 比纯向量搜索更适合知识管理 |
| MCP 协议 | 大量现成 server | Java 端可参考 Code-RAG |
| VSCode 批注 | Annotate.ai, Mind-Trace, code-historian | 可参考交互模式 |

### 5.2 需要自己做的部分

| 能力 | 原因 |
|------|------|
| 三个场景的统一 frontmatter schema | 没有现成项目同时覆盖三个场景 |
| 主题/领域分类（金融/软工/代码原理） | 现有项目多为通用型 |
| "AI 像老师一样提醒"的主动推送 | 现有项目都是被动检索，没有主动推送 |
| Java 技术栈的端到端整合 | Code-RAG 是 Java 但只管代码，不管笔记 |

### 5.3 技术选型建议（Java 路线）

| 层 | 推荐方案 | 参考项目 |
|---|---------|---------|
| 代码切分 | 符号级切分（Tree-sitter Java binding 或 JavaParser） | Code-RAG |
| 向量库 | pgvector（强一致）或 LanceDB（轻量） | Code-RAG / vault-search |
| Embedding | Ollama 本地（nomic-embed-text 或 bge-m3 多语言） | 多数项目 |
| 检索 | 混合检索（语义 + BM25）+ RRF 融合 | vault-search, obsidian-brain |
| 图检索 | Personalized PageRank on wikilink graph | vault-search |
| MCP Server | Java 实现（参考 Code-RAG） | Code-RAG |
| 存储 | Obsidian Markdown + frontmatter | 所有 vault 项目 |
| VSCode 插件 | TypeScript（参考 code-historian 或 Annotate.ai） | code-historian |

---

## 六、过滤掉的低质量/广告结果

以下项目被过滤，原因标注：

| 项目 | 过滤原因 |
|------|---------|
| BenjaminX/dify-docs (annotation-reply) | 只是 Dify 文档片段，不是独立项目 |
| swapnil34/cognita | 只是 pinned repo 列表，无实质内容 |
| Mohithbuildszz/Zia-AI-Extention | 描述过于简略，无架构细节 |
| shubhamparamhans/CodeSage | 描述像广告，无实质技术细节 |
| maciek-O-digiaidev/CodeRAG | 描述过于简略 |
| Sarvesh-Kannan/SarvX | 依赖 Pinecone（云），不够本地化 |
| ShifatJahanShifa/StackRAGFix | 基于 Stack Overflow，不是个人知识管理 |
| GeorgijGrigoriev/gorag | 只支持 Go 语言，不通用 |

---

*下一步：确定要深入参考的 2-3 个项目，读取其源码和架构文档*
