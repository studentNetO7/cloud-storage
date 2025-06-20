# Облачное хранилище — REST-сервис для загрузки и управления файлами

## О проекте

Данный проект реализует REST-сервис для загрузки, отображения и удаления файлов пользователя.  
Сервис использует JWT авторизацию и предназначен для интеграции с готовым фронтендом (FRONT).

**Проект переработан согласно замечаниям:**
- устранён coupling между слоями (удалена работа с DTO внутри сервисов),
- улучшена атомарность операций загрузки,
- методы сервисов теперь принимают объект User вместо token.
---

## Функционал

- Авторизация пользователя (login/logout) с JWT токеном
- Загрузка файлов с ограничением размера (до 10 МБ)
- Получение списка загруженных файлов пользователя
- Удаление выбранных файлов
- Защищённые все запросы, кроме login
- Поддержка CORS для фронтенда (http://localhost:8081)

---

## Технологии

- Java 17, Spring Boot
- PostgreSQL — база данных пользователей и метаданных файлов
- Хранение файлов в локальной файловой системе
- Docker и Docker Compose для контейнеризации и упрощённого запуска
- Тестирование: JUnit 5, Mockito, Testcontainers
- OpenAPI спецификация (yaml) для описания API

---

## Быстрый старт

### 1. Клонируйте репозиторий
```bash
   git clone https://github.com/studentNetO7/cloud-storage.git
   cd cloud-storage
```

### 2. Запустите фронтенд
Перейдите в папку фронтенда. Установите зависимости и запустите:

```bash

   npm install
   npm run serve
```
В файле .env фронтенда укажите:

```
VUE_APP_BASE_URL=http://localhost:8080
```
Фронтенд будет доступен по адресу: http://localhost:8081 (или ближайший свободный порт).


### 3. Соберите и запустите контейнеры через Docker Compose
```bash
   docker-compose up --build
```
- Сервис будет доступен по адресу: http://localhost:8080

- Контейнер db запустит PostgreSQL с базой данных cloud_storage_docker_container.

- Контейнер app запустит Spring Boot приложение, подключённое к базе.

- Чтобы запустить приложение в боевом режиме, закомментируйте или удалите строку `SPRING_PROFILES_ACTIVE` из `docker-compose.yml`. В этом режиме приложение подключается к контейнерной базе данных Postgres и работает с реальными данными.

### 4.Инициализация базы данных

###### Автоматическое создание таблиц

- При первом запуске контейнера PostgreSQL автоматически выполнит все SQL скрипты из папки ./init (локально), смонтированной в /docker-entrypoint-initdb.d внутри контейнера.

- В папке ./init лежат скрипты создания таблиц и начальных данных (если они нужны).

- Если вы хотите добавить новые таблицы или изменить структуру — добавьте соответствующие SQL скрипты в эту папку перед запуском.

###### Создание таблиц вручную
- Если контейнер не создает таблицы автоматически (например, скрипты отсутствуют), таблицы нужно создать вручную. Для этого: 
- Подключитесь к базе через psql или pgAdmin:

```bash
   psql -h localhost -p 5432 -U postgres -d cloud_storage_docker_container
```
- Выполните SQL скрипты, находящиеся в папке ./init, например:

``` 
CREATE TABLE users (...);
CREATE TABLE files (...);
```

###### Дополнительно
- 
- Файлы загружаются и сохраняются в папку, указанную в настройках upload-dir.

- Если вы хотите, чтобы файлы сохранялись на хост-машине (вне контейнера), раскомментируйте (или добавьте) в docker-compose.yml в сервисе app строку с volume-монтированием, например:

``` 
   # volumes:
   #   - ./uploads:/uploads
``` 
---

## Конфигурация
Все настройки находятся в src/main/resources/application.yml, включая:

- Параметры подключения к базе данных

- Пути для хранения файлов

- Ограничения на загрузку (max file size)

- Порты сервера и логирование

---

## Тестирование

- Для запуска приложения в тестовом режиме необходимо указать профиль test. Это можно сделать, добавив переменную окружения в docker-compose.yml:
```
environment:
SPRING_PROFILES_ACTIVE: test
```
- В тестовом режиме приложение использует отдельную базу данных.
Unit-тесты запускаются через:

```bash
   ./mvnw test
```
- Интеграционные тесты с Testcontainers запускаются автоматически при тестировании.

---

## API

Все методы соответствуют спецификации OpenAPI версии 3.0.0. Полный файл спецификации находится в репозитории под именем `CloudServiceSpecification.yaml`.

Основные доступные эндпоинты:

- `POST /login` — авторизация пользователя
- `POST /logout` — выход из системы
- `POST /file` — загрузка файла
- `GET /file` — скачивание файла
- `DELETE /file` — удаление файла
- `PUT /file` — переименование файла
- `GET /list` — получение списка файлов пользователя

 Для подробной информации и примеров запросов смотрите файл спецификации OpenAPI.

---

## Важные моменты
- Для авторизации необходимо передавать токен в заголовке `auth-token`.

- Logout деактивирует токен, последующие запросы с ним возвращают 401

- CORS настроен для фронтенда на http://localhost:8081

- Для продакшен-окружения рекомендуются доработки по безопасности и хранению файлов

---

## Контакты
По вопросам и предложениям — создавайте issue в репозитории