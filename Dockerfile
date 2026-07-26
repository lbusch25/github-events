FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
COPY src src
RUN ./mvnw package -DskipTests -q

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
