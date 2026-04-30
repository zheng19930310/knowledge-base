@echo off
chcp 65001 >nul
title 知识库系统启动

echo ========================================
echo 知识库系统启动
echo ========================================
echo.

REM 设置颜色
color 0A

echo 正在检查环境...
echo.

REM 尝试查找并设置 Java
if not defined JAVA_HOME (
    echo [提示] 未检测到 JAVA_HOME，尝试自动定位...
    
    for /d %%i in ("C:\Program Files\Java\jdk-17*") do (
        set "JAVA_HOME=%%i"
        goto :java_set
    )
    
    for /d %%i in ("C:\Program Files\Eclipse Adoptium\jdk-17*") do (
        set "JAVA_HOME=%%i"
        goto :java_set
    )
    
    for /d %%i in ("%LOCALAPPDATA%\Programs\AdoptOpenJDK\jdk-17*") do (
        set "JAVA_HOME=%%i"
        goto :java_set
    )
    
    echo [错误] 无法自动找到 Java 17
    echo 请手动设置 JAVA_HOME 环境变量
    pause
    exit /b 1
)

:java_set
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo [√] Java 路径: %JAVA_HOME%
java -version 2>&1 | findstr "version"
echo.

REM 尝试查找并设置 Maven
if not defined M2_HOME (
    echo [提示] 未检测到 M2_HOME，尝试自动定位...
    
    if exist "C:\Program Files\Apache\maven" (
        set "M2_HOME=C:\Program Files\Apache\maven"
        goto :maven_set
    )
    
    if exist "C:\apache-maven" (
        set "M2_HOME=C:\apache-maven"
        goto :maven_set
    )
    
    echo [错误] 无法自动找到 Maven
    echo 请手动设置 M2_HOME 环境变量
    pause
    exit /b 1
)

:maven_set
set "PATH=%M2_HOME%\bin;%PATH%"
echo [√] Maven 路径: %M2_HOME%
mvn -version 2>&1 | findstr "Apache Maven"
echo.

REM 检查 Qdrant
echo [检查] Qdrant 状态...
powershell -Command "try { Invoke-WebRequest -Uri 'http://localhost:6333/collections' -UseBasicParsing | Out-Null; Write-Host '[√] Qdrant 正在运行' } catch { Write-Host '[×] Qdrant 未运行' }"
echo.

echo ========================================
echo 开始编译项目...
echo ========================================
echo.

call mvn clean package -DskipTests

if errorlevel 1 (
    echo.
    echo [错误] 编译失败！
    pause
    exit /b 1
)

echo.
echo ========================================
echo 编译成功！正在启动应用...
echo ========================================
echo.
echo 访问地址: http://localhost:8080
echo 按 Ctrl+C 停止服务
echo.

java -jar target\knowledge-base-1.0.0.jar

pause
