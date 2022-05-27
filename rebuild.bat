SET PATH=%PATH%;C:\apache-maven-3.8.4\bin
call mvn clean
call mvn install -DskipTests
call mvn install