@echo off
setlocal

cd /d "%~dp0"

if not exist "target\classes" (
    mkdir "target\classes"
)

if not exist "dist" (
    mkdir "dist"
)

echo.
echo [1/2] Compilando classes...
pushd "src\main\java"
javac -d "..\..\..\target\classes" *.java
if errorlevel 1 (
    echo Falha na compilacao.
    popd
    pause
    exit /b 1
)
popd

echo.
echo [2/3] Gerando JAR executavel...
jar cfe "dist\RoadGraph-AStar.jar" GamePanel -C "target\classes" .
if errorlevel 1 (
    echo Falha ao gerar o JAR.
    pause
    exit /b 1
)

echo.
echo [3/3] Copiando mapas .osm para a pasta dist...
copy /Y "*.osm" "dist\" >nul

echo.
echo JAR gerado com sucesso:
echo   %cd%\dist\RoadGraph-AStar.jar
echo.
echo Os mapas .osm tambem foram copiados para:
echo   %cd%\dist
pause
