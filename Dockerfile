FROM maven:3.9-eclipse-temurin-8 AS build

WORKDIR /workspace
COPY backend/pom.xml backend/pom.xml
COPY backend/src backend/src
RUN mvn -f backend/pom.xml -B -DskipTests package

FROM eclipse-temurin:8-jre

WORKDIR /app
COPY --from=build /workspace/backend/target/railway-ticket-risk-system-0.1.0.jar app.jar

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=docker

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
