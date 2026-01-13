# Stage 1: Build stage
# Using Maven with Java 25
FROM maven:3.9.12-eclipse-temurin-25 AS build
WORKDIR /digitalwallet

# Copy only the pom.xml first to leverage Docker layer caching
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime stage
# Using the slim Java 25 JRE image
FROM eclipse-temurin:25-jre-jammy
WORKDIR /digitalwallet

COPY --from=build /digitalwallet/target/*.jar digitalwallet.jar

# Fine-tune Java 25 performance (e.g., using ZGC for low latency)
ENV JAVA_OPTS="-XX:+UseZGC -XX:+ZGenerational"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar digitalwallet.jar"]