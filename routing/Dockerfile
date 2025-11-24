FROM ubuntu:latest
LABEL authors="dell"
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
ARG JAR_FILE=target/routing-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8000
ENTRYPOINT ["java","-Xms128m","-Xmx512m","-jar","/app/app.jar"]