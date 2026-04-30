@echo off
echo ========================================
echo Qdrant 矢量数据库启动脚本
echo ========================================
echo.

REM Check if qdrant.exe exists
if not exist "qdrant.exe" (
    echo [错误] 未找到 qdrant.exe 文件
    echo.
    echo 请先下载 Qdrant:
    echo 1. 访问 https://github.com/qdrant/qdrant/releases
    echo 2. 下载最新版本的 Windows 版本
    echo 3. 解压并将 qdrant.exe 放在此目录
    echo.
    pause
    exit /b 1
)

echo [信息] 正在启动 Qdrant...
echo [信息] 数据将存储在 ./qdrant_storage 目录
echo.

REM Start Qdrant with persistent storage
start "Qdrant Server" qdrant.exe --storage-path ./qdrant_storage

echo [成功] Qdrant 已启动
echo [信息] Web UI: http://localhost:6333/dashboard
echo [信息] API 端点: http://localhost:6333
echo.
echo 按任意键关闭此窗口（Qdrant将继续在后台运行）
pause > nul
