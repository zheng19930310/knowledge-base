@echo off
chcp 65001 >nul
echo ========================================
echo 知识库系统启动
echo ========================================
echo.

echo 正在启动应用...
echo JVM内存配置: -Xms512m -Xmx1024m (优化内存使用)
echo 访问地址: http://localhost:8080
echo.

mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xms512m -Xmx1024m"

pause
