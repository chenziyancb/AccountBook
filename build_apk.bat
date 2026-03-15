@echo off
setlocal
set JAVA_HOME=C:\jdk-17
set ANDROID_HOME=C:\Users\Administrator\AppData\Local\Android\Sdk
set ANDROID_SDK_ROOT=C:\Users\Administrator\AppData\Local\Android\Sdk
set PATH=C:\jdk-17\bin;C:\base\gradle-9.0.0\bin;%PATH%
cd /d C:\Users\Administrator\WorkBuddy\Claw
gradle assembleDebug --no-daemon --info > C:\Users\Administrator\WorkBuddy\Claw\build_log.txt 2>&1
echo Exit code: %ERRORLEVEL% >> C:\Users\Administrator\WorkBuddy\Claw\build_log.txt
endlocal
