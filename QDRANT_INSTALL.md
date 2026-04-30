# Qdrant 安装指南

## Windows 系统安装步骤

### 方法一：直接下载（推荐）

1. **访问发布页面**
   ```
   https://github.com/qdrant/qdrant/releases
   ```

2. **下载 Windows 版本**
   - 找到最新版本（如 v1.8.0）
   - 下载 `qdrant-x86_64-pc-windows-msvc.zip`

3. **解压文件**
   - 解压 zip 文件
   - 将 `qdrant.exe` 复制到项目根目录 `d:\aiwork\knowledge-base\`

4. **验证安装**
   ```bash
   .\qdrant.exe --version
   ```

5. **启动 Qdrant**
   ```bash
   # 使用提供的脚本
   .\start-qdrant.bat
   
   # 或手动启动
   .\qdrant.exe --storage-path .\qdrant_storage
   ```

6. **访问 Web UI**
   ```
   http://localhost:6333/dashboard
   ```

### 方法二：使用 Docker（备选）

如果已安装 Docker，可以使用容器方式：

```bash
docker pull qdrant/qdrant

docker run -p 6333:6333 -p 6334:6334 ^
    -v "%cd%\qdrant_storage:/qdrant/storage" ^
    qdrant/qdrant
```

## 配置说明

### 默认端口

- **HTTP API**: 6333
- **gRPC API**: 6334
- **Web UI**: http://localhost:6333/dashboard

### 数据存储

Qdrant 数据默认存储在 `./qdrant_storage` 目录

### 集合配置

应用启动时会自动创建名为 `knowledge_base` 的集合，配置如下：

- **向量维度**: 1536
- **距离度量**: Cosine
- **存储类型**: Memory + Disk

## 常见问题

### 1. 端口被占用

**错误**: `Address already in use`

**解决**:
```bash
# 查找占用端口的进程
netstat -ano | findstr :6333

# 结束进程（替换 PID）
taskkill /PID <PID> /F
```

### 2. 无法启动

**错误**: 缺少 DLL 文件

**解决**:
- 安装 Visual C++ Redistributable
- 下载: https://aka.ms/vs/17/release/vc_redist.x64.exe

### 3. 数据持久化

确保启动时指定了 `--storage-path` 参数：

```bash
qdrant.exe --storage-path ./qdrant_storage
```

## 测试连接

启动后，在浏览器访问：

```
http://localhost:6333/collections
```

应该返回：

```json
{
  "result": {
    "collections": []
  },
  "status": "ok",
  "time": 0.000xxx
}
```

## 管理工具

### Web Dashboard

访问 http://localhost:6333/dashboard 可以：
- 查看集合列表
- 浏览向量数据
- 执行搜索测试
- 监控性能指标

### REST API

常用 API 示例：

```bash
# 创建集合
curl -X PUT http://localhost:6333/collections/knowledge_base \
  -H 'Content-Type: application/json' \
  -d '{
    "vectors": {
      "size": 1536,
      "distance": "Cosine"
    }
  }'

# 查看集合信息
curl http://localhost:6333/collections/knowledge_base

# 删除集合
curl -X DELETE http://localhost:6333/collections/knowledge_base
```

## 性能调优

### 内存配置

编辑 `config.yaml`（首次启动后生成）：

```yaml
storage:
  optimizers_config:
    max_segment_size: 200000
    memmap_threshold: 50000
```

### 并发设置

```yaml
service:
  max_workers: 4
  grpc_timeout: 60
```

## 备份和恢复

### 备份

直接复制 `qdrant_storage` 目录：

```bash
xcopy /E /I qdrant_storage qdrant_backup
```

### 恢复

停止 Qdrant，替换目录：

```bash
taskkill /IM qdrant.exe /F
rmdir /S /Q qdrant_storage
xcopy /E /I qdrant_backup qdrant_storage
.\qdrant.exe --storage-path .\qdrant_storage
```

## 更新 Qdrant

1. 停止当前运行的 Qdrant
2. 备份 `qdrant_storage` 目录
3. 下载新版本
4. 替换 `qdrant.exe`
5. 重新启动

## 更多资源

- 官方文档: https://qdrant.tech/documentation/
- GitHub: https://github.com/qdrant/qdrant
- API 文档: https://qdrant.github.io/qdrant/redoc/index.html
