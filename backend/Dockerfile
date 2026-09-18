FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 dentalcrm
COPY --from=build /app/target/*.jar app.jar
USER dentalcrm
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]