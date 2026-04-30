# 项目快速开始指南

## 5分钟快速启动

### 第一步：准备环境

1. **安装 Java 17**
   ```bash
   # 验证安装
   java -version
   ```

2. **安装 Maven**
   ```bash
   # 验证安装
   mvn -version
   ```

3. **下载 Qdrant**
   - 访问: https://github.com/qdrant/qdrant/releases
   - 下载 Windows 版本 zip 文件
   - 解压后将 `qdrant.exe` 放在项目根目录

4. **获取 API Key**
   - 访问: https://dashscope.aliyun.com/
   - 注册并创建 API Key

### 第二步：配置项目

编辑 `src/main/resources/application.yml`:

```yaml
qianwen:
  api-key: sk-xxxxxxxxxxxxxx  # 替换为你的 API Key
```

### 第三步：启动服务

1. **启动 Qdrant**
   ```bash
   # 双击运行
   start-qdrant.bat
   
   # 或命令行
   .\start-qdrant.bat
   ```

2. **启动应用**
   ```bash
   # 双击运行
   start.bat
   
   # 或命令行
   mvn spring-boot:run
   ```

3. **访问系统**
   ```
   http://localhost:8080
   ```

### 第四步：使用系统

#### 上传文档
1. 点击"文档管理"菜单
2. 拖拽或点击上传 Word/PDF/TXT 文件
3. 等待上传完成

#### 同步到矢量库
1. 在文档列表中找到刚上传的文件
2. 点击"同步"按钮
3. 等待同步完成（状态变为"已同步"）

#### 智能问答
1. 点击"智能问答"菜单
2. 在输入框输入问题
3. 查看 AI 回答和匹配度

#### 查看知识图谱
1. 点击"知识图谱"菜单
2. 点击"刷新图谱"
3. 查看实体关系图

## 测试示例

### 示例文档内容

创建一个测试文档 `test.txt`，内容：

```
人工智能（Artificial Intelligence，简称AI）是计算机科学的一个分支，
它企图了解智能的实质，并生产出一种新的能以人类智能相似的方式做出反应的智能机器。

深度学习是机器学习的一种特殊类型，它使用多层神经网络来模拟人脑的工作方式。

自然语言处理（NLP）是人工智能的一个重要应用领域，它使计算机能够理解、解释和生成人类语言。
```

### 测试问题

上传并同步后，尝试提问：

1. "什么是人工智能？"
2. "深度学习和机器学习有什么关系？"
3. "NLP是什么？"

系统会基于文档内容回答，并显示来源和匹配度。

## 故障排查

### 问题1：无法连接 Qdrant

**症状**: 启动时提示连接失败

**解决**:
```bash
# 检查 Qdrant 是否运行
curl http://localhost:6333/collections

# 如果没有响应，重新启动 Qdrant
.\start-qdrant.bat
```

### 问题2：API 调用失败

**症状**: 问答时返回错误

**解决**:
1. 检查 API Key 是否正确
2. 确认网络连接
3. 查看控制台日志

### 问题3：文档解析失败

**症状**: 上传后无法读取内容

**解决**:
1. 确认文件格式正确
2. 检查文件是否损坏
3. 确保文件大小 < 50MB

## 下一步

- 阅读完整的 [README.md](README.md)
- 查看 [需求文档](requirements.md)
- 了解 [Qdrant 安装](QDRANT_INSTALL.md)

## 技术支持

遇到问题？请检查：
1. 控制台日志
2. 浏览器开发者工具
3. 上述常见问题解决方案

祝使用愉快！🎉
