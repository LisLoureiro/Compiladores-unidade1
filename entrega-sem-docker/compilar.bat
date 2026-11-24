@echo off
rem ============================================================
rem  Compila o analisador lexico/sintatico (linguagem Sigma).
rem  Requer um JDK instalado (comando javac). No Linux/macOS use
rem  o script compilar.sh.
rem ============================================================
rem  Usa --release 8 quando o JDK suporta (JDK 9+), mantendo o
rem  bytecode compativel com Java 8.
javac --help 2>&1 | findstr /C:"--release" >nul
if errorlevel 1 (
    javac -encoding UTF-8 -d . *.java
) else (
    javac --release 8 -encoding UTF-8 -d . *.java
)
if errorlevel 1 (
    echo.
    echo Falha na compilacao.
) else (
    echo.
    echo Compilacao concluida com sucesso.
)
