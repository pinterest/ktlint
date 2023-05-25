@echo off
REM By default this batch file and the "ktlint.jar" are located in same directory. Please adjust path below when this batch
REM file and the jar file are located in different directories.
if not defined JAR_PATH set JAR_PATH=.\ktlint.jar

REM The --add-opens argument is needed for java 16+ (see https://github.com/pinterest/ktlint/issues/1986)
java --add-opens=java.base/java.lang=ALL-UNNAMED -jar "%JAR_PATH%" %*
