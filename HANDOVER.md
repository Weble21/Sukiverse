# Sukiverse 백엔드 인수인계서

> 작성일: 2026-06-28
> 대상: 차기 담당자
> 인계자: (이전 담당자)

---

## 1. 프로젝트 개요

**Sukiverse**는 애니메이션 정보 기반 서비스의 **백엔드 API 서버**입니다.

- 소셜 로그인(Google / Kakao / Naver) + 자체 JWT 인증
- 외부 [Jikan API](https://docs.api.jikan.moe/)(MyAnimeList 비공식 API)로 애니메이션 Top 100 데이터 동기화
- 애니메이션을 **시리즈(Anime) — 시즌(AnimeSeason)** 2계층으로 관리

현재 두 개 도메인만 구현되어 있습니다: **auth(인증)**, **anime(애니메이션)**.

---

## 2. 기술 스택

| 항목 | 내용 |
|------|------|
| 언어 | Kotlin 2.2.21 (JVM toolchain 17) |
| 프레임워크 | Spring Boot 4.0.6 |
| 빌드 | Gradle (Groovy DSL, `build.gradle`) |
| DB | PostgreSQL 16 (운영: AWS RDS / 로컬: Docker) |
| ORM | Spring Data JPA (Hibernate, `ddl-auto: update`) |
| 인증 | 자체 JWT(jjwt 0.12.3) + OAuth2 소셜 로그인 |
| HTTP 클라이언트 | OkHttp 4.12.0 (외부 OAuth / Jikan 호출) |
| API 문서 | springdoc-openapi (Swagger UI) |
| 배포 | Docker + nginx + AWS EC2(t3.micro) + RDS |

---

## 3. 디렉토리 구조

```
src/main/kotlin/com/example/Sukiverse/
├── SukiverseApplication.kt        # 진입점
├── common/
│   ├── httpClient/CallClient.kt   # OkHttp 래퍼 (GET/POST)
│   ├── json/JsonUtil.kt           # Jackson 직렬화 유틸
│   └── jwt/JwtProvider.kt         # JWT 발급/검증
├── config/
│   ├── OAuth2Config.kt            # oauth2.providers.* 바인딩
│   ├── OkHttpClientConfig.kt
│   └── SwaggerConfig.kt
├── domain/
│   ├── anime/                     # 애니메이션 도메인 (controller/service/repository/dto)
│   └── auth/                      # 인증 도메인 (controller/service/repository)
│       └── service/               # AuthService + Google/Kakao/Naver 별 OAuth 서비스
├── exception/
│   ├── CustomException.kt
│   └── ErrorCode.kt               # 에러 코드 enum
├── interfaces/OAuth.kt            # OAuthServiceInterface 등 공용 인터페이스
├── security/
│   ├── filter/JWTFilter.kt        # 요청별 JWT 인증 필터
│   └── web/WebSecurity.kt         # Spring Security 설정
└── types/
    ├── dto/Response.kt            # ApiResponse 표준 응답 래퍼
    └── entity/                    # Anime / AnimeSeason / User 엔티티
```

---

## 4. 도메인 모델 (DB 스키마)

JPA `ddl-auto: update`로 자동 생성됩니다. 주요 테이블 3개:

### `auth_users` (User)
| 컬럼 | 설명 |
|------|------|
| `ulid` (PK) | UUID 문자열 (코드상 `UUID.randomUUID()` 사용) |
| `provider` | google / kakao / naver |
| `provider_id` | 소셜 제공자가 부여한 사용자 ID |
| `email`, `nickname`, `profile_image` | 프로필 정보 |
| `is_onboarding_completed` | 온보딩 완료 여부 |
| `access_token`, `refresh_token` | 최신 발급 토큰 저장 |
| `created_at` | 생성 시각 |

> `(provider, provider_id)` 조합으로 기존 사용자를 조회합니다.

### `anime` (Anime — 시리즈)
- `title`(unique), `genre`, `rank`, `score`, `image_url`
- `rank` = 소속 시즌 중 **최고 순위(MIN)**, `score` = 소속 시즌 중 **최고 점수(MAX)** → `recalcSeries()`로 재계산
- 시즌과 1:N (`cascade = ALL`, `orphanRemoval = true`)

### `anime_season` (AnimeSeason — 시즌)
- `title`, `year`, `rank`, `score`
- `mal_id`(unique) — **Jikan/MAL 식별자, 동기화 매칭 키**
- `anime_id` (FK → anime)

---

## 5. 인증 흐름 (중요)

```
[클라이언트]
  1) 소셜 제공자에서 authorization code 획득
  2) POST /api/v1/auth/login { provider, code }
        ↓
[AuthService.handleAuth]
  3) provider별 OAuthService가 code → access_token → 사용자 정보 조회
  4) (provider, provider_id)로 User 조회/생성
  5) JWT accessToken(1h) + refreshToken(30d) 발급
        ↓
[응답]
  - accessToken: 응답 바디로 전달
  - refreshToken: HttpOnly 쿠키 (Secure, SameSite=Strict, path=/api/v1/auth/refresh)
```

- **Access Token 만료(1시간)** 시 → `POST /api/v1/auth/refresh` (쿠키의 refreshToken 사용) → 새 accessToken 발급
- 보호된 API는 `Authorization: Bearer <accessToken>` 헤더 필요. `JWTFilter`가 검증.
- JWT 서명: HMAC-SHA, 시크릿은 `jwt.secret` 환경변수.

### OAuth 서비스 구조
`OAuthServiceInterface`를 구현한 `GoogleAuthService` / `KakaoAuthService` / `NaverAuthService`가 있고,
`@Service("google")` 등 **빈 이름 = provider 키**로 등록되어 `AuthService`가 `Map<String, OAuthServiceInterface>`로 주입받아 분기합니다. 새 제공자 추가 시 인터페이스 구현 + `@Service("이름")`만 추가하면 됩니다.

---

## 6. API 목록

Base URL(운영): `https://sukiverse.duckdns.org`

### 인증 (`/api/v1/auth`)
| 메서드 | 경로 | 인증 | 설명 |
|--------|------|------|------|
| POST | `/login` | 공개 | 소셜 로그인 (`{provider, code}`) |
| GET | `/me` | Bearer | 내 정보 조회 |
| POST | `/refresh` | 쿠키 | accessToken 재발급 (Swagger에서 숨김) |

### 애니메이션 (`/api/v1/anime`) — **현재 전부 공개(permitAll)**
| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/` | 전체 목록 (`?sort=rank`(기본) / `score`) |
| GET | `/{id}` | 단건 조회 |
| POST | `/` | 생성 |
| PATCH | `/{id}` | 수정 |
| DELETE | `/{id}` | 삭제 |
| POST | `/sync/top` | Jikan Top 100 동기화 (4페이지 × 25, 페이지당 0.5s 딜레이) |

> ⚠️ `/api/v1/anime/**`가 `WebSecurity`에서 `permitAll`로 열려 있습니다. 생성/수정/삭제/동기화까지 **인증 없이 호출 가능**한 상태이므로, 운영 전 권한 정책 검토가 필요합니다. (아래 8번 참고)

API 문서: 운영 Swagger UI는 nginx가 `/swagger-ui`, `/v3/api-docs`를 백엔드로 프록시합니다.

---

## 7. 로컬 개발 환경 설정

### 7-1. 사전 준비
- JDK 17, Docker Desktop

### 7-2. 환경 변수 (`.env`)
프로젝트 루트에 `.env` 파일이 필요합니다. (`.gitignore`에 포함되어 git에 올라가지 않음)
**현재 시크릿 값은 별도 안전한 채널로 전달받으세요.** 필요한 키:

```
# DB
DB_USERNAME=...
DB_PASSWORD=...
SPRING_DATASOURCE_URL=...

# JWT
JWT_SECRET=...                 # HMAC 키 (32바이트 이상)

# OAuth - 제공자별 client id/secret/redirect uri
GOOGLE_CLIENT_ID=...   GOOGLE_CLIENT_SECRET=...   GOOGLE_REDIRECT_URI=...
KAKAO_CLIENT_ID=...    KAKAO_CLIENT_SECRET=...    KAKAO_REDIRECT_URI=...
NAVER_CLIENT_ID=...    NAVER_CLIENT_SECRET=...    NAVER_REDIRECT_URI=...
```

### 7-3. 실행 방법

**A. 로컬 DB까지 Docker로 한 번에** (`docker-compose.yml`)
```bash
docker compose up --build
# postgres(5432) + app(8080) 기동, JPA가 스키마 자동 생성
```

**B. 앱만 IDE/Gradle로 실행** (DB는 별도)
```bash
./gradlew bootRun        # 기본 포트 3000 (PORT 환경변수로 변경 가능)
```

- 운영 RDS는 **`application.yaml`에 호스트가 하드코딩**되어 있고 프라이빗망에 있습니다. 로컬에서 운영 DB로 붙으려면 EC2 SSH 터널이 필요합니다(아래 9번).
- 로컬 전용 설정은 `application-local.yaml`(gitignore 대상)에 둡니다.

---

## 8. 배포 (AWS)

빌드는 **로컬에서 수행**하고 EC2에는 **jar만 올려 실행**하는 구조입니다(t3.micro 메모리 절약).

### 구성
```
인터넷 → (도메인: sukiverse.duckdns.org) → EC2(t3.micro)
            ├─ nginx :80  ── 정적 Swagger UI + /api, /swagger-ui, /v3/api-docs, /login/oauth2 프록시
            └─ Docker: sukiverse-app :8080  ──→  AWS RDS(PostgreSQL, 프라이빗)
```

- **`Dockerfile.prod`**: `eclipse-temurin:17-jre`에 `app.jar`만 COPY 후 실행
- **`docker-compose.prod.yml`**: `.env`(`env_file`)로 환경변수 주입, 앱만 기동
- **`nginx-sukiverse.conf`**: 80번 포트에서 정적 Swagger UI 서빙 + 백엔드 경로 프록시
- **`global-bundle.pem`**: AWS RDS SSL 접속용 인증서 번들

### 대략적 배포 순서
```bash
# 1) 로컬에서 빌드
./gradlew clean bootJar
# 2) build/libs/*.jar 를 app.jar 로 EC2에 전송 (scp)
# 3) EC2에서
docker compose -f docker-compose.prod.yml up -d --build
```

> 정확한 배포 스크립트/명령은 인계 시 구두 또는 별도 메모로 보완 필요. (CI/CD 미구성, 수동 배포)

---

## 9. 운영 인프라 접속 정보

| 항목 | 내용 |
|------|------|
| 도메인 | `sukiverse.duckdns.org` (DuckDNS 무료 동적 DNS) |
| 서버 | AWS EC2 `t3.micro`, 접속: `ssh ubuntu@sukiverse.duckdns.org` |
| DB | AWS RDS PostgreSQL (`sukiversedb...ap-northeast-2.rds.amazonaws.com:5432`, DB명 `sukiverseDB`) |
| DB 접근 | **RDS는 프라이빗.** EC2를 경유한 SSH 터널로만 접속 가능 |

SSH 키, AWS 콘솔 계정, DuckDNS 토큰은 **별도 채널로 전달** 필요(아래 체크리스트 참고).

---

## 10. ⚠️ 보안 주의사항 (인계 전 반드시 처리)

1. **모든 시크릿 교체(rotation) 권장** — 인계 시점에 다음을 새로 발급/변경:
   - `JWT_SECRET` (변경 시 기존 발급 토큰 전부 무효화됨)
   - Google / Kakao / Naver OAuth client secret
   - RDS DB 비밀번호
2. **`.env` 파일은 절대 git/메신저 평문 공유 금지** — 비밀번호 관리도구나 암호화 채널 사용.
3. `anime` API가 전부 `permitAll` — 쓰기 작업(POST/PATCH/DELETE/sync)에 관리자 인증을 붙일지 결정 필요.
4. CORS가 `allowedOriginPatterns = ["*"]` + `allowCredentials = true` 조합 — 운영에서는 허용 오리진을 명시적으로 좁히는 것을 권장.

---

## 11. 알려진 이슈 / TODO

- **Jikan 동기화 매칭 한계**: 재구성 이전부터 있던 시즌은 `mal_id`가 비어 있어 `syncTop100()`에서 매칭되지 않고 신규 시리즈로 중복 생성될 수 있음. → 1회성 `mal_id` 백필 필요 (`AnimeService.kt` 주석 참고).
- **시리즈 그룹핑(병합) 규칙 미구현**: 같은 시리즈의 여러 시즌을 자동 묶는 매핑 규칙이 아직 없음(현재는 제목 일치 기반).
- **CI/CD 없음**: 수동 빌드·배포.
- **테스트 거의 없음**: `SukiverseApplicationTests`(컨텍스트 로드)만 존재.
- DB 백업 SQL이 루트에 존재(`anime_backup_*.sql`) — 운영 데이터 참고용.

---

## 12. 인수인계 체크리스트

차기 담당자에게 **계정/권한 이양**이 필요한 항목:

- [ ] AWS 콘솔 계정 (EC2 / RDS 관리 권한)
- [ ] EC2 SSH 키 페어 (`.pem`)
- [ ] DuckDNS 계정 및 도메인 토큰 (`sukiverse.duckdns.org`)
- [ ] Google Cloud Console — OAuth 클라이언트 (앱 소유권/리디렉션 URI)
- [ ] Kakao Developers — 앱 관리자 권한
- [ ] Naver Developers — 앱 관리자 권한
- [ ] `.env` 운영 시크릿 일체 (전달 후 rotation 권장)
- [ ] RDS 접속 정보 및 SSH 터널 구성 방법
- [ ] GitHub 리포지토리 권한 이양
- [ ] (선택) 배포 스크립트/수동 배포 절차 시연

---

## 13. 참고 명령어 모음

```bash
# 로컬 전체 기동 (DB 포함)
docker compose up --build

# 앱만 실행
./gradlew bootRun

# 운영 jar 빌드
./gradlew clean bootJar

# EC2 접속
ssh ubuntu@sukiverse.duckdns.org

# 운영 컨테이너 상태/로그
docker ps
docker logs -f sukiverse-app
```
