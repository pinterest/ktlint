@echo off
setlocal

REM By default this batch file and the "ktlint" are located in same directory. Note that "ktlint" is a JAR
REM despite it is not being suffixed with extension ".jar". Please adjust path below when this batch file
REM and the jar file are located in different directories.
if not defined JAR_PATH set JAR_PATH=%~dp0ktlint

REM The --add-opens argument is needed for java 16+ (see https://github.com/pinterest/ktlint/issues/1986)
java --add-opens=java.base/java.lang=ALL-UNNAMED -jar "%JAR_PATH%" %*

REM With Java24+
REM Suppress warning "sun.misc.Unsafe::objectFieldOffset" on Java24+ (https://github.com/pinterest/ktlint/issues/2973)
REM java --sun-misc-unsafe-memory-access=allow -jar "%JAR_PATH%" %*
