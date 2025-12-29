# SUH-sejong-univ-auth

<!-- 수정하지마세요 자동으로 동기화 됩니다 -->
## 최신 버전 : v1.0.0 (2025-12-29)

[전체 버전 기록 보기](CHANGELOG.md)

</br>

<!-- 템플릿 초기화 완료: 2025-12-29 18:34:53 KST -->

---

세종대학교 포털 인증을 위한 Spring Boot 라이브러리입니다.  
학생 정보 조회 및 고전독서 인증 정보를 제공합니다.

## 설치

### Gradle
```gradle
dependencies {
    implementation 'kr.suhsaechan:sejong-univ-auth:x.x.x' # 최신 버전 
}
```

### Maven
```xml
<dependency>
    <groupId>kr.suhsaechan</groupId>
    <artifactId>sejong-univ-auth</artifactId>
    <version>x.x.x</version>  <!-- 최신 버전  -->
</dependency>
```

## 사용법

### 기본 사용

```java
@RestController
public class AuthController {
    
    @Autowired
    private SejongAuthService sejongAuthService;
    
    @PostMapping("/auth")
    public SejongAuthResult authenticate(@RequestParam String studentId,
                                         @RequestParam String password) {
        return sejongAuthService.authenticate(studentId, password);
    }
}
```

### 전체 정보 조회

```java
// 학생 정보 + 고전독서 정보
SejongAuthResult result = sejongAuthService.authenticate("학번", "비밀번호");

SejongStudentInfo studentInfo = result.getStudentInfo();
SejongClassicReading classicReading = result.getClassicReading();
```

### 기본 정보만 조회

```java
// 학생 기본 정보만
SejongStudentInfo studentInfo = sejongAuthService.authenticateBasic("학번", "비밀번호");
```

## API

### SejongAuthService

#### `authenticate(String studentId, String password)`
전체 정보 조회 (학생 정보 + 고전독서 정보)

**반환**: `SejongAuthResult`
- `studentInfo`: 학생 기본 정보
- `classicReading`: 고전독서 인증 정보
- `authenticatedAt`: 인증 시간

#### `authenticateBasic(String studentId, String password)`
기본 정보만 조회

**반환**: `SejongStudentInfo`
- `major`: 학과명
- `studentId`: 학번
- `name`: 이름
- `grade`: 학년
- `status`: 상태 (재학/휴학/졸업 등)

#### `authenticateWithRawHtml(String studentId, String password)`
원본 HTML 포함 조회 (디버깅용)

**반환**: `SejongAuthResult` (rawHtml 필드 포함)

### 예외 처리

```java
try {
    SejongAuthResult result = sejongAuthService.authenticate(studentId, password);
} catch (SejongAuthException e) {
    SejongAuthErrorCode errorCode = e.getErrorCode();
    // AUTHENTICATION_FAILED: 인증 실패
    // INVALID_INPUT: 입력값 오류
    // CONNECTION_FAILED: 연결 실패
    // 등등...
}
```

## 설정

`application.yml`에서 설정 가능합니다:

```yaml
sejong:
  auth:
    ssl-verification: false    # SSL 검증 비활성화 (기본: true)
    timeout-seconds: 10        # 타임아웃 (초, 기본: 10)
    max-retry: 3               # 최대 재시도 횟수 (기본: 3)
```

## 상세 문서

더 자세한 내용은 [docs](./docs) 폴더를 참고하세요.
