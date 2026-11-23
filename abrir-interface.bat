@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0"

set "PY=python"
where python >nul 2>nul || set "PY=py"
%PY% --version >nul 2>nul
if errorlevel 1 (
    echo Python 3 nao encontrado no PATH.
    echo Instale o Python 3 e execute novamente.
    pause
    exit /b 1
)

echo Iniciando a interface do analisador Sigma...
%PY% servidor.py %*
if errorlevel 1 pause
endlocal
