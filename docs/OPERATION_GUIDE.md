# SR Management System 운영 가이드

이 문서는 SR 관리 시스템의 설치, 설정, 운영 및 유지보수를 위한 가이드입니다.

## 1. 백엔드 설정 (Backend)

백엔드 설정은 주로 `backend/src/main/resources/application.yml` 파일에서 관리됩니다.

### 1.1 데이터베이스 프로필 설정

애플리케이션 실행 시 활성화할 프로필을 선택하여 데이터베이스를 변경할 수 있습니다.

- **기본값 (H2)**: 별도 프로필 지정 없이 실행 시 **파일 기반** H2 DB 사용 (데이터 유지됨, `backend/data/srdb`)
- **운영 환경 (H2)**: `-Dspring.profiles.active=prod` (운영 최적화 설정, `./data/srdb_prod`에 저장)
- **CUBRID**: `-Dspring.profiles.active=cubrid`
- **MySQL**: `-Dspring.profiles.active=mysql`
- **PostgreSQL**: `-Dspring.profiles.active=postgresql`

**실행 예시:**
```bash
# 운영 환경 프로필로 실행 (H2 임베디드 모드)
java -jar -Dspring.profiles.active=prod target/sr-management-0.0.1-SNAPSHOT.jar
```

### 1.2 데이터베이스 연결 정보 수정

각 프로필별 `datasource` 설정을 환경에 맞게 수정해야 합니다.

```yaml
# 운영 환경 (prod) 설정 (application.yml)
spring:
  datasource:
    # 실행 위치의 data/srdb_prod 파일에 데이터 저장
    url: jdbc:h2:file:./data/srdb_prod;AUTO_SERVER=TRUE
    username: sa
    password: ${DB_PASSWORD:sa1234!} # 환경변수 DB_PASSWORD로 변경 가능
```

```yaml
# H2 (File Mode) 기본 개발 환경 예시
spring:
  datasource:
    # 주의: 절대 경로를 사용하는 것을 권장합니다.
    url: jdbc:h2:file:/Users/username/project/backend/data/srdb;AUTO_SERVER=TRUE
    username: sa
    password: sa1234!
  h2:
    console:
      enabled: true
      path: /h2-console # 접속: http://localhost:8080/h2-console
```

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

**JPA DDL 설정 (`application.yml`)**
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create  # create: 시작 시 테이블 삭제 후 재생성 (데이터 초기화)
                        # update: 변경된 스키마만 반영 (데이터 유지)
                        # validate: 엔티티와 테이블 스키마 일치 여부만 확인
                        # none: 아무 작업도 안 함
```

**SQL 초기화 설정**
```yaml
spring:
  sql:
    init:
      mode: never  # always: 시작 시 항상 초기화 (데이터 삭제됨), never: 초기화 안 함 (데이터 유지)
```
- **운영 환경 권장**: `ddl-auto: validate` 또는 `none`, `init.mode: never`
- **개발/테스트 (데이터 리셋)**: `ddl-auto: create`
- **개발 (데이터 유지)**: `ddl-auto: update`

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

### 1.6 빌드 및 실행 주의사항

Maven 빌드 시 반드시 `backend` 디렉토리 내부에서 명령어를 실행해야 합니다.

```bash
# 올바른 예시
cd backend
mvn clean package

# 잘못된 예시 (루트 디렉토리에서 실행 시 .m2 폴더가 루트에 생성됨)
mvn -f backend/pom.xml clean package
```

프로젝트 설정(`backend/.mvn/maven.config`)에 의해 로컬 저장소 경로가 상대 경로(`-Dmaven.repo.local=./.m2/repository`)로 지정되어 있어, 실행 위치에 따라 `.m2` 폴더가 생성되는 위치가 달라질 수 있습니다.

### 1.7 백엔드 실행 및 종료 스크립트

백엔드 서버의 간편한 실행과 종료를 위해 스크립트를 제공합니다.

**실행 스크립트 (`start.sh`)**
- 위치: `backend/scripts/start.sh`
- 기능:
  - `backend/logs` 디렉토리 자동 생성
  - JAR 파일이 없으면 자동 빌드 (`mvn clean package`)
  - `nohup`을 사용하여 백그라운드 실행
  - 로그를 `backend/logs/server.log` 파일에 저장
  - 중복 실행 방지

**종료 스크립트 (`stop.sh`)**
- 위치: `backend/scripts/stop.sh`
- 기능:
  - 8080 포트를 사용하는 프로세스 안전 종료 (`kill`)
  - 종료 실패 시 강제 종료 (`kill -9`)

**사용법:**
```bash
# 프로젝트 루트에서 실행
./backend/scripts/start.sh  # 서버 시작
./backend/scripts/stop.sh   # 서버 종료

# 로그 확인
tail -f backend/logs/server.log
```

### 1.8 API 테스트 스크립트

백엔드 API의 정상 동작 여부를 확인하기 위한 자동화 테스트 스크립트를 제공합니다.

- **위치**: `backend/scripts/backend_test_script.sh`
- **기능**:
  - 로그인, 토큰 갱신
  - 사용자 조회, 조직 검색
  - 설문 조사(OpenApiSurvey) 조회
  - SR 생성, 조회, 수정, 상태 변경, 이력 관리, 삭제 등 주요 시나리오 테스트

**사용법:**
```bash
# 백엔드 서버가 실행 중인 상태에서 실행
./backend/scripts/backend_test_script.sh
```

### 1.9 CSV 일괄 등록 운영 가이드

OPEN API 현황조사 CSV 업로드 기능 운영 시 다음 사항을 참고하십시오.

- **지원 날짜 형식**: `yyyy-MM-dd` (기본), `yyyy.MM.dd` (자동 변환 지원)
- **필수 값 처리**: CSV 내 필수 항목이 비어있는 경우, 시스템이 자동으로 다음 기본값을 할당합니다.
  - 텍스트 필드: "미입력"
  - 선택 필드: "NO_RESPONSE" (미회신)
- **데이터 검증**: 각 컬럼의 데이터 길이는 DB 스키마에 정의된 크기를 초과할 수 없으며, 초과 시 업로드가 실패합니다.

### 1.10 데이터베이스 스키마 변경 사항 (2025-12-04)

운영 중인 데이터베이스에 수동으로 반영해야 할 스키마 변경 사항입니다. (`ddl-auto: update` 사용 시 자동 반영됨)

```sql
-- OpenApiSurvey 테이블 연락처 컬럼 길이 확장 (20 -> 30)
ALTER TABLE open_api_survey MODIFY COLUMN contact_phone VARCHAR(30);
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
