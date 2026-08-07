@echo off & setlocal enabledelayedexpansion

echo ================================================================
echo =================== Automatic Mod Publishing ===================
echo ================================================================

set /p confirm= release:

echo ================================================================

IF "%confirm%"=="true" (
    echo will be released
) ELSE (
    echo you didn't confirm with "true"
    exit
)

echo ================================================================

echo Are you sure, you want to publish all versions now?
pause

del /F /Q buildAllJars\*
mkdir buildAllJars

@rem Loop trough everything in the version properties folder
for %%f in (versionProperties\*) do (
    @rem Get the name of the version that is going to be compiled
    set version=%%~nf
    @rem Clean out the folders, build it, and publish it
    echo ==================== Cleaning workspace to build !version! ====================
    call .\gradlew.bat clean -PmcVer="!version!" --no-daemon
    del fabric\build\libs\*.jar
    del /F /Q fabric\build
    del forge\build\libs\*.jar
    del /F /Q forge\build
    del neoforge\build\libs\*.jar
    del /F /Q neoforge\build
    echo ==================== Building and Publishing !version! ==================
    call .\gradlew.bat publishMods -PmcVer="!version!" --no-daemon
    echo ==================== Copying jars ====================
    copy fabric\build\libs\*.jar buildAllJars\
    copy forge\build\libs\*.jar buildAllJars\
    copy neoforge\build\libs\*.jar buildAllJars\
    echo ==================== Deleting unnecessary *-all.jars ====================
    del /F /Q buildAllJars\*-all.jar
)

echo ================================================================
echo =========================== FINISHED ===========================
echo ================================================================

endlocal
