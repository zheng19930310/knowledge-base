# 环境配置与启动指南

## 当前状态检查

❌ **Java**: 未安装或未配置
❌ **Maven**: 未安装或未配置  
❌ **Qdrant**: 未下载

## 第一步：安装 Java 17

### 方法一：使用 Adoptium（推荐）

1. **下载安装包**
   - 访问: https://adoptium.net/temurin/releases/?version=17
   - 选择 Windows x64 MSI 安装包
   - 下载并运行安装程序

2. **验证安装**
   ```powershell
   java -version
   ```
   应该显示类似：
   ```
   openjdk version "17.0.x"
   ```

### 方法二：使用 Oracle JDK

1. **下载**
   - 访问: https://www.oracle.com/java/technologies/downloads/#java17
   - 下载 Windows x64 Installer

2. **安装并验证**
   ```powershell
   java -version
   ```

### 配置环境变量（如果需要）

如果安装后 `java -version` 仍无法识别：

1. 右键"此电脑" → "属性" → "高级系统设置"
2. 点击"环境变量"
3. 在"系统变量"中找到 `Path`，点击"编辑"
4. 添加 Java bin 目录路径，例如：
   ```
   C:\Program Files\Eclipse Adoptium\jdk-17.x.x.x-hotspot\bin
   ```
5. 重启 PowerShell

## 第二步：安装 Maven

### 方法一：使用 Chocolatey（最简单）

1. **安装 Chocolatey**（如果还没有）
   ```powershell
   # 以管理员身份运行 PowerShell
   Set-ExecutionPolicy Bypass -Scope Process -Force; 
   [System.Net.ServicePointManager]::SecurityProtocol = 
   [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; 
   iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
   ```

2. **安装 Maven**
   ```powershell
   choco install maven
   ```

3. **验证**
   ```powershell
   mvn -version
   ```

### 方法二：手动安装

1. **下载 Maven**
   - 访问: https://maven.apache.org/download.cgi
   - 下载 `apache-maven-3.9.x-bin.zip`

2. **解压**
   - 解压到 `C:\Program Files\Apache\maven`

3. **配置环境变量**
   - 新建系统变量 `MAVEN_HOME`: `C:\Program Files\Apache\maven`
   - 编辑 `Path`，添加: `%MAVEN_HOME%\bin`

4. **验证**
   ```powershell
   mvn -version
   ```

## 第三步：下载 Qdrant

1. **访问发布页面**
   ```
   https://github.com/qdrant/qdrant/releases
   ```

2. **下载最新版本**
   - 找到最新 release（如 v1.8.0）
   - 下载 `qdrant-x86_64-pc-windows-msvc.zip`

3. **解压并放置**
   - 解压 zip 文件
   - 将 `qdrant.exe` 复制到项目根目录：
     ```
     d:\aiwork\knowledge-base\qdrant.exe
     ```

4. **验证**
   ```powershell
   .\qdrant.exe --version
   ```

## 第四步：配置通义千问 API Key

1. **申请 API Key**
   - 访问: https://dashscope.aliyun.com/
   - 注册阿里云账号
   - 开通通义千问服务
   - 创建 API Key

2. **配置到项目**
   
   **方式一：编辑配置文件**
   
   编辑 `src/main/resources/application.yml`：
   ```yaml
   qianwen:
     api-key: sk-your-api-key-here  # 替换为你的真实 Key
   ```
   
   **方式二：设置环境变量（推荐）**
   ```powershell
   # 临时设置（当前会话有效）
   $env:QIANWEN_API_KEY="sk-your-api-key-here"
   
   # 永久设置
   [Environment]::SetEnvironmentVariable("QIANWEN_API_KEY", "sk-your-api-key-here", "User")
   ```

## 第五步：启动系统

### 启动 Qdrant

```powershell
# 在项目根目录执行
.\start-qdrant.bat
```

或手动启动：
```powershell
.\qdrant.exe --storage-path .\qdrant_storage
```

验证 Qdrant 是否启动成功：
```powershell
curl http://localhost:6333/collections
```

应该返回 JSON 数据。

### 启动应用

**方式一：使用启动脚本**
```powershell
.\start.bat
```

**方式二：使用 Maven**
```powershell
mvn spring-boot:run
```

**方式三：先编译再运行**
```powershell
# 编译
mvn clean package -DskipTests

# 运行
java -jar target\knowledge-base-1.0.0.jar
```

## 第六步：访问系统

启动成功后，打开浏览器访问：
```
http://localhost:8080
```

## 快速检查清单

启动前请确认：

- [ ] Java 17 已安装并可用（`java -version`）
- [ ] Maven 已安装并可用（`mvn -version`）
- [ ] Qdrant 已下载并放在项目根目录
- [ ] 通义千问 API Key 已配置
- [ ] Qdrant 已启动（端口 6333）

## 常见问题

### 1. Java 版本不对

**问题**: 提示需要 Java 17，但安装的是其他版本

**解决**: 
- 卸载旧版本
- 重新安装 Java 17
- 确保环境变量指向正确版本

### 2. Maven 下载依赖慢

**解决**: 配置国内镜像

编辑 `C:\Users\你的用户名\.m2\settings.xml`：
```xml
<mirrors>
  <mirror>
    <id>aliyun</id>
    <mirrorOf>central</mirrorOf>
    <name>Aliyun Maven</name>
    <url>https://maven.aliyun.com/repository/public</url>
  </mirror>
</mirrors>
```

### 3. Qdrant 端口被占用

**解决**:
```powershell
# 查找占用端口的进程
netstat -ano | findstr :6333

# 结束进程（替换 PID）
taskkill /PID <进程ID> /F
```

### 4. API Key 无效

**检查**:
- Key 是否正确复制（没有多余空格）
- 阿里云账号是否已实名认证
- 通义千问服务是否已开通
- 是否有足够的配额

### 5. 编译失败

**解决**:
```powershell
# 清理并重新编译
mvn clean
mvn compile

# 跳过测试
mvn package -DskipTests
```

## 一键启动脚本（环境准备好后）

创建 `quick-start.ps1`：

```powershell
# 检查 Java
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Host "错误: Java 未安装" -ForegroundColor Red
    exit 1
}

# 检查 Maven
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Host "错误: Maven 未安装" -ForegroundColor Red
    exit 1
}

# 检查 Qdrant
if (-not (Test-Path ".\qdrant.exe")) {
    Write-Host "错误: Qdrant 未下载" -ForegroundColor Red
    exit 1
}

# 检查 API Key
if ([string]::IsNullOrEmpty($env:QIANWEN_API_KEY)) {
    Write-Host "警告: 未设置 QIANWEN_API_KEY 环境变量" -ForegroundColor Yellow
}

# 启动 Qdrant
Write-Host "正在启动 Qdrant..." -ForegroundColor Green
Start-Process ".\qdrant.exe" -ArgumentList "--storage-path", ".\qdrant_storage" -WindowStyle Minimized
Start-Sleep -Seconds 3

# 检查 Qdrant 是否启动
try {
    $response = Invoke-WebRequest -Uri "http://localhost:6333/collections" -UseBasicParsing
    Write-Host "Qdrant 启动成功" -ForegroundColor Green
} catch {
    Write-Host "错误: Qdrant 启动失败" -ForegroundColor Red
    exit 1
}

# 启动应用
Write-Host "正在启动应用..." -ForegroundColor Green
mvn spring-boot:run
```

使用方法：
```powershell
.\quick-start.ps1
```

## 下一步

环境配置完成后：

1. 阅读 [QUICKSTART.md](QUICKSTART.md) 了解如何使用系统
2. 查看 [README.md](README.md) 了解完整功能
3. 参考 [requirements.md](requirements.md) 了解需求细节

祝您使用愉快！🎉
