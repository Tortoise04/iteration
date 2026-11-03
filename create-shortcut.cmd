@echo off
setlocal
set "ROOT=%~dp0"
if "%ROOT:~-1%"=="\" set "ROOT=%ROOT:~0,-1%"
set "SC_ROOT=%ROOT%"
powershell -NoProfile -ExecutionPolicy Bypass -Command "$d=[Environment]::GetFolderPath('Desktop'); $p=Join-Path $d 'SmartSchedule.lnk'; $w=New-Object -ComObject WScript.Shell; $s=$w.CreateShortcut($p); $s.TargetPath=(Join-Path $env:SC_ROOT 'start.cmd'); $s.WorkingDirectory=$env:SC_ROOT; $s.IconLocation='%SystemRoot%\System32\shell32.dll,43'; $s.Description='SmartSchedule 学习任务书'; $s.Save(); Write-Output ('OK ' + $p)"
if errorlevel 1 goto :bad
echo 已生成桌面快捷方式 SmartSchedule。
exit /b 0
:bad
echo [错误] 生成快捷方式失败。
exit /b 1
