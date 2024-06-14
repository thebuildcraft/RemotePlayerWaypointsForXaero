@echo off & setlocal enabledelayedexpansion

echo ================================================================
echo =================== Automatic Mod Publishing ===================
echo ================================================================

rem needs special handling... set /p github= release to github:
set /p modrinth= release to modrinth:
set /p curseforge= release to curseforge:

echo ================================================================

rem needs special handling... IF %github%==true echo will be released to github
IF %modrinth%==true echo will be released to modrinth
IF %curseforge%==true echo will be released to curseforge

echo ================================================================

echo Are you sure, you want to publish all versions now?
pause

@rem Loop trough everything in the version properties folder
for %%f in (versionProperties\*) do (
    @rem Get the name of the version that is going to be compiled
    set version=%%~nf
    @rem Clean out the folders, build it, and merge it
    echo ==================== Cleaning workspace to build !version! ====================
    call .\gradlew.bat clean -PmcVer="!version!" --no-daemon
    del fabric\build\libs\*.jar
    del /F /Q fabric\build
    del forge\build\libs\*.jar
    del /F /Q forge\build
    del neoforge\build\libs\*.jar
    del /F /Q neoforge\build
    del spigot\build\libs\*.jar
    del /F /Q spigot\build
    echo ==================== Building !version! ====================
    call .\gradlew.bat build -PmcVer="!version!" --no-daemon
    echo ==================== Publishing !version! ==================
    rem call .\gradlew.bat publishMod -PmcVer="!version!" --no-daemon
    rem needs special handling... IF %github%==true call .\gradlew.bat publishGitHub -PmcVer="!version!" --no-daemon
    IF %modrinth%==true call .\gradlew.bat publishModrinth -PmcVer="!version!" --no-daemon
    IF %curseforge%==true call .\gradlew.bat publishCurseforge -PmcVer="!version!" --no-daemon
)

echo ================================================================
echo =========================== FINISHED ===========================
echo ================================================================

endlocal
