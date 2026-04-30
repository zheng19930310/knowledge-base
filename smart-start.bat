@echo off
chcp 65001 >nul
echo ========================================
echo 知识库系统智能启动脚本
echo ========================================
echo.

REM 检查 Java
echo [1/4] 检查 Java 环境...
java -version >nul 2>&1
if errorlevel 1 (
    echo [警告] Java 未在 PATH 中找到，尝试自动检测...
    
    REM 尝试常见安装路径
    set JAVA_FOUND=0
    
    if exist "C:\Program Files\Java\jdk-17*\bin\java.exe" (
        for /d %%i in ("C:\Program Files\Java\jdk-17*") do (
            set "JAVA_HOME=%%i"
            set "PATH=%%i\bin;%PATH%"
            set JAVA_FOUND=1
            echo [成功] 找到 Java: %%i
            goto :java_found
        )
    )
    
    if exist "C:\Program Files\Eclipse Adoptium\jdk-17*\bin\java.exe" (
        for /d %%i in ("C:\Program Files\Eclipse Adoptium\jdk-17*") do (
            set "JAVA_HOME=%%i"
            set "PATH=%%i\bin;%PATH%"
            set JAVA_FOUND=1
            echo [成功] 找到 Java: %%i
            goto :java_found
        )
    )
    
    if exist "%LOCALAPPDATA%\Programs\AdoptOpenJDK\jdk-17*\bin\java.exe" (
        for /d %%i in ("%LOCALAPPDATA%\Programs\AdoptOpenJDK\jdk-17*") do (
            set "JAVA_HOME=%%i"
            set "PATH=%%i\bin;%PATH%"
            set JAVA_FOUND=1
            echo [成功] 找到 Java: %%i
            goto :java_found
        )
    )
    
    if !JAVA_FOUND! equ 0 (
        echo [错误] 未找到 Java 17
        echo.
        echo 请先安装 Java 17:
        echo 1. 访问 https://adoptium.net/temurin/releases/?version=17
        echo 2. 下载并安装 Windows x64 MSI 版本
        echo 3. 重新运行此脚本
        echo.
        pause
        exit /b 1
    )
)

:java_found
java -version
echo.

REM 检查 Maven
echo [2/4] 检查 Maven 环境...
mvn -version >nul 2>&1
if errorlevel 1 (
    echo [警告] Maven 未在 PATH 中找到
    
    REM 尝试常见安装路径
    if exist "C:\Program Files\Apache\maven\bin\mvn.cmd" (
        set "PATH=C:\Program Files\Apache\maven\bin;%PATH%"
        echo [成功] 找到 Maven
    ) else if exist "C:\apache-maven\bin\mvn.cmd" (
        set "PATH=C:\apache-maven\bin;%PATH%"
        echo [成功] 找到 Maven
    ) else (
        echo [错误] 未找到 Maven
        echo.
        echo 请先安装 Maven:
        echo 1. 访问 https://maven.apache.org/download.cgi
        echo 2. 下载并解压
        echo 3. 配置环境变量或重新运行此脚本
        echo.
        pause
        exit /b 1
    )
)

mvn -version | findstr "Apache Maven"
echo.

REM 检查 Qdrant
echo [3/4] 检查 Qdrant 状态...
curl -s http://localhost:6333/collections >nul 2>&1
if errorlevel 1 (
    echo [警告] Qdrant 未运行
    
    if exist "qdrant.exe" (
        echo [信息] 正在启动 Qdrant...
        start "Qdrant Server" qdrant.exe --storage-path .\qdrant_storage
        timeout /t 3 /nobreak >nul
        
        REM 验证 Qdrant 是否启动
        curl -s http://localhost:6333/collections >nul 2>&1
        if errorlevel 1 (
            echo [错误] Qdrant 启动失败
            pause
            exit /b 1
        )
        echo [成功] Qdrant 已启动
    ) else (
        echo [错误] 未找到 qdrant.exe
        echo 请下载 Qdrant 并放在项目根目录
        pause
        exit /b 1
    )
) else (
    echo [成功] Qdrant 正在运行
)
echo.

REM 检查 API Key
echo [4/4] 检查 API Key 配置...
set CHECK_API_KEY=0
findstr /C:"your-api-key-here" src\main\resources\application.yml >nul 2>&1
if errorlevel 1 (
    echo [成功] API Key 已配置
) else (
    echo [警告] API Key 尚未配置
    echo.
    echo 请编辑 src\main\resources\application.yml
    echo 将 qianwen.api-key 设置为您的通义千问 API Key
    echo.
    choice /C YN /M "是否继续启动（可能会失败）"
    if errorlevel 2 exit /b 1
)
echo.

echo ========================================
echo 开始启动应用...
echo ========================================
echo.

REM 编译并运行
echo [编译中] 请稍候...
call mvn clean package -DskipTests
if errorlevel 1 (
    echo [错误] 编译失败
    pause
    exit /b 1
)

echo.
echo [成功] 编译完成
echo.
echo ========================================
echo 应用启动中...
echo 访问地址: http://localhost:8080
echo ========================================
echo.

java -jar target\knowledge-base-1.0.0.jar

pause
