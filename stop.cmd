@echo off
setlocal enabledelayedexpansion
set "FOUND="
set "KILLED="
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING"') do (
  set "FOUND=1"
  if not defined KILLED (
    set "KILLED=%%p"
    echo 正在结束进程 PID %%p ...
)
  taskkill /PID %%p /F >nul 2>&1
)
if not defined FOUND goto :notrunning
ping -n 3 127.0.0.1 >nul
netstat -ano | findstr ":8080" | findstr "LISTENING" >nul 2>&1
if errorlevel 1 goto :stopped
echo [警告] 8080 还有监听，请到任务管理器确认 java.exe。
exit /b 1
:notrunning
echo 没在跑，8080 上没有监听。
exit /b 0
:stopped
echo 已停止，8080 已释放。
exit /b 0
