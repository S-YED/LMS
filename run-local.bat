@echo off
echo Setting JAVA_HOME...
set JAVA_HOME=C:\Program Files\Java\jdk-21

echo Starting Mini Leave Management System in local profile...
./mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.jvmArguments="-Dserver.port=8081" -DskipTests

pause