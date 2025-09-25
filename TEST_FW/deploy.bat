@echo off

:: Variables
set APP_NAME=TestFramework
set SRC_DIR=src\main\java
set WEB_DIR=src\main\webapp
set BUILD_DIR=build
set LIB_DIR=lib
set TOMCAT_WEBAPPS=C:\Program Files\Apache Software Foundation\Tomcat 10.1\webapps
set SERVLET_API_JAR=%LIB_DIR%\servlet-api.jar
set FRONT_SERVLET_JAR=%LIB_DIR%\FrameworkServlet.jar

:: Vérifier que la librairie FrontServlet existe
if not exist %FRONT_SERVLET_JAR% (
    echo Erreur: %FRONT_SERVLET_JAR% n'existe pas!
    echo Executez d'abord deploy-lib.bat dans le projet FrontServlet
    pause
    exit /b 1
)

:: Vérifier que web.xml existe
if not exist %WEB_DIR%\WEB-INF\web.xml (
    echo Erreur: web.xml introuvable dans %WEB_DIR%\WEB-INF\
    echo Assurez-vous que le fichier web.xml existe
    pause
    exit /b 1
)

:: Nettoyage
if exist %BUILD_DIR% (
    rmdir /s /q %BUILD_DIR%
)
mkdir %BUILD_DIR%\WEB-INF
mkdir %BUILD_DIR%\WEB-INF\classes
mkdir %BUILD_DIR%\WEB-INF\lib

:: Compilation avec les deux JARs
@REM echo Compilation de l'application Test...
@REM dir /b /s %SRC_DIR%\*.java > sources.txt
@REM javac -cp "%SERVLET_API_JAR%;%FRONT_SERVLET_JAR%" -d %BUILD_DIR%\WEB-INF\classes @sources.txt
@REM if errorlevel 1 (
@REM     echo Erreur de compilation!
@REM     del sources.txt
@REM     pause
@REM     exit /b 1
@REM )
@REM del sources.txt

:: Copier les librairies
copy /Y %LIB_DIR%\*.jar %BUILD_DIR%\WEB-INF\lib\

:: Copier TOUS les fichiers web (y compris le web.xml existant)
if exist %WEB_DIR% (
    echo Copie des fichiers web...
    xcopy /E /I /Y %WEB_DIR%\* %BUILD_DIR%\
)

:: Création du WAR
echo Creation du WAR %APP_NAME%.war...
cd %BUILD_DIR%
jar -cvf %APP_NAME%.war *
cd ..

:: Déploiement vers Tomcat
echo Deploiement vers Tomcat...
copy /Y "%BUILD_DIR%\%APP_NAME%.war" "%TOMCAT_WEBAPPS%\"

echo.
echo Déploiement terminé avec succes!
echo Application accessible sur: http://localhost:8080/%APP_NAME%/
echo.
pause