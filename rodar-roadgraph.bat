@echo off
setlocal
setlocal EnableDelayedExpansion

cd /d "%~dp0"

if not exist "target\classes" (
    mkdir "target\classes"
)

echo.
echo [1/2] Compilando arquivos Java...
pushd "src\main\java"
javac -d "..\..\..\target\classes" *.java
if errorlevel 1 (
    echo.
    echo Falha na compilacao. Verifique se o Java JDK esta instalado e no PATH.
    popd
    pause
    exit /b 1
)
popd

echo.
echo [2/2] Escolha o mapa .osm:
set /a count=0
for %%F in (*.osm) do (
    set /a count+=1
    set "mapa[!count!]=%%F"
    echo   !count!^) %%F
)

if %count%==0 (
    echo Nenhum arquivo .osm encontrado na pasta.
    pause
    exit /b 1
)

set "escolha="
set /p escolha=Digite o numero do mapa [1-%count%] (Enter = 1): 
if "%escolha%"=="" set "escolha=1"

set "mapaEscolhido=!mapa[%escolha%]!"
if "!mapaEscolhido!"=="" (
    echo Opcao invalida. Usando mapa 1.
    set "mapaEscolhido=!mapa[1]!"
)

echo.
echo Executando RoadGraph com mapa: !mapaEscolhido!
java -cp "target\classes" GamePanel "!mapaEscolhido!"

echo.
echo Aplicacao finalizada.
pause
