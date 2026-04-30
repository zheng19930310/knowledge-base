# 🚨 启动前必须完成 - Qdrant 安装指南

## ❌ 当前问题

**qdrant.exe 文件不存在**，导致项目无法启动。

---

## ✅ 解决方案

### 步骤1：下载 Qdrant

#### 方式一：直接下载（推荐）

1. **访问 GitHub Releases**
   ```
   https://github.com/qdrant/qdrant/releases
   ```

2. **下载最新版本**
   - 找到最新的 release（如 v1.14.1）
   - 下载 `qdrant-x86_64-pc-windows-msvc.zip`

3. **使用加速链接**（如果 GitHub 慢）
   ```
   https://ghproxy.com/https://github.com/qdrant/qdrant/releases/download/v1.14.1/qdrant-x86_64-pc-windows-msvc.zip
   ```

#### 方式二：使用 GitCode 镜像

访问：https://gitcode.com/GitHub_Trending/qd/qdrant

---

### 步骤2：解压并放置

1. **解压 zip 文件**
   - 右键点击下载的 zip 文件
   - 选择"全部提取"

2. **复制 qdrant.exe**
   - 从解压后的文件夹中找到 `qdrant.exe`
   - 复制到项目根目录：
     ```
     d:\aiwork\knowledge-base\qdrant.exe
     ```

3. **验证**
   ```powershell
   cd d:\aiwork\knowledge-base
   .\qdrant.exe --version
   ```

---

### 步骤3：启动 Qdrant

#### 方式一：使用启动脚本

```powershell
cd d:\aiwork\knowledge-base
.\start-qdrant.bat
```

#### 方式二：手动启动

```powershell
cd d:\aiwork\knowledge-base
.\qdrant.exe --storage-path .\qdrant_storage
```

Qdrant 将在后台运行，窗口最小化。

---

### 步骤4：验证 Qdrant 是否启动成功

```powershell
# PowerShell
Invoke-WebRequest -Uri 'http://localhost:6333/collections' -UseBasicParsing

# 或浏览器访问
http://localhost:6333/dashboard
```

应该看到 JSON 响应或 Web UI 界面。

---

### 步骤5：启动知识库应用

确认 Qdrant 运行后：

```powershell
cd d:\aiwork\knowledge-base
start.bat
```

---

## 📋 完整启动流程

每次启动项目的正确顺序：

1. **启动 Qdrant**
   ```powershell
   .\start-qdrant.bat
   ```
   等待 3-5 秒

2. **启动应用**
   ```powershell
   .\start.bat
   ```

3. **访问系统**
   ```
   http://localhost:8080
   ```

---

## 🔍 检查清单

启动前请确认：

- [ ] qdrant.exe 已下载并放在项目根目录
- [ ] Qdrant 正在运行（端口 6333）
- [ ] Java 环境变量已配置
- [ ] Maven 环境变量已配置
- [ ] API Key 已在 application.yml 中配置

---

## 💡 快速验证脚本

创建 `check-all.bat` 来检查所有环境：

```batch
@echo off
echo 检查 Qdrant...
if exist "qdrant.exe" (
    echo [✓] qdrant.exe 存在
) else (
    echo [×] qdrant.exe 不存在，请先下载
    exit /b 1
)

echo.
echo 检查 Java...
java -version >nul 2>&1
if errorlevel 1 (
    echo [×] Java 未配置
) else (
    echo [✓] Java 已配置
)

echo.
echo 检查 Maven...
mvn -version >nul 2>&1
if errorlevel 1 (
    echo [×] Maven 未配置
) else (
    echo [✓] Maven 已配置
)

echo.
echo 检查 Qdrant 服务...
powershell -Command "try { Invoke-WebRequest -Uri 'http://localhost:6333/collections' -UseBasicParsing -TimeoutSec 2 | Out-Null; Write-Host '[✓] Qdrant 正在运行' } catch { Write-Host '[×] Qdrant 未运行' }"

pause
```

---

## ❓ 常见问题

### Q1: 下载很慢怎么办？

**A:** 使用加速链接或 GitCode 镜像

### Q2: qdrant.exe 放在哪里？

**A:** 必须放在项目根目录：`d:\aiwork\knowledge-base\qdrant.exe`

### Q3: 如何知道 Qdrant 启动成功了？

**A:** 
- 访问 http://localhost:6333/dashboard
- 或运行：`curl http://localhost:6333/collections`

### Q4: 端口 6333 被占用怎么办？

**A:**
```powershell
# 查找占用端口的进程
netstat -ano | findstr :6333

# 结束进程
taskkill /PID <进程ID> /F
```

---

## 🎯 下一步

1. **立即下载 Qdrant**
2. **解压并放置到项目根目录**
3. **运行 start-qdrant.bat**
4. **运行 start.bat**

祝您成功！🚀
