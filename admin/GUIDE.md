# 관리자 페이지 구현 가이드

관리자 페이지(상위 이슈 #201) 화면을 나눠 맡은 사람이 읽는 문서다. **Claude에게 작업을 맡길 때 이 파일을 먼저 읽히면 된다.**
예: `admin/GUIDE.md를 읽고 #211 문의 API를 구현해줘`

이미 만들어진 것: 관리자 권한(#202), 관리자 웹 뼈대와 아이디·비밀번호 로그인(#203). 각 화면은 아직 "준비 중"이다.

---

## 1. 담당과 순서

한 사람이 한 페이지를 **백엔드 API부터 화면까지** 끝까지 맡는다. 각자 **API 이슈 → 화면 이슈** 순서로 진행한다.

| 담당 | 페이지 | 이슈 |
|---|---|---|
| 민수 | 인증 서류 | #204 → #207 → #208, #205 |
| 용민 | 문의 + 대시보드 | #211 → #213, #212 / (다른 페이지 머지 후) #216 → #217 |
| 창현 | 캠페인 + 물품 관리 | #209 → #210 / #214 → #215 |

- 이슈 본문에 할 일 체크리스트와 API 모양이 있다. **이슈 본문이 기준**이고, 이 문서는 공통 규칙과 코드 관례를 설명한다.
- 대시보드(#216·#217)는 다른 페이지의 숫자를 모으므로 마지막에 한다.
- 물품 숨기기(#214)는 회원 앱 목록·상세 조회를 바꾸므로 일찍 머지한다.
- 브랜치는 이슈마다 하나(`feature/admin-inquiry-api` 등), PR은 `dev`로. PR 본문에 `close #이슈번호`.

## 2. 정해진 결정 사항 (바꾸지 말 것)

| 항목 | 결정 |
|---|---|
| 구조 | 관리자 웹은 레포 안 별도 앱 `admin/`. API 서버(`backend/`)는 회원 앱과 공유 |
| 관리자 권한 | `User.role` = `USER` / `ADMIN`. **`/admin/**` API는 ADMIN만** (SecurityConfig에서 처리됨) |
| 관리자 로그인 | 아이디·비밀번호 `POST /auth/admin/login`. 로컬 기본 **admin / 1234**, 운영은 환경변수 `ADMIN_LOGIN_ID`·`ADMIN_PASSWORD` |
| 알림 | 만들지 않는다. 회원은 앱에서 처리 결과를 확인한다 (반려 사유, 문의 답변 등) |
| 문의 카테고리 | 거래 · 인증 · 거점 · 기타 = `TRADE` · `VERIFICATION` · `HUB` · `ETC` |
| 캠페인 | 날짜 6개(물품 등록·신청·수령 기간) 구조 유지, 거점 1개 |
| 물품 관리 | **숨기기만**. 신고 기능은 이번 범위 밖 (시안의 "신고 사유" 열은 빼고, "신고된 물품" 카드는 "숨긴 물품"으로) |
| 우선배정 실명 | 신청할 때 직접 입력, 서류와 함께 검토 후 30일 뒤 삭제 |
| 승인 유효기간 | 신입생 = 입학한 해 12월 31일까지(승인할 때 관리자가 입학 연도 선택), 기초수급자 = 승인일 + 1년 |

---

## 3. 로컬 실행

```bash
# 1) DB (Docker Desktop 켠 상태)
cd backend && docker compose up -d

# 2) 백엔드 — local 프로필
cd backend && ./gradlew bootRun --args='--spring.profiles.active=local'

# 3) 관리자 웹 — http://localhost:5174, admin / 1234
cd admin && cp .env.example .env.local && npm install && npm run dev
```

- 회원 앱은 `frontend/`에서 `npm run dev` (5173). 백엔드 CORS 기본값에 5173·5174가 들어 있다.
- 일반 회원 토큰이 필요하면 로컬 전용 `POST /dev/auth/login {"kakaoId":"아무값","nickname":"테스트"}` (local 프로필에서만 열림).
- 백엔드 테스트는 실제 MySQL을 쓴다 (`backend/.env`의 DB). `ProdProfileTest`는 CI처럼 `DB_*`·`JWT_SECRET`·`KAKAO_*` 환경변수가 있어야 통과한다.

---

## 4. 백엔드 관례 (`backend/`)

패키지: `com.Wolgyesangdan.backend.domain.<도메인>.{controller,service,repository,entity,dto,exception}`

### 관리자 API 추가
- **컨트롤러를 따로 만든다**: `domain/<도메인>/controller/Admin<도메인>Controller.java`, `@RequestMapping("/admin/<복수형>")`.
  `/admin/**`라서 권한 확인은 자동이다 (비로그인 401 `AUTH_UNAUTHORIZED`, 일반 회원 403 `AUTH_FORBIDDEN`). SecurityConfig를 고칠 필요 없다.
- 로그인한 관리자 id: `@AuthenticationPrincipal Long userId` (처리자 기록 등에 사용).
- 요청 DTO는 `record` + Bean Validation(`@NotBlank`, `@Size` …), 컨트롤러에서 `@Valid @RequestBody`.
- 목록은 `Pageable`로 받고 `PageResponse.from(page)`(`global/dto/PageResponse`)로 응답한다. 회원 API와 같은 형식이다.
- 서비스는 클래스에 `@Transactional(readOnly = true)`, 쓰기 메서드에만 `@Transactional`.

### 오류
- 도메인별 enum `domain/<도메인>/exception/<도메인>ErrorCode implements BaseErrorCode` (`@Getter @RequiredArgsConstructor`, `(HttpStatus, 메시지)`).
- 던질 때 `throw new BusinessException(XxxErrorCode.CODE)`. 응답 형식은 `GlobalExceptionHandler`가 맞춰 준다:
  `{"status":409,"code":"...","message":"...","errors":[]}`
- 이슈에 적힌 코드 이름을 그대로 쓴다 (예: `VERIFICATION_ALREADY_REVIEWED`, `CAMPAIGN_ALREADY_RUNNING`).

### 엔티티·DB
- 마이그레이션 도구 없이 `ddl-auto: update`다. **컬럼 추가만 자동**이고 삭제·NOT NULL 해제는 안 된다.
- **기존 행이 있는 테이블에 NOT NULL 컬럼을 추가하면 `@ColumnDefault`를 꼭 붙인다.** 예 (`User.role`):
  ```java
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @ColumnDefault("'USER'")
  @Column(nullable = false, length = 30)
  @Builder.Default
  private Role role = Role.USER;
  ```
- **enum 컬럼은 `@Enumerated(STRING)` + `@JdbcTypeCode(SqlTypes.VARCHAR)` + `length = 30`**. 새 enum 컬럼을 만들면 `EnumColumnTypeTest`의 목록과 개수에 추가한다.
- 엔티티는 `@Builder` + 상태를 바꾸는 메서드(예: `approve(...)`, `hide()`)로 바꾼다. setter를 열지 않는다.

### 테스트
- 컨트롤러: `@WebMvcTest(controllers = X.class, properties = "jwt.secret=test-secret-key-that-is-long-enough-for-hs256")` + `@Import({SecurityConfig.class, JwtProvider.class, JwtAuthenticationEntryPoint.class})` + 서비스는 `@MockitoBean`.
  - 관리자 토큰: `"Bearer " + jwtProvider.createAccessToken(1L, Role.ADMIN)`
  - 일반 회원 토큰: `jwtProvider.createAccessToken(1L)` → 관리자 API에서 403 확인
- 서비스·리포지토리: 기존 `*ServiceTest`, `*QueryTest` 형식을 따른다.
- 테스트 이름은 한글 문장 (`관리자가_서류를_승인하면_만료일이_기록된다`).

### S3 (인증 서류 — 민수)
- 물품 사진과 같은 presigned URL 방식 (`ItemImageUploadService` 참고). 서류는 **공개 URL을 주지 않고**, 관리자가 볼 때만 5분짜리 presigned GET을 발급한다.

---

## 5. 관리자 웹 관례 (`admin/`)

Vite + React 19 + TypeScript + Tailwind v4 + react-router 7. 회원 앱과 같은 스택이다.

```
admin/src
├─ api/          client.ts(apiFetch) + 도메인별 API 함수
├─ components/   공통: StatusBadge, Card, FilterChips, DataTable, MaterialIcon, AdminGate
├─ layouts/      AdminLayout(사이드바+상단 바), Sidebar, TopBar
├─ lib/          menu.ts(메뉴·제목·부제), authStorage.ts
├─ pages/        LoginPage, PreparingPage(준비 중 자리)
└─ types/        API 응답 타입
```

### 새 화면 만들기
1. `src/types/<도메인>.ts`에 API 응답 타입 (백엔드 DTO와 같은 이름·형태). 목록은 `src/types/page.ts`의 `PageResponse<T>`로 감싼다
2. `src/api/<도메인>.ts`에 호출 함수 — **반드시 `apiFetch` 사용** (토큰 첨부·재발급·오류 처리 자동)
   ```ts
   export function getAdminInquiries(status?: InquiryStatus, page = 0) {
     const query = new URLSearchParams({ page: String(page) })
     if (status) query.set('status', status)
     return apiFetch<PageResponse<AdminInquirySummary>>(`/admin/inquiries?${query}`)
   }
   ```
3. `src/pages/<도메인>Page.tsx` 작성
4. `src/App.tsx`에서 자기 경로의 `<PreparingPage issue="#..." />` **한 줄만** 실제 페이지로 바꾼다 (다른 사람 줄은 건드리지 않음 → 충돌 최소화)

### 공통 컴포넌트
| 컴포넌트 | 쓰임 |
|---|---|
| `StatusBadge tone="waiting" \| "done" \| "rejected" \| "muted"` | 검토 대기·답변 대기·배정 중 / 승인·답변 완료·신청 가능 / 반려 / 숨김 |
| `Card` | 흰 카드 (r16, 테두리). 표는 패딩 없이 넣는다 |
| `FilterChips` | 목록 위 필터 (검토 대기·승인·반려·전체 등) |
| `DataTable` | 표. `columns`, `rows`, `rowKey`, `onRowClick`+`selectedKey`(목록+상세), `emptyMessage`, `dimmed`(숨긴 행 흐리게) |
| `MaterialIcon name="..."` | Material Symbols Rounded 아이콘 |

- 공통 컴포넌트를 **고쳐야 하면 PR에 적고 팀에 공유**한다. 새 공통 컴포넌트는 추가만 한다.
- 오류 문구: `e instanceof ApiError ? e.message : '…하지 못했어요. 잠시 후 다시 시도해 주세요.'` (서버 메시지를 그대로 보여 준다)
- 사이드바 처리 건수 배지는 `Sidebar`의 `counts` prop으로 넣는다 (대시보드 #217에서 연결).

### 스타일
- Tailwind 클래스 + `src/index.css`의 토큰 이름을 쓴다 (`bg-surface`, `text-label`, `text-label-alt`, `border-border`, `bg-primary`, `bg-table-head`, `border-row-line`, `bg-row-selected`, `text-terracotta` …). 색 hex를 직접 쓰지 않는다.
- 데스크톱 전용(최소 1024px). 모바일 대응 불필요.
- 확인: `npm run lint` · `npm run build` (CI `admin-ci.yml`도 같은 것을 돌린다).

---

## 6. 화면별 디자인 요약 (시안 `handoff_admin` 기준)

시안 원본(`handoff_admin/admin-screens.html`, `README.md`)은 민수에게 받는다. 아래는 구현에 필요한 핵심만 옮긴 것이다.

공통: 본문 카드 `#FFFDF6`·테두리·r16 / 표 헤더 12px 700 / 페이지 제목·부제는 `lib/menu.ts`에 이미 있음.

### 인증 서류 `/verifications` (민수)
- 좌: 필터 칩(검토 대기·승인·반려·전체, 기본 검토 대기) + 표(신청자·유형·서류·신청일·상태), 없으면 "해당하는 서류가 없어요"
- 우: 이름 + 배지, 메타 / 서류 미리보기 300px(클릭 시 원본) / 확인할 것 / 반려 사유 select + [반려] [승인(2배 폭)] / 처리 후 다음 대기 건 자동 선택

### 캠페인 `/campaigns` (창현)
- 좌: 현재 캠페인 폼 — 운영 중 토글, 이름, 소개(textarea), **기간 3쌍**(물품 등록·신청·수령), 거점(이름·주소·운영 시간), [취소] [저장하기]
- 우: "새 캠페인 만들기"(`bg-admin-sidebar`, 예정·진행 중 캠페인이 있으면 비활성) + 지난 캠페인 목록(이름·기간·거래 수·절감량)
- ⚠️ 지금 캠페인의 진행 상태는 DB `status`가 아니라 **날짜로 계산**한다 (`Campaign.statusOn(today)`, `CampaignService`). 운영 중 토글을 어떻게 반영할지(예: 끄면 `ENDED`로 저장하고 날짜 계산보다 우선) 정해서 PR에 적을 것
- 샘플 캠페인(`SampleCampaignInitializer`)은 이미 local 프로필에서만 만들어진다

### 문의 `/inquiries` (용민)
- 좌: 목록 — 카테고리 칩 + 제목(말줄임) + 상태 배지, 아래 이름·날짜, 답변 대기 우선
- 우: 카테고리 + 제목(17px 800), 작성자·날짜, 본문 박스(`bg-screen`)
  - 답변 대기: textarea(120px) + [답변 보내기] / 답변 완료: 왼쪽 3px 초록 선 + "운영자 답변"
- 회원 앱(#212): 마이페이지 "문의하기" 버튼(`frontend/src/pages/MyPage.tsx`의 `onClick: () => {}`)에 연결. 회원 앱 스타일은 `frontend/` 관례를 따른다

### 물품 관리 `/items` (창현)
- 표: 물품(이름 + 등록자·카테고리) · 상태 배지 · 등록일 · [숨기기 / 다시 보이기]. 숨긴 행은 `dimmed` + "숨김" 배지
- 숨기면 회원 앱 목록·검색·홈에서 빠지고 상세는 404 (회원 앱 문구 "볼 수 없는 물품이에요"), 신청 불가. 영구 삭제 없음

### 대시보드 `/` (용민, 마지막)
- 요약 카드 4개(auto-fit, min 200px, 숫자 30px 800): 검토 대기 서류 · 답변 대기 문의 · 숨긴 물품 · 이번 캠페인 거래(`bg-primary-tint`, 클릭 없음). 앞 3개는 클릭 시 해당 페이지
- 최근 들어온 서류 3건 / 최근 문의 3건 + "전체 보기" — 각 목록 API를 `size=3`으로 호출
- 같은 요약으로 사이드바 건수 배지(`Sidebar counts`)도 채운다

---

## 7. 보안 규칙
- 관리자 API는 반드시 `/admin/**` 아래 (권한 확인이 자동으로 걸리는 경로).
- 화면도 `AdminGate`가 막지만 **진짜 보호는 백엔드**다. 화면에서만 숨기고 API를 열어 두지 않는다.
- 서류 파일은 공개 URL 금지, 열람·승인·반려는 감사 로그.
- 관리자 화면에서 기초수급자 여부를 물품·문의 화면에 섞어 보여주지 않는다.
