# JPA Converter 및 암호화 적용 가이드

## 1. JPA Converter란?

**JPA Converter**(`AttributeConverter`)는 Java 엔티티의 속성(Attribute)과 데이터베이스의 컬럼(Column) 데이터 간의 변환을 자동으로 처리해 주는 JPA(Java Persistence API)의 기능입니다.

애플리케이션 코드와 데이터베이스 사이에서 '번역기' 역할을 수행하여, 비즈니스 로직을 오염시키지 않고 데이터의 저장 형식을 제어할 수 있습니다.

### 주요 메서드
*   **`convertToDatabaseColumn(EntityAttribute)`**: 엔티티의 데이터를 DB에 저장하기 전에 변환합니다. (예: 평문 -> 암호문)
*   **`convertToEntityAttribute(DatabaseColumn)`**: DB에서 조회한 데이터를 엔티티에 매핑하기 전에 변환합니다. (예: 암호문 -> 평문)

---

## 2. 프로젝트 적용 현황

본 프로젝트(`sr-manage-system`)에서는 **개인정보 보호**를 위해 주요 민감 정보(이름, 전화번호, 이메일)를 데이터베이스에 저장할 때 **AES-256 알고리즘**으로 자동 암호화하는 데 JPA Converter를 사용하고 있습니다.

### A. 구성 요소

1.  **`com.srmanagement.util.CryptoUtil`**: 실제 암호화/복호화 로직을 수행하는 유틸리티 클래스입니다.
2.  **`com.srmanagement.converter.EncryptConverter`**: JPA Converter 구현체로, 엔티티 필드와 DB 컬럼 사이를 중개합니다.

### B. 구현 코드

#### 1. EncryptConverter.java
```java
@Converter
public class EncryptConverter implements AttributeConverter<String, String> {
    
    // DB 저장 시: 평문 -> 암호문
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        return CryptoUtil.encrypt(attribute);
    }

    // DB 조회 시: 암호문 -> 평문
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return CryptoUtil.decrypt(dbData);
    }
}
```

#### 2. 엔티티 적용 예시 (User.java)
`@Convert` 어노테이션을 사용하여 암호화가 필요한 필드에 컨버터를 지정합니다.

```java
@Entity
public class User {
    // ...
    
    @Convert(converter = EncryptConverter.class)
    private String name;  // DB에는 암호화되어 저장됨

    @Convert(converter = EncryptConverter.class)
    private String email; // DB에는 암호화되어 저장됨

    @Convert(converter = EncryptConverter.class)
    private String phone; // DB에는 암호화되어 저장됨
    
    // ...
}
```

### C. 적용 대상 필드

| 엔티티 | 필드명 | 설명 |
| :--- | :--- | :--- |
| **User** | `name` | 사용자 이름 |
| | `email` | 사용자 이메일 |
| | `phone` | 사용자 전화번호 |
| **OpenApiSurvey** | `contactName` | 담당자 이름 |
| | `contactEmail` | 담당자 이메일 |
| | `contactPhone` | 담당자 전화번호 |
| **Sr** | `applicantName` | 신청자 이름 |
| | `applicantPhone` | 신청자 전화번호 |

---

## 3. 동작 흐름 (투명한 암호화)

JPA Converter를 사용함으로써 개발자는 비즈니스 로직에서 암호화를 신경 쓸 필요가 없습니다.

1.  **저장 (Save)**:
    *   `user.setName("홍길동")` 호출.
    *   `repository.save(user)` 실행.
    *   **Converter 개입**: "홍길동" -> `AES256(...)` -> `8A3F...` (암호문).
    *   **DB 저장**: `INSERT INTO user (name, ...) VALUES ('8A3F...', ...);`

2.  **조회 (Load)**:
    *   `repository.findById(1L)` 실행.
    *   **DB 조회**: `SELECT name, ... FROM user WHERE id = 1;` -> `8A3F...` 반환.
    *   **Converter 개입**: `8A3F...` -> `Decrypt(...)` -> "홍길동".
    *   `user.getName()` 호출 시 "홍길동" 반환.

## 4. 주의사항

*   **검색 (Search)**: 암호화된 데이터는 `LIKE` 검색(부분 일치)이 불가능하거나 제한적입니다. 본 프로젝트에서는 암호화된 필드에 대해 **정확히 일치(=)**하는 경우에만 검색되도록 로직이 구현되어 있습니다.
*   **이중 암호화**: Service 계층에서 수동으로 암호화한 값을 엔티티에 넣으면, Converter가 다시 암호화를 수행하여 데이터가 손상될 수 있습니다. **Service 계층에서는 항상 평문을 다뤄야 합니다.**
