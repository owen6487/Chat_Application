# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Explicitly tell the container to map Render variables to Spring properties
ENV MONGO_URI=${MONGO_URI}
ENV GROQ_API_KEY=${GROQ_API_KEY}
ENV GROQ_API_URL=${GROQ_API_URL}
ENV GROQ_MODEL=${GROQ_MODEL}
ENV GROQ_MAX_TOKENS=${GROQ_MAX_TOKENS}

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]