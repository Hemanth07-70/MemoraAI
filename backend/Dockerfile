FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=builder /app/target/memoraai-backend-0.1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx256m", "-Xss512k", "-jar", "app.jar"]
