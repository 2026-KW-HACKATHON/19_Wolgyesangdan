# 19_Wolgyesangdan

## 프로젝트 구조

```
.
├── backend/   # Spring Boot (Gradle)
└── frontend/  # React + Vite
```

## Backend

```bash
cd backend
cp .env.example .env   # DB 접속 정보 채우기
docker compose up -d   # MySQL 실행
./gradlew bootRun
```

## Frontend

```bash
cd frontend
npm install
npm run dev
```
