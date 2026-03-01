# Mall Lease

Desktop-приложение для управления арендой торговых площадей. Написано на JavaFX, данные хранятся в PostgreSQL.

[![CI](https://img.shields.io/github/actions/workflow/status/YOUR_USERNAME/YOUR_REPO/ci.yml?branch=main&style=flat-square&label=build)](../../actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21.0.2-blue?style=flat-square)](https://openjfx.io/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?style=flat-square&logo=postgresql&logoColor=white)](https://www.postgresql.org/)

---

## Стек

- Java 21, JavaFX 21.0.2, FXML + CSS
- PostgreSQL 15 через Docker (порт 5433)
- HikariCP, BCrypt (favre), Jackson, Ikonli (Feather), Logback
- Maven + Shade plugin (fat JAR)

## Архитектура

MVC: контроллеры — тонкие, бизнес-логика — в сервисах, SQL — в DAO через `BaseDao<T>` и `PreparedStatement`. Никакого Hibernate.

```
controller/ → service/ + dao/ → HikariCP → PostgreSQL
```

## Быстрый старт

Нужны: JDK 21+, Maven 3.9+, Docker с Compose v2.

```bash
make db-up      # поднять базу (схема + сид применяются автоматически)
make run        # собрать и запустить через maven javafx:run
```

Или запустить fat JAR:

```bash
make build
make run-jar
```

Все таргеты: `build`, `run`, `run-jar`, `clean`, `db-up`, `db-down`, `db-reset`.

## Тестовые пользователи

| Логин | Пароль | Роль |
|---|---|---|
| `admin` | `Admin123!` | Администратор |
| `manager` | `Manager1!` | Менеджер |
| `client` | `Client123!` | Клиент |

## Структура проекта

```
src/main/java/com/malllease/
├── config/        подключение к БД (HikariCP)
├── model/         доменные сущности
├── dao/           SQL-запросы (BaseDao + PreparedStatement)
├── service/       бизнес-логика
├── controller/    FXML-контроллеры (один на экран)
└── ui/            кастомные контролы

src/main/resources/
├── fxml/          разметка экранов
├── css/           стили (один файл)
├── db/            schema.sql + seed.sql
└── maps/          JSON-планировки этажей
```

## База данных

Схема: `role → users → client`, `shopping_center → trade_point`, `showing → contract → contract_rental → monthly_charges → payment`.

Ключевые особенности:
- Статус точки (free/occupied/unavailable) **не хранится** — вычисляется SQL `CASE` по активным арендам
- `EXCLUDE USING gist` — запрет пересекающихся аренд на уровне БД
- Помесячные начисления генерируются через `generate_series` при создании договора

## Роли

| Экран | Клиент | Менеджер | Админ |
|---|:---:|:---:|:---:|
| Интерактивная карта | ✓ | ✓ | ✓ |
| Запросить показ | ✓ | — | — |
| Управление показами и договорами | — | ✓ | ✓ |
| Платежи | — | ✓ | ✓ |
| Мои договоры и история платежей | ✓ | — | — |
| CRUD всех справочников | — | — | ✓ |

## Конфигурация

`src/main/resources/application.properties`:

```properties
db.url=jdbc:postgresql://localhost:5433/mall_lease
db.user=mall_lease
db.password=mall_lease_dev
db.pool.maximumPoolSize=10
```

## Git

```
main       — стабильные релизы (тэги)
develop    — интеграция
feature/*  — новый функционал
fix/*      — фиксы
```

Формат коммитов: `feat: ...`, `fix: ...`, `refactor: ...`, `chore: ...`
