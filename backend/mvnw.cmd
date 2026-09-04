@REM Wrapper minimo de Maven para Windows.
@REM   1. Si hay mvn en el PATH, lo usa.
@REM   2. Si no, descarga una vez la distribucion de .mvn\wrapper\maven-wrapper.properties.
@REM Se puede reemplazar por el wrapper oficial con: mvn -N wrapper:wrapper
@echo off
setlocal

where mvn >nul 2>nul
if %ERRORLEVEL%==0 (
  mvn %*
  exit /b %ERRORLEVEL%
)

set "BASE=%~dp0"
set "PROPS=%BASE%.mvn\wrapper\maven-wrapper.properties"
if not exist "%PROPS%" (
  echo Falta %PROPS%
  exit /b 1
)

for /f "usebackq tokens=1,* delims==" %%A in ("%PROPS%") do (
  if "%%A"=="distributionUrl" set "URL=%%B"
)
for %%F in ("%URL%") do set "ZIP=%%~nxF"
set "VER=%ZIP:apache-maven-=%"
set "VER=%VER:-bin.zip=%"
set "DEST=%USERPROFILE%\.m2\wrapper\dists\apache-maven-%VER%"

if not exist "%DEST%\bin\mvn.cmd" (
  echo Descargando Apache Maven %VER%...
  powershell -NoProfile -Command "New-Item -ItemType Directory -Force -Path '%DEST%.tmp' ^| Out-Null; Invoke-WebRequest -Uri '%URL%' -OutFile '%DEST%.tmp\maven.zip'; Expand-Archive -Path '%DEST%.tmp\maven.zip' -DestinationPath '%DEST%.tmp' -Force; Move-Item '%DEST%.tmp\apache-maven-%VER%' '%DEST%'; Remove-Item -Recurse -Force '%DEST%.tmp'"
)

"%DEST%\bin\mvn.cmd" %*
exit /b %ERRORLEVEL%
