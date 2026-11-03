@echo off
setlocal enabledelayedexpansion
set "ROOT=%~dp0"
if "%ROOT:~-1%"=="\" set "ROOT=%ROOT:~0,-1%"
set "JAR=%ROOT%\backend\target\smart-schedule-1.0.0.jar"
set "HEALTH=http://localhost:8080/api/health"
set "URL=http://localhost:8080/today"
set "LOGDIR=%LOCALAPPDATA%\SmartSchedule\logs"
if not exist "%LOGDIR%" mkdir "%LOGDIR%"
for /f %%d in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMdd"') do set "DAY=%%d"
set "LOG=!LOGDIR!\app-!DAY!.log"

where java >nul 2>nul
if errorlevel 1 goto :nojava

call :health
if "!CODE!"=="200" goto :running

if exist "!JAR!" goto :launch
echo 没找到 jar 包，先自动构建一次...
call "!ROOT!\build.cmd"
if errorlevel 1 goto :buildfail

:launch
echo 正在启动服务，请稍等...
echo 日志文件: !LOG!
powershell -NoProfile -ExecutionPolicy Bypass -Command "Start-Process -FilePath 'cmd.exe' -ArgumentList '/c','java -jar !JAR! >> !LOG! 2>&1' -WindowStyle Hidden"
set /a tries=0
:poll
call :health
if "!CODE!"=="200" goto :open
set /a tries+=1
if !tries! GEQ 45 goto :fail
ping -n 3 127.0.0.1 >nul
goto :poll

:open
start "" "!URL!"
echo 已打开 !URL!
exit /b 0

:running
echo 服务已经在运行了，直接打开浏览器。
goto :open

:nojava
echo [错误] 没找到 java，请先安装 JRE 8 或更高版本，并确保 java 在 PATH 里。
pause
exit /b 1

:buildfail
echo [错误] 构建失败，已停止。
pause
exit /b 1

:fail
echo [错误] 等了 90 秒服务还是没起来。
echo 日志文件: !LOG!
if exist "!LOG!" powershell -NoProfile -Command "Get-Content -LiteralPath '!LOG!' -Tail 20"
pause
exit /b 1

:health
set "CODE=000"
for /f "delims=" %%c in ('curl -s -m 3 -o NUL -w "%%{http_code}" %HEALTH% 2^>NUL') do set "CODE=%%c"
exit /b 0
