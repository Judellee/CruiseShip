set JAVABIN=C:\Program Files\Java\jdk-25\bin\

echo Cleaning...
if exist bin rmdir /S /Q bin
mkdir bin

echo Compiling...
"%JAVABIN%javac" --release 17 -cp "lib\*" -d bin -sourcepath src src\cruise\Main.java
if errorlevel 1 goto fail

echo Copying config...
copy /Y db.properties bin\ >nul

echo Extracting JDBC driver...
cd bin
for %%f in (..\lib\*.jar) do "%JAVABIN%jar" xf "%%f"
if exist META-INF\MANIFEST.MF del /Q META-INF\MANIFEST.MF
cd ..

echo Packaging JAR...
echo Main-Class: cruise.Main > manifest.txt
cd bin
"%JAVABIN%jar" cfm ..\..\CruiseMS.jar ..\manifest.txt .
cd ..
del manifest.txt

echo.
echo BUILD SUCCESSFUL - run with: java -jar CruiseMS.jar
goto end

:fail
echo.
echo BUILD FAILED
:end
