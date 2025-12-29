<div align="center">

# 🎓 SUH Sejong Univ Auth

**세종대학교 포털 인증, 단 3줄이면 끝**

<!-- 수정하지마세요 자동으로 동기화 됩니다 -->
## 최신 버전 : v1.0.0 (2025-12-29)

[![Nexus](https://img.shields.io/badge/Nexus-버전_목록-4E9BCD?style=flat-square&logo=sonatype&logoColor=white)](https://nexus.suhsaechan.kr/#browse/browse:maven-releases:kr%2Fsuhsaechan%2Fsejong-univ-auth)
[![Java](https://img.shields.io/badge/Java-17+-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)

[빠른 시작](#-빠른-시작) • [주요 기능](#-주요-기능) • [API 문서](#-api-레퍼런스) • [설정](#%EF%B8%8F-설정)

</div>

---

## ✨ 왜 SUH-SEJONG-UNIV-AUTH 라이브러리인가?

세종대학교 포털 인증을 직접 구현하려면 **SSO 처리, 쿠키 관리, HTML 파싱**까지 복잡한 작업이 필요합니다.
이 라이브러리는 모든 복잡함을 숨기고, **단 3줄의 코드**로 학생 인증을 완료합니다.

| 기존 방식 | 이 라이브러리 |
|----------|--------------|
| 직접 HTTP 요청 구현 | ✅ 자동화된 SSO 처리 |
| 쿠키/세션 수동 관리 | ✅ 자동 세션 관리 |
| HTML 파싱 직접 구현 | ✅ 구조화된 데이터 반환 |
| 에러 처리 복잡 | ✅ 명확한 에러 코드 |
| 로그인 실패 감지 어려움 | ✅ 즉시 실패 감지 |

```java
// 3줄이면 끝!
SejongAuthResult result = authService.authenticate("학번", "비밀번호");
String name = result.getStudentInfo().getName();       // "홍길동"
String major = result.getStudentInfo().getMajor();     // "컴퓨터공학과"
```

---

## 🚀 빠른 시작

### 1. 의존성 추가

**Gradle**
```groovy
dependencies {
    implementation 'kr.suhsaechan:sejong-univ-auth:X.X.X' # 최신 버전으로 수정
}
```

**Maven**
```xml
<dependency>
    <groupId>kr.suhsaechan</groupId>
    <artifactId>sejong-univ-auth</artifactId>
    <version>X.X.X</version> <!-- 최신 버전으로 수정 -->
</dependency>
```

### 2. 바로 사용

```java
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final SejongAuthService sejongAuthService;

    @PostMapping("/auth/sejong")
    public SejongStudentInfo authenticate(@RequestParam String studentId,
                                          @RequestParam String password) {
        return sejongAuthService.authenticateBasic(studentId, password);
    }
}
```

**끝!** Spring Boot Auto Configuration으로 별도 설정 없이 바로 사용 가능합니다.

---

## 🎯 주요 기능

### 🔐 SSO 인증
- 세종대학교 포털 **자동 로그인**
- 복잡한 SSO 리다이렉트 자동 처리
- **로그인 실패 즉시 감지** (`AUTHENTICATION_FAILED`)

### 👤 학생 정보 조회
```java
SejongStudentInfo info = authService.authenticateBasic("학번", "비밀번호");

info.getMajor();     // "바이오융합공학전공"
info.getStudentId(); // "18010561"
info.getName();      // "홍길동"
info.getGrade();     // "4"
info.getStatus();    // "재학" / "휴학" / "졸업"
```

### 📚 고전독서 인증 조회
```java
SejongAuthResult result = authService.authenticate("학번", "비밀번호");
SejongClassicReading reading = result.getClassicReading();

reading.getCertifications();  // 영역별 인증 도서 목록
reading.getExamRecords();     // 시험 응시 이력
reading.getContestRecords();  // 공모전 참가 이력
```

### ⚡ Spring Boot 자동 설정
- `@Autowired` 또는 생성자 주입으로 **바로 사용**
- `application.yml`로 세부 설정 커스터마이징
- **Zero Configuration** - 기본값으로 바로 동작

---

## 📖 API 레퍼런스

### SejongAuthService

| 메서드 | 설명 | 반환 타입 |
|--------|------|----------|
| `authenticate(studentId, password)` | 전체 정보 조회 (학생정보 + 고전독서) | `SejongAuthResult` |
| `authenticateBasic(studentId, password)` | 기본 정보만 조회 | `SejongStudentInfo` |
| `authenticateWithRawHtml(studentId, password)` | 원본 HTML 포함 (디버깅용) | `SejongAuthResult` |

### 반환 객체

**SejongStudentInfo**
| 필드 | 타입 | 설명 |
|------|------|------|
| `major` | String | 학과명 |
| `studentId` | String | 학번 |
| `name` | String | 이름 |
| `grade` | String | 학년 |
| `status` | String | 상태 (재학/휴학/졸업) |

**SejongAuthResult**
| 필드 | 타입 | 설명 |
|------|------|------|
| `success` | boolean | 인증 성공 여부 |
| `studentInfo` | SejongStudentInfo | 학생 기본 정보 |
| `classicReading` | SejongClassicReading | 고전독서 인증 정보 |
| `authenticatedAt` | LocalDateTime | 인증 시간 |

### 예외 처리

```java
try {
    SejongAuthResult result = authService.authenticate(studentId, password);
} catch (SejongAuthException e) {
    switch (e.getErrorCode()) {
        case AUTHENTICATION_FAILED -> // 학번/비밀번호 불일치
        case INVALID_INPUT -> // 입력값 오류 (빈 값 등)
        case CONNECTION_FAILED -> // 포털 연결 실패
        case CONNECTION_TIMEOUT -> // 연결 시간 초과
        case PARSE_ERROR -> // HTML 파싱 실패
    }
}
```

---

## ⚙️ 설정

`application.yml`에서 세부 설정이 가능합니다:

```yaml
sejong:
  auth:
    ssl-verification: false    # SSL 검증 (기본: true)
    timeout-seconds: 10        # 타임아웃 초 (기본: 10)
    max-retry: 3               # 최대 재시도 횟수 (기본: 3)
```

> **💡 Tip**: 세종대학교 포털 SSL 인증서 문제로 `ssl-verification: false` 권장

---

## 📋 에러 코드

| 코드 | 설명 |
|------|------|
| `AUTHENTICATION_FAILED` | 학번 또는 비밀번호가 일치하지 않음 |
| `INVALID_INPUT` | 입력값이 비어있거나 유효하지 않음 |
| `CONNECTION_FAILED` | 세종대학교 포털 연결 실패 |
| `CONNECTION_TIMEOUT` | 연결 시간 초과 |
| `DATA_FETCH_FAILED` | 학생 정보 조회 실패 |
| `PARSE_ERROR` | HTML 파싱 오류 |
| `SESSION_ERROR` | 세션 처리 오류 |

---

## 🔧 요구사항

- **Java 17+**
- **Spring Boot 3.x**

---

## 📄 라이선스

MIT License - 자유롭게 사용하세요!

---

<div align="center">

**⭐ 이 프로젝트가 도움이 되었다면 Star를 눌러주세요!**

Made with ❤️ by [SUH-LAB](https://github.com/Cassiiopeia)

</div>
