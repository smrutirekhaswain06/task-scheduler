# ⚙️ Task Scheduler & Job Monitoring System

A backend job-scheduling engine built in **Java + Spring Boot** that mimics how real production systems (cron pipelines, Celery, Sidekiq, Quartz) run background jobs reliably — with **concurrent execution, automatic retries using exponential backoff, and a live monitoring dashboard**.

> Built to demonstrate core system-engineering concepts: multi-threading, fault tolerance, scheduling, and REST API design.

<img width="900" height="305" alt="image" src="https://github.com/user-attachments/assets/4f978c20-eb95-4037-9a30-27ef25b7b1da" />

---

## ✨ Features

- **Create scheduled jobs** — one-time or recurring — via a REST API or the web dashboard
- **Concurrent execution** using a fixed worker thread pool (`ExecutorService`)
- **Automatic retries with exponential backoff** (2s → 4s → 8s...) when a task fails
- **Live status tracking**: `PENDING → RUNNING → COMPLETED / FAILED`
- **Recurring tasks** automatically re-queue themselves after a successful run
- **Live dashboard** (auto-refreshes every 5s) showing task status, retry counts, and results
- **Zero-setup persistence** using an embedded H2 database (file-based, survives restarts) — with a ready-to-use MySQL config for production

## 🏗️ Architecture

```
┌─────────────┐     REST API      ┌──────────────────┐
│  Dashboard   │ ───────────────▶ │  TaskController   │
│  (HTML/JS)   │ ◀─────────────── │                   │
└─────────────┘     JSON          └─────────┬─────────┘
                                             │
                                    ┌────────▼─────────┐
                                    │   TaskService     │  (CRUD)
                                    └────────┬─────────┘
                                             │
                                    ┌────────▼──────────────┐
                                    │  TaskExecutorService   │
                                    │  ┌──────────────────┐  │
                                    │  │ Poller (every 5s) │  │──▶ finds due PENDING tasks
                                    │  └──────────────────┘  │
                                    │  ┌──────────────────┐  │
                                    │  │ Worker Pool (x5)  │  │──▶ executes task "work"
                                    │  └──────────────────┘  │
                                    │  ┌──────────────────┐  │
                                    │  │ Retry Pool (x2)   │  │──▶ schedules delayed retries
                                    │  └──────────────────┘  │
                                    └────────┬───────────────┘
                                             │
                                    ┌────────▼─────────┐
                                    │   H2 / MySQL DB   │
                                    └───────────────────┘
```

**Why two thread pools?** The worker pool executes jobs immediately; the retry pool schedules *delayed* re-attempts without blocking workers or the poller — this separation is how real distributed schedulers avoid cascading slowdowns under failure.

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3 (Web, Data JPA, Validation) |
| Concurrency | `ExecutorService`, `ScheduledExecutorService` |
| Database | H2 (embedded, file-based) — MySQL-ready |
| Frontend | HTML, CSS, vanilla JavaScript (no framework dependency) |
| Build Tool | Maven |

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Maven 3.6+

### Run locally
```bash
# Clone the repo
git clone https://github.com/<your-username>/task-scheduler.git
cd task-scheduler

# Run the application
mvn spring-boot:run
```

Then open:
- **Dashboard:** http://localhost:8080
- **API base:** http://localhost:8080/api/tasks
- **H2 console** (optional, to browse the DB): http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:file:./data/tasksdb`

### Try it out
1. Create a task on the dashboard with type **"Send Notification"** and `maxRetries = 3`
2. This task type randomly fails ~30% of the time — watch the dashboard show it retry automatically with increasing delays before landing on `COMPLETED` or `FAILED`
3. Create a **recurring** task (e.g. every 60 seconds) and watch it re-schedule itself after every run

## 📡 API Reference

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/tasks` | Create a new task |
| `GET` | `/api/tasks` | List all tasks (optional `?status=PENDING`) |
| `GET` | `/api/tasks/{id}` | Get a single task |
| `DELETE` | `/api/tasks/{id}` | Delete a task |
| `GET` | `/api/tasks/stats` | Get counts by status |

**Example — create a task:**
```json
POST /api/tasks
{
  "name": "Send weekly digest",
  "description": "Emails the weekly summary to all users",
  "type": "SEND_NOTIFICATION",
  "scheduledTime": "2026-09-28T10:00:00",
  "maxRetries": 3,
  "intervalSeconds": 604800
}
```

## 🔮 Future Improvements

- Replace polling with a real message broker (RabbitMQ / Kafka) for true distributed execution
- Add Spring Security with role-based access (Admin vs Viewer)
- Dockerize the app + provide a `docker-compose.yml` with MySQL
- WebSocket-based live updates instead of dashboard polling
- Dead-letter queue for permanently failed tasks

---

*Built as a hands-on project to explore backend scheduling, concurrency, and fault-tolerance patterns in Java.*
