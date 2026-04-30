@echo off
chcp 65001 >nul
echo ========================================
echo 环境变量验证工具
echo ========================================
echo.

echo [检查] JAVA_HOME...
echo %JAVA_HOME%
echo.

echo [检查] MAVEN_HOME...
echo %MAVEN_HOME%
echo.

echo [测试] Java 版本...
java -version 2>&1 | findstr "version"
echo.

echo [测试] Maven 版本...
call mvn -version 2>&1 | findstr "Apache Maven"
echo.

echo ========================================
echo 如果上面显示了版本信息，说明配置成功！
echo ========================================
echo.
pause
