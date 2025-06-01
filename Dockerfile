# Используем официальный образ JDK 17
FROM eclipse-temurin:17-jdk-alpine

# Указываем рабочую директорию
WORKDIR /app

# Копируем зависимости и код
COPY target/cloudstorage-0.0.1-SNAPSHOT.jar app.jar

# Указываем порт (для информативности)
EXPOSE 8080

# Команда запуска Spring Boot приложения
ENTRYPOINT ["java", "-jar", "app.jar"]
