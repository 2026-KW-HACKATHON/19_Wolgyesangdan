# 월계장터 관리자 웹

운영자가 쓰는 데스크톱 관리자 화면 (최소 폭 1024px). 회원 앱(`frontend/`)과 별도 앱이고 API 서버(`backend/`)는 같이 쓴다.

## 실행

```bash
cp .env.example .env.local   # VITE_API_BASE_URL=http://localhost:8080
npm install
npm run dev                  # http://localhost:5174
```

- 백엔드 CORS 기본값에 `http://localhost:5174`가 들어 있다. 배포 시에는 서버 `CORS_ALLOWED_ORIGINS`에 관리자 웹 주소를 추가한다.
- 로그인: 아이디 · 비밀번호 (`POST /auth/admin/login`). 로컬 기본값 **admin / 1234**, 운영은 서버 환경변수 `ADMIN_LOGIN_ID` · `ADMIN_PASSWORD`.

## 구조

| 경로 | 화면 |
|---|---|
| `/login` | 로그인 |
| `/` | 대시보드 |
| `/verifications` | 인증 서류 |
| `/campaigns` | 캠페인 |
| `/inquiries` | 문의 |
| `/items` | 물품 관리 |

- `src/api/client.ts`는 회원 앱과 같은 API 클라이언트 (토큰 자동 첨부·재발급)
- 디자인 토큰은 회원 앱과 같은 이름·값 + 관리자 전용(`admin-sidebar`, `table-head`, `row-line`, `row-selected`) — `src/index.css`
- 관리자 권한은 백엔드가 확인한다 (`/admin/**`는 ADMIN만). 화면은 `GET /users/me`의 `role`로 한 번 더 막는다
