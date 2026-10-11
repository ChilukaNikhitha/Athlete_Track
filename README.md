# AthleteTrack

AthleteTrack is split into two independent projects:

```text
Athlete_Track/
├── frontend/                 React 19 + Vite
│   ├── src/                  UI, API client, player/coach screens
│   ├── Dockerfile
│   └── package.json
├── backend/                  Java 21 + Spring Boot
│   ├── src/main/java/        REST API, security, domain modules
│   ├── src/main/resources/   Spring configuration
│   ├── Dockerfile
│   └── pom.xml
├── compose.yaml              MySQL + API + frontend
├── package.json              Root shortcuts for frontend npm commands
└── .env.example              Local Docker settings (development only)
```

## Run everything with Docker

Requirements: Docker Desktop with Docker Compose.

```powershell
Copy-Item .env.example .env
docker compose up --build
```

Then open `http://localhost:5173`. The API is at `http://localhost:8080`; the MySQL container is exposed on port `3307`. MySQL data is stored in the named Docker volume `athlete_data` and survives container restarts. `docker compose down` stops the services without removing the database volume. To deliberately remove the database and all stored accounts/data, run `docker compose down -v`.

Compose waits for MySQL and the API readiness check before starting the frontend. `GET /api/health` returns `200` when the API can reach its database and `503` otherwise.

The checked-in `.env.example` values are **development-only**. Set a new random Base64-encoded JWT key (at least 32 decoded bytes), strong database passwords, and a private coach invitation code in `.env` before using the app outside a local machine. Never commit `.env` or share the invitation code publicly.

## Run locally without Docker

Requirements: Node.js 20+, Java 21, Maven 3.9+, and MySQL 8+.

1. Start MySQL and create the `athletetrack` database/user, or start only the database service using `docker compose up -d database`.
2. In PowerShell set backend variables and launch Spring Boot:

   ```powershell
   $env:JWT_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
   $env:DB_URL = "jdbc:mysql://localhost:3307/athletetrack?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
   $env:DB_USERNAME = "athletetrack"
   $env:DB_PASSWORD = "athletetrack_dev"
   # Optional: configure a private invitation code to enable coach registration.
   $env:COACH_REGISTRATION_CODE = "replace-with-a-strong-private-value"
   cd backend
   mvn spring-boot:run
   ```

3. In a second terminal, from the project root, install and run the frontend:

   ```powershell
   npm run install:frontend
   npm run dev
   ```

   The frontend is served at `http://localhost:5173`. You can also run `npm ci` and `npm run dev` from inside `frontend/`.

For a disposable in-memory development database, start Spring Boot with `-Dspring-boot.run.profiles=h2`. H2 data is lost when that backend process stops. MySQL is the default configuration for normal runs.

## Features

- Player/coach dashboards and role-based screens.
- Player registration and role-specific player/coach JWT login. Player registration cannot assign a coach role.
- Sign-in is preserved for the current browser tab and cleared on sign-out.
- Optional coach registration with an administrator-configured invitation code.
- Player profile and BMI, performance records, and wellness check-ins.
- Coach player assignments, assigned-player performance review, tournament fixtures, training/nutrition plans, and feedback.
- Coaches can create tournament fixtures for assigned players; players see only fixtures on their own tournament schedule.
- Dashboard summaries based on saved performance, wellness, and assigned-player data.
- MySQL persistence using Spring Data JPA/Hibernate.

Configure `COACH_REGISTRATION_CODE` to allow coaches to register at `/api/auth/register-coach`; provide the code privately to intended coaches. Anyone with the active code can create a coach account, so use a strong random value and rotate it by changing the environment variable and restarting the backend. Leave it unset to disable coach self-registration. Existing accounts can also be promoted by a trusted database administrator:

```sql
UPDATE user_accounts SET role = 'COACH' WHERE email = 'coach@example.com';
```

## API routes

All protected routes require `Authorization: Bearer <token>`.

| Method | Route | Access | Purpose |
| --- | --- | --- | --- |
| GET | `/api/health` | Public | Health status |
| POST | `/api/auth/register` | Public | Register player |
| POST | `/api/auth/register-coach` | Public, invitation code required | Register coach when `COACH_REGISTRATION_CODE` is configured |
| POST | `/api/auth/login` | Public | Sign in with `email`, `password`, and the selected `role` (`PLAYER` or `COACH`) |
| GET | `/api/auth/me` | Valid bearer token | Check the current account ID and role; returns `401` without a valid token |
| GET, PUT | `/api/profile` | Player | Read/update profile and calculated BMI |
| GET, POST | `/api/performance` | Player | Read/log performance |
| GET, POST | `/api/wellness` | Player | Read/submit wellness check-ins |
| GET, POST | `/api/coach/players` | Coach | List assigned players / assign by a player's registered email or account ID |
| GET | `/api/coach/players/{playerId}/performance` | Coach assigned to player | View the assigned player’s recent performance records |
| GET | `/api/tournaments` | Player/Coach | View personal tournament schedule / coach-created fixtures |
| POST | `/api/tournaments` | Coach assigned to player | Add a tournament fixture for an assigned player |
| GET, POST | `/api/plans` | Player/Coach | View own plans / create for assigned players |
| GET, POST | `/api/feedback` | Player/Coach | View own feedback / send to assigned players |

The overview provides role-specific shortcuts and API-backed summaries; profile, performance, wellness, plan, and feedback records are loaded from the API. Empty lists display an empty state rather than example records.
