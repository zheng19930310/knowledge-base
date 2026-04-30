@echo off
chcp 65001 >nul
echo ========================================
echo 环境变量快速配置工具
echo ========================================
echo.
echo 请根据您的实际安装路径修改下面的配置：
echo.
echo 常见的 Java 安装路径：
echo   C:\Program Files\Java\jdk-17.x.x
echo   C:\Program Files\Eclipse Adoptium\jdk-17.x.x
echo.
echo 常见的 Maven 安装路径：
echo   C:\Program Files\Apache\maven
echo   C:\apache-maven-3.9.x
echo.
pause

REM ===== 请修改下面两行路径为您的实际安装路径 =====
set "JAVA_HOME=C:\Program Files\Java\jdk-17"
set "M2_HOME=C:\Program Files\Apache\maven"
REM ===========================================

set "PATH=%JAVA_HOME%\bin;%M2_HOME%\bin;%PATH%"

echo.
echo [配置完成] 正在验证...
echo.

echo Java 版本：
java -version 2>&1 | findstr "version"
echo.

echo Maven 版本：
mvn -version 2>&1 | findstr "Apache Maven"
echo.

echo ========================================
echo 现在可以启动项目了！
echo ========================================
echo.
echo 请选择操作：
echo 1. 直接启动项目
echo 2. 仅测试环境（不启动）
echo.
choice /C 12 /M "请选择"

if errorlevel 2 goto :test_only
if errorlevel 1 goto :start_app

:test_only
echo.
echo 环境配置完成！
echo 下次可以直接运行 run.bat 启动项目
pause
exit /b 0

:start_app
echo.
cd /d d:\aiwork\knowledge-base
call run.bat
