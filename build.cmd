@echo off
setlocal
set "ROOT=%~dp0"
if "%ROOT:~-1%"=="\" set "ROOT=%ROOT:~0,-1%"
echo [1/2] 构建前端...
cd /d "%ROOT%\frontend"
if errorlevel 1 goto :badcd
if exist node_modules goto :npmbuild
echo 首次构建，先安装前端依赖，只做一次，比较慢...
call npm install
if errorlevel 1 goto :badnpm
:npmbuild
call npm run build
if errorlevel 1 goto :badbuild
echo [2/2] 打包后端...
cd /d "%ROOT%\backend"
if errorlevel 1 goto :badcd
call mvn -DskipTests clean package
if errorlevel 1 goto :badmvn
echo.
echo 构建完成: %ROOT%\backend\target\smart-schedule-1.0.0.jar
exit /b 0
:badcd
echo [错误] 进不去子目录。
pause
exit /b 1
:badnpm
echo [错误] npm install 失败。
pause
exit /b 1
:badbuild
echo [错误] 前端构建失败，npm run build。
pause
exit /b 1
:badmvn
echo [错误] 后端打包失败，mvn package。
pause
exit /b 1
