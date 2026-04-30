# 知识库系统

基于 Spring Boot + Qdrant + 通义千问的智能知识库管理系统，支持文档管理、RAG增强问答和知识图谱展示。

## 功能特性

- 📄 **文档管理**：支持 Word、PDF、TXT 格式文件上传和管理
- 🔍 **RAG 问答**：基于本地知识库的检索增强生成问答
- 🧠 **知识图谱**：自动提取实体并可视化展示
- 💭 **对话记忆**：支持多轮对话，可开关记忆功能
- 📊 **匹配指标**：显示匹配度和召回率
- 🔗 **来源标注**：回答附带文档来源和路径

## 技术栈

- **后端**: Spring Boot 3.2 + Java 17
- **矢量数据库**: Qdrant
- **关系数据库**: H2
- **前端**: HTML + JavaScript + ECharts
- **AI 模型**: 阿里云通义千问 API
- **文档处理**: Apache POI, PDFBox

## 前置要求

1. **Java 17+**
   - 下载: https://adoptium.net/
   - 验证: `java -version`

2. **Maven 3.6+**
   - 下载: https://maven.apache.org/download.cgi
   - 验证: `mvn -version`

3. **Qdrant**
   - 下载: https://github.com/qdrant/qdrant/releases
   - 选择 Windows 版本 (qdrant-x86_64-pc-windows-msvc.zip)

4. **阿里云通义千问 API Key**
   - 申请: https://dashscope.aliyun.com/

## 快速开始

### 1. 下载 Qdrant

```bash
# 访问 https://github.com/qdrant/qdrant/releases
# 下载最新版本的 Windows zip 包
# 解压后将 qdrant.exe 复制到项目根目录
```

### 2. 配置 API Key

编辑 `src/main/resources/application.yml`，修改：

```yaml
qianwen:
  api-key: your-api-key-here  # 替换为你的通义千问 API Key
```

或者设置环境变量：

```bash
set QIANWEN_API_KEY=your-api-key-here
```

### 3. 启动 Qdrant

双击运行 `start-qdrant.bat`，或手动执行：

```bash
qdrant.exe --storage-path ./qdrant_storage
```

Qdrant 将在 http://localhost:6333 运行

### 4. 启动应用

双击运行 `start.bat`，或手动执行：

```bash
mvn clean package -DskipTests
java -jar target/knowledge-base-1.0.0.jar
```

应用将在 http://localhost:8080 运行

### 5. 访问系统

打开浏览器访问: http://localhost:8080

## 使用说明

### 文档管理

1. **上传文档**
   - 点击上传区域或拖拽文件
   - 支持 .docx, .pdf, .txt 格式
   - 单个文件不超过 50MB

2. **同步到矢量库**
   - 点击"同步"按钮
   - 系统将自动分块并向量化文档内容
   - 同步状态显示为"已同步"

3. **管理文档**
   - **删除**：从系统和矢量库中完全删除
   - **移除向量**：仅从矢量库删除，保留原文档
   - **重新同步**：更新矢量库中的内容

### 智能问答

1. **提问**
   - 在输入框输入问题
   - 按 Enter 或点击"发送"

2. **记忆开关**
   - 右上角切换对话记忆
   - 开启：携带历史对话上下文
   - 关闭：仅基于当前问题回答

3. **查看结果**
   - 匹配度：最高相似度分数
   - 召回率：相关文档召回比例
   - 来源：引用的文档路径

### 知识图谱

1. **查看图谱**
   - 切换到"知识图谱"页面
   - 点击"刷新图谱"加载数据

2. **交互操作**
   - 拖拽节点调整布局
   - 滚轮缩放
   - 点击查看节点详情

3. **实体类型**
   - PERSON：人名
   - LOCATION：地点
   - ORGANIZATION：组织
   - TERM：术语

## 项目结构

```
knowledge-base/
├── src/main/java/com/knowledge/
│   ├── controller/          # REST API 控制器
│   ├── service/             # 业务逻辑层
│   ├── repository/          # 数据访问层
│   ├── model/               # JPA 实体
│   ├── config/              # 配置类
│   ├── util/                # 工具类
│   └── KnowledgeBaseApplication.java
├── src/main/resources/
│   ├── static/              # 前端页面
│   │   └── index.html
│   └── application.yml      # 应用配置
├── data/                    # H2 数据库文件
├── uploads/                 # 上传的文件
├── qdrant_storage/          # Qdrant 数据（自动生成）
├── pom.xml                  # Maven 配置
├── start.bat                # 启动脚本
├── start-qdrant.bat         # Qdrant 启动脚本
└── requirements.md          # 需求文档
```

## API 接口

### 文档管理

- `POST /api/documents/upload` - 上传文件
- `GET /api/documents` - 获取文档列表
- `DELETE /api/documents/{id}` - 删除文档
- `POST /api/documents/{id}/sync` - 同步到矢量库
- `POST /api/documents/{id}/remove-from-vector` - 从矢量库移除

### 智能问答

- `POST /api/chat` - 发送问题
  ```json
  {
    "question": "你的问题",
    "sessionId": "session_xxx",
    "memoryEnabled": true
  }
  ```
- `POST /api/chat/clear` - 清空对话历史

### 知识图谱

- `GET /api/graph` - 获取图谱数据
- `POST /api/graph/extract/{documentId}` - 提取实体

## 配置说明

### application.yml

```yaml
# 服务器端口
server:
  port: 8080

# Qdrant 配置
qdrant:
  host: localhost
  port: 6333
  collection-name: knowledge_base
  vector-size: 1536

# 通义千问配置
qianwen:
  api-key: ${QIANWEN_API_KEY}
  model: qwen-turbo
  embedding-model: text-embedding-v2

# 应用配置
app:
  chunk-size: 600          # 文本分块大小
  chunk-overlap: 50        # 分块重叠
  top-k: 5                 # 召回文档数量
  similarity-threshold: 0.7 # 相似度阈值
  max-memory-turns: 10     # 最大记忆轮数
```

## 常见问题

### 1. Qdrant 连接失败

**问题**: 启动时提示无法连接 Qdrant

**解决**:
- 确保 Qdrant 已启动
- 检查端口 6333 是否被占用
- 确认 application.yml 中的配置正确

### 2. API 调用失败

**问题**: 问答时返回错误

**解决**:
- 检查 API Key 是否正确
- 确认网络连接正常
- 查看控制台日志获取详细错误

### 3. 文档解析失败

**问题**: 上传文件后无法解析

**解决**:
- 确认文件格式正确
- 检查文件是否损坏
- 查看文件大小是否超过限制

### 4. H2 数据库位置

H2 数据库文件存储在 `./data/knowledge_db.mv.db`

可以通过 http://localhost:8080/h2-console 访问数据库控制台

## 性能优化建议

1. **批量同步**：大量文档建议在非高峰期同步
2. **分块大小**：根据文档类型调整 chunk-size
3. **Top-K 值**：平衡召回率和响应时间
4. **缓存策略**：可扩展添加 Redis 缓存

## 开发计划

- [ ] 支持更多文档格式（Excel, PPT）
- [ ] 用户认证和权限管理
- [ ] 多语言支持
- [ ] 本地 Embedding 模型
- [ ] 更复杂的实体提取算法
- [ ] 对话历史导出
- [ ] 批量导入功能

## 许可证

MIT License

## 联系方式

如有问题或建议，请提交 Issue。
