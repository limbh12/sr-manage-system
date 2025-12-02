# SR Management System 운영 가이드

이 문서는 SR 관리 시스템의 설치, 설정, 운영 및 유지보수를 위한 가이드입니다.

## 1. 백엔드 설정 (Backend)

백엔드 설정은 주로 `backend/src/main/resources/application.yml` 파일에서 관리됩니다.

### 1.1 데이터베이스 프로필 설정

애플리케이션 실행 시 활성화할 프로필을 선택하여 데이터베이스를 변경할 수 있습니다.

- **기본값 (H2)**: 별도 프로필 지정 없이 실행 시 인메모리 DB 사용 (개발용)
- **CUBRID**: `-Dspring.profiles.active=cubrid`
- **MySQL**: `-Dspring.profiles.active=mysql`
- **PostgreSQL**: `-Dspring.profiles.active=postgresql`

**실행 예시:**
```bash
# CUBRID 프로필로 실행
java -jar -Dspring.profiles.active=cubrid target/sr-management-0.0.1-SNAPSHOT.jar
```

### 1.2 데이터베이스 연결 정보 수정

각 프로필별 `datasource` 설정을 환경에 맞게 수정해야 합니다.

```yaml
# CUBRID 예시
spring:
  config:
    activate:
      on-profile: cubrid
  datasource:
    url: "jdbc:cubrid:localhost:33000:sr_db:::"  # IP, Port, DB명 수정
    username: soa                                 # DB 사용자명
    password: soa                                 # DB 비밀번호
```

### 1.3 데이터베이스 초기화 설정 (중요)

서버 시작 시 데이터베이스 테이블 및 초기 데이터 생성 동작을 제어합니다.

```yaml
spring:
  sql:
    init:
      mode: never  # always: 시작 시 항상 초기화 (데이터 삭제됨), never: 초기화 안 함 (데이터 유지)
```
- **운영 환경 권장**: `never` (데이터 보존)
- **초기 구축/테스트**: `always` (스키마 및 데이터 리셋)
- **주의**: `always`로 설정 시 `schema.sql`의 `DROP TABLE` 명령이 실행되어 기존 데이터가 모두 삭제됩니다.

### 1.4 JWT 보안 설정

보안을 위해 운영 환경에서는 반드시 비밀키(`secret`)를 변경해야 합니다.

```yaml
jwt:
  secret: ${JWT_SECRET}  # 환경 변수로 주입받거나, 복잡한 문자열로 변경
  access-token-validity: 1800000   # 30분 (밀리초)
  refresh-token-validity: 604800000 # 7일 (밀리초)
```

### 1.5 로깅 레벨 설정

문제 해결을 위해 로그 레벨을 조정할 수 있습니다.

```yaml
logging:
  level:
    com.srmanagement: INFO        # 운영 시 INFO, 개발 시 DEBUG 권장
    org.springframework.security: WARN
    org.hibernate.SQL: OFF        # 쿼리 로그 (성능 영향 있음)
```

---

## 2. 프론트엔드 설정 (Frontend)

프론트엔드 설정은 `frontend/vite.config.ts` 및 `frontend/src/services/api.ts` 등에서 관리됩니다.

### 2.1 API 프록시 설정 (개발 환경)

개발 서버의 API 프록시 대상(백엔드 서버 주소)은 `frontend/.env` 파일에서 설정할 수 있습니다.

```properties
# .env 파일
VITE_API_URL=http://localhost:8080
```

`vite.config.ts`는 이 환경 변수를 로드하여 프록시 설정을 자동으로 구성합니다.

```typescript
// vite.config.ts (참고용)
proxy: {
  '/api': {
    target: env.VITE_API_URL || 'http://localhost:8080',
    changeOrigin: true,
  },
},
```

### 2.2 빌드 및 배포

운영 환경 배포를 위해 정적 파일로 빌드합니다.

```bash
cd frontend
npm run build
```
빌드 결과물은 `frontend/dist` 디렉토리에 생성되며, 이를 웹 서버(Nginx, Apache 등)에 배포합니다.

### 2.3 Mock 데이터 모드 설정

백엔드 서버 없이 프론트엔드만 단독으로 개발하거나 테스트할 때 Mock(가짜) 데이터를 사용할 수 있습니다.

1. `frontend` 디렉토리에 `.env` 파일을 생성하거나 수정합니다.
2. `VITE_USE_MOCK` 환경 변수를 설정합니다.

```properties
# .env 파일
VITE_USE_MOCK=true  # Mock 모드 활성화
# VITE_USE_MOCK=false # 실제 백엔드 API 사용 (기본값)
```

- **true**: `src/services/mock` 디렉토리의 Mock 데이터를 사용합니다.
- **false (또는 설정 없음)**: 실제 백엔드 API(`http://localhost:8080`)를 호출합니다.

---

## 3. 운영 체크리스트

### 배포 전 확인사항
1. [ ] `application.yml`의 `spring.sql.init.mode`가 `never`로 설정되어 있는가?
2. [ ] `jwt.secret`이 안전한 값으로 변경되었는가?
3. [ ] 데이터베이스 연결 정보(URL, Username, Password)가 운영 환경에 맞게 설정되었는가?
4. [ ] 불필요한 디버그 로그(`logging.level`)가 비활성화되었는가?

### 문제 해결 (Troubleshooting)

**Q. 서버 재시작 후 데이터가 모두 사라졌습니다.**
A. `application.yml`의 `spring.sql.init.mode`가 `always`로 설정되어 있는지 확인하세요. `never`로 변경하면 데이터가 유지됩니다.

**Q. 로그인 시 500 에러가 발생합니다.**
A. 백엔드 로그를 확인하세요. DB 연결 오류이거나, `CubridDialect` 관련 호환성 문제일 수 있습니다. CUBRID 사용 시 `schema.sql`을 통해 테이블이 올바르게 생성되었는지 확인하세요.

**Q. CORS 오류가 발생합니다.**
A. 프론트엔드와 백엔드가 다른 도메인/포트에서 실행 중인 경우 발생할 수 있습니다. `backend/src/main/java/com/srmanagement/config/CorsConfig.java`에서 허용할 Origin을 추가하세요.
