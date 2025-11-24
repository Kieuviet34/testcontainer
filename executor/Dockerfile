FROM ubuntu:latest
LABEL authors="dell"
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
ARG JAR_FILE=target/executor-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
RUN apt-get update && apt-get install -y --no-install-recommends docker.io && rm -rf /var/lib/apt/lists/*
EXPOSE 8081
ENTRYPOINT ["java","-Xms128m","-Xmx512m","-jar","/app/app.jar"]