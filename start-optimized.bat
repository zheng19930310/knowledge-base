@echo off
chcp 65001 >nul
echo ========================================
echo 知识库系统启动（优化版）
echo ========================================
echo.

echo JVM内存配置: -Xms512m -Xmx1024m
echo CPU优化: 每chunk暂停500ms
echo 访问地址: http://localhost:8080
echo.

java -Xms512m -Xmx1024m -XX:+UseG1GC -jar target\knowledge-base-1.0.0.jar

pause
