@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM Begin all REM://
@echo off

@REM Set the current directory to the location of this script
set MAVEN_PROJECTBASEDIR=%~dp0
if "%MAVEN_PROJECTBASEDIR:~-1%"=="\" set MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%

@REM Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto init

echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH. >&2
echo Please set the JAVA_HOME variable in your environment to match the >&2
echo location of your Java installation. >&2
goto error

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%\bin\java.exe

if exist "%JAVA_EXE%" goto init

echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME% >&2
echo Please set the JAVA_HOME variable in your environment to match the >&2
echo location of your Java installation. >&2
goto error

:init
@REM Determine Maven wrapper jar
set WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
set WRAPPER_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"

if exist %WRAPPER_JAR% goto runMaven

@REM Download Maven Wrapper jar
echo Downloading Maven Wrapper...
powershell -Command "(New-Object Net.WebClient).DownloadFile('%WRAPPER_URL:"=%', '%WRAPPER_JAR:"=%')"
if %ERRORLEVEL% neq 0 (
    echo ERROR: Failed to download Maven Wrapper >&2
    goto error
)

:runMaven
@REM Set the MAVEN_OPTS
set WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

"%JAVA_EXE%" ^
  "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
  %MAVEN_OPTS% ^
  -classpath %WRAPPER_JAR% ^
  %WRAPPER_LAUNCHER% %*

if %ERRORLEVEL% neq 0 goto error
goto end

:error
set ERROR_CODE=1

:end
@endlocal & set ERROR_CODE=%ERROR_CODE%
cmd /C exit /B %ERROR_CODE%
