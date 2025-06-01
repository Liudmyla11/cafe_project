# Використання офіційного образу Maven для білду
FROM maven:3.9.6-eclipse-temurin-17 AS build

# Встановлюємо робочу директорію всередині контейнера
WORKDIR /app

# Копіюємо весь код у контейнер
COPY . .

# Виконуємо білд Spring Boot-додатку
RUN mvn clean package -DskipTests

# Використання легкого OpenJDK для фінального контейнера
FROM eclipse-temurin:17-jdk-jammy

# Встановлюємо робочу директорію
WORKDIR /app

# Копіюємо JAR-файл із попереднього контейнера
COPY --from=build /app/target/*.jar app.jar

# Відкриваємо порт (Render автоматично визначає його)
EXPOSE 8080

# Запускаємо Spring Boot-додаток
ENTRYPOINT ["java", "-jar", "app.jar"]