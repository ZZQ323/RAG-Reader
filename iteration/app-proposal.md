# RAG-Reader 产品提案 v2

> 版本：2.0
> 日期：2026-10-23
> 状态：方案确认，待实现

---

## 一、产品定位

**一句话**：个人知识管理系统，让 AI 像老师一样在你需要时提醒你读过的东西。

**三个场景**：
1. **读书笔记 comment**：阅读 PDF/网页/电子书时随手批注
2. **代码阅读 comment**：读项目代码时添加批注，记录理解
3. **速成学科笔记**：快速学习某领域时的碎片化笔记

**主题领域**：金融、软工、代码原理、计算机网络 等

---

## 二、核心设计原则

### 2.1 "知识引擎纯 Java，外设可异构"

- **Java 负责知识引擎**：代码 RAG + Vault RAG，统一检索入口
- **非 Java 组件只做数据生产或主动层**：小红书同步（Obsidian 插件）、主动推送（ProactiveAgent）
- **文件系统是唯一契约**：同步层和 Java RAG 层之间只通过 vault 目录通信

### 2.2 "组装优于造轮子"

- Code-RAG（Java）：代码 RAG，符号级切分，直接复用
- LangChain4j：Vault RAG 组件库，提供切分/嵌入/检索/MCP
- ProactiveAgent（TypeScript）：主动推送层，通过 MCP 挂载
- 小红书同步：Obsidian 插件，输出 markdown 到 vault

### 2.3 "真正需要自研的只有两件事"

1. **Obsidian-aware 的 markdown 切分器**：保留 wikilink、frontmatter、heading 层级
2. **Vault RAG 的摄入管道**：扫描 vault → 切分 → 嵌入 → 写入向量库 → 暴露 MCP

---

## 三、架构设计

```
┌─────────────────────────────────────────────────────────────┐
│  交互层                                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │  Obsidian    │  │  AI 客户端    │  │  VSCode 插件     │  │
│  │  (浏览/写作) │  │  (问答/调用) │  │  (代码批注)      │  │
│  └──────┬───────┘  └──────┬───────┘  └────────┬─────────┘  │
└─────────┼──────────────────┼───────────────────┼────────────┘
          │                  │                   │
          ▼                  ▼                   ▼
┌─────────────────────────────────────────────────────────────┐
│  主动层（外设，TypeScript）                                   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  ProactiveAgent                                      │   │
│  │  - 跨工具共享记忆                                     │   │
│  │  - 主动建议（纠正/重复/待办）                         │   │
│  │  - daemon 桌面通知                                    │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
          │ MCP
          ▼
┌─────────────────────────────────────────────────────────────┐
│  知识引擎层（Java）                                           │
│  ┌────────────────────┐  ┌────────────────────────────┐     │
│  │  Code-RAG          │  │  Vault-RAG                 │     │
│  │  (直接复用)        │  │  (自研摄入管道)            │     │
│  │  - 符号级切分      │  │  - Obsidian-aware 切分     │     │
│  │  - pgvector+HNSW   │  │  - Milvus/pgvector         │     │
│  │  - MCP Server      │  │  - 混合检索(BM25+向量)     │     │
│  └────────────────────┘  │  - MCP Server              │     │
│                           └────────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
          ▲
          │ 文件系统（vault 目录）
          │
┌─────────┴───────────────────────────────────────────────────┐
│  数据生产层（外设，异构）                                      │
│  ┌────────────────────┐  ┌────────────────────────────┐     │
│  │  小红书同步         │  │  手动输入                   │     │
│  │  (Obsidian 插件)   │  │  - Obsidian 写作            │     │
│  │  - 增量抓取        │  │  - VSCode 批注              │     │
│  │  - 导出 Markdown   │  │  - CLI 工具                 │     │
│  └────────────────────┘  └────────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

---

## 四、技术栈

| 层 | 组件 | 技术 | 来源 |
|---|------|------|------|
| 知识引擎 | Code-RAG | Java 21 + pgvector | 直接复用 blakemcbride/Code-RAG |
| 知识引擎 | Vault-RAG | Java 21 + LangChain4j + Milvus/pgvector | 自研摄入管道，复用 LangChain4j 组件 |
| 知识引擎 | 切分器 | Java + flexmark（Markdown 解析） | 自研 Obsidian-aware splitter |
| 知识引擎 | MCP Server | Java + LangChain4j MCP | 暴露检索工具 |
| 主动层 | ProactiveAgent | TypeScript/Node | 直接挂载 ConradLu2740/ProactiveAgent |
| 数据生产 | 小红书同步 | Obsidian 插件 | sunfang1cn/social-favorites-to-obsidian |
| 数据生产 | VSCode 批注 | TypeScript | 自研（可选，Phase 4） |
| 存储 | Obsidian Vault | Markdown + frontmatter | 文件系统 |
| 向量库 | Milvus 或 pgvector | 二选一 | 见下文取舍 |

### 4.1 向量库取舍

**选项 A：Milvus（推荐）**
- LangChain4j 原生支持混合检索（BM25 + 向量 + RRF）
- 独立部署，不依赖 PostgreSQL
- 内存占用较高（ standalone 模式 ~2GB）

**选项 B：pgvector**
- 和 Code-RAG 统一（Code-RAG 用 pgvector）
- 轻量，复用现有 PostgreSQL
- 但 LangChain4j 的混合检索不原生支持 pgvector，需要自己写 BM25 融合

**建议**：先用 Milvus 快速跑通，后期如果性能/资源有问题再考虑迁移到 pgvector + 自研 BM25。

### 4.2 内存优化

用户提到"Java 跑起来费内存"。优化方向：
- LangChain4j 支持本地 embedding（ONNX Runtime），不需要调用外部 API
- 向量库用 Milvus Lite（嵌入式模式），不需要独立部署
- 切分器用流式处理，不一次性加载整个 vault
- 如果内存仍是问题，考虑用 GraalVM native-image 编译成原生可执行文件

---

## 五、功能清单

### Phase 1：Vault RAG 基础（2 周）✅ 已完成

**目标**：Obsidian vault 可被语义搜索

**状态**：代码实现完成（2026-10-24）

**功能**：
- [x] F1.1 Obsidian-aware 切分器
  - 按 heading 层级切分
  - 保留 frontmatter（type/tags/source）
  - 保留 wikilink（[[原文]]）
  - 保留代码块和 LaTeX
  - **实现**：`ObsidianMarkdownChunker.java`
- [x] F1.2 摄入管道
  - 扫描 vault 目录
  - 增量索引（基于 content hash）
  - 调用 embedding 模型（本地 ONNX AllMiniLmL6V2）
  - 写入向量库（InMemoryEmbeddingStore，可切换 Milvus）
  - **实现**：`VaultIngestor.java`, `NoteEmbeddingStore.java`
- [x] F1.3 MCP Server
  - `search_notes(query, limit)`：语义搜索
  - `get_note(path)`：读取笔记
  - `list_notes()`：列出笔记
  - **实现**：`McpServer.java`（HTTP JSON-RPC）
- [x] F1.4 文件监听
  - 监听 vault 目录变化
  - 自动触发增量索引
  - **实现**：`VaultWatcher.java`（directory-watcher）

**验收标准**：
- [ ] 新写入的 markdown 在 2 秒内被索引（待验证）
- [ ] 问"关于 Agent 设计模式我之前读过什么"能返回相关笔记（待验证）

**待办**：
1. 本地环境编译：`mvn clean package`
2. 创建测试 vault 并运行
3. 在 AI 客户端中配置 MCP 并测试

---

### Phase 2：Code-RAG 集成（1 周）

**目标**：代码库可被语义搜索

**功能**：
- [ ] F2.1 部署 Code-RAG
  - 安装 PostgreSQL + pgvector
  - 配置代码库路径
  - 启动 MCP Server
- [ ] F2.2 验证符号级切分
  - 测试 Java/Python/TypeScript 代码
  - 验证函数/类级别的检索精度
- [ ] F2.3 接入 AI 客户端
  - 在 Cursor/Claude Code 中配置 MCP
  - 测试"这个函数在哪里定义"类问题

**验收标准**：
- 代码库索引完成
- AI 客户端能通过 MCP 查询代码

---

### Phase 3：主动推送集成（1 周）

**目标**：AI 能在合适时机提醒

**功能**：
- [ ] F3.1 安装 ProactiveAgent
  - `npm install @proactive-agent/mcp`
  - `npx proactive-mcp init`
- [ ] F3.2 配置克制参数
  - 每日通知上限：6 条
  - 冷却时间：15 分钟
  - DND 时段：22:30-08:00
- [ ] F3.3 测试主动建议
  - 输入"以后提交前先写单元测试" → 收到纠正建议
  - 输入"每天下午 5 点检查项目进展" → 收到自动化建议

**验收标准**：
- ProactiveAgent 能记住用户纠正
- 能在合适时机主动提醒

---

### Phase 4：小红书同步（1 周）

**目标**：收藏夹增量同步到 vault

**功能**：
- [ ] F4.1 安装 Obsidian 插件
  - 选择 sunfang1cn/social-favorites-to-obsidian 或其他成熟插件
  - 配置 CookieCloud 或手动导出 Cookie
- [ ] F4.2 跑通全量导出
  - 首次导出所有收藏
  - 验证图片本地化
- [ ] F4.3 配置增量同步
  - 设置 cron 定时任务（每天凌晨 3 点）
  - 验证只抓新增条目

**验收标准**：
- 收藏夹 500+ 条笔记全部导出为 markdown
- 增量同步只抓新增条目

---

### Phase 5：感想沉淀工作流（持续）

**目标**：阅读时随手写感想

**功能**：
- [ ] F5.1 Obsidian 模板
  - `templates/感想.md`（含 frontmatter）
  - 快捷键 `Cmd+T` 插入模板
- [ ] F5.2 Dataview 查询
  - 按 type/tags/created_at 生成视图
  - "本周感想"视图
- [ ] F5.3 回顾机制
  - 每周回顾：哪些感想值得展开
  - 每月回顾：哪些主题需要深入

**验收标准**：
- 每天至少写一条感想
- 感想通过 `[[原文]]` 链接到原文

---

### Phase 6：VSCode 批注插件（可选，2-3 周）

**目标**：读代码时随手批注

**功能**：
- [ ] F6.1 选中文本 → 右键"添加批注"
- [ ] F6.2 批注存储为 Markdown（含文件路径、行号）
- [ ] F6.3 自动同步到 vault
- [ ] F6.4 触发 Vault-RAG 索引

**验收标准**：
- 批注自动同步到 vault
- 能通过语义搜索找到批注

---

## 六、迭代计划

见 `iteration/README.md`，每个迭代一个文件，逐步细化。

---

## 七、风险与应对

| 风险 | 概率 | 应对 |
|------|------|------|
| Milvus 内存占用过高 | 中 | 改用 Milvus Lite 或迁移到 pgvector + 自研 BM25 |
| Obsidian-aware 切分器开发复杂 | 中 | 先用 LangChain4j 通用切分器跑通，后期优化 |
| 小红书插件失效 | 中 | 降级为手动导出（浏览器插件 MarkDownload） |
| ProactiveAgent 不够"主动" | 高 | 接受现状，后期自己扩展"基于语义的主动提醒" |
| Java 开发效率低 | 中 | 用 LangChain4j 减少样板代码，必要时用 Kotlin |

---

## 八、下一步

1. **本周**：开始 Phase 1，写 Obsidian-aware 切分器的设计文档
2. **下周**：实现切分器 + 摄入管道，跑通 vault 索引
3. **持续**：每天写一条感想，积累语料

---

*文档版本：v2.0*
*最后更新：2026-10-23*
