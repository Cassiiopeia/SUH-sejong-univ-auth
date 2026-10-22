# pro-sejong-auth 다국어(Java·Python·Node) 모노레포 설계 (2026-10-22)

## 의도
세종대 학생 개발자들은 각자 다른 언어로 서비스를 만든다. 지금은 Java/Spring만 지원해서 Python·Node 개발자는 포털 로그인(SSO, 쿠키, HTML 파싱, 레거시 TLS)을 직접 구현해야 한다.
목표는 **어떤 언어든 공식 패키지 저장소에서 한 줄로 설치해서 바로 세종대 로그인을 붙이는 것**이다.

- Java/Spring: Maven Central `kr.suhsaechan:sejong-univ-auth`
- Python: PyPI `pip install pro-sejong-auth`
- Node: npm `npm install pro-sejong-auth`

## 결정된 것 (사용자)
- 한 레포(모노레포) `Cassiiopeia/pro-sejong-auth`에서 세 언어 모두 지원
- 방식: **언어별 네이티브 라이브러리** (각 언어 코드가 사용자 서버에서 직접 포털에 로그인). 인증 서버를 거치지 않음
- 설치가 쉬워야 함 (pip, npm처럼)
- 배포는 언어별 공식 저장소(Maven Central, PyPI, npm). 지금의 개인 Nexus 대신
- Maven groupId는 `kr.suhsaechan` 유지
- 테스트는 실제 포털 인증까지 세 언어 모두 확인

## 목표
1. 세 언어가 같은 기능·같은 결과 필드·같은 에러 코드를 제공
2. 세 언어 모두 공식 저장소에서 설치 가능, 태그(릴리스) 한 번으로 자동 배포
3. 포털이 바뀌었을 때 한 이슈·한 PR로 세 언어를 같이 고칠 수 있는 구조
4. 문서(README, Pages)에서 언어별 빠른 시작을 탭으로 제공

## 하지 않는 것 (non-goals)
- 인증 서버(호스팅 API)나 Docker 이미지 제공 (데모용 lab 서버는 지금처럼 유지)
- Go, Kotlin 전용, PHP 등 다른 언어 (구조는 나중에 추가 가능하게만)
- Python async API, Node 브라우저 지원 (서버 전용)
- 성적·학적 등 새 데이터 조회 기능

## 실패로 보는 것 (anti-goals)
- 기존 Java 사용자(lab 서버 포함)의 의존성 좌표나 공개 API가 깨짐
- 비밀번호나 개인정보가 로그·예외·결과 외 경로로 남음
- 학생이 쓰기에 설치가 번거로움 (무거운 의존성, 추가 프로그램 설치)
- 세 언어의 결과가 서로 다름 (같은 계정인데 필드 값·에러 코드가 다름)

## 확인된 사실 (2026-10-22 실측)
- 포털 TLS: TLSv1.2 + `AES256-SHA`(TLS_RSA_WITH_AES_256_CBC_SHA)만 제공
- Python 3.14 + OpenSSL 3.6: 표준 라이브러리 `ssl` 컨텍스트에 `DEFAULT:AES256-SHA:@SECLEVEL=1` 설정 시 로그인 > SSO > 고전독서 페이지 200
- Node 26 + OpenSSL 3.6: `https.Agent({ciphers: 'DEFAULT:AES256-SHA:@SECLEVEL=0'})` + **User-Agent 헤더 필수**(없으면 SSO 이후 404)로 같은 흐름 200
- Maven Central에 `kr.suhsaechan` 네임스페이스는 이미 확인됨(suh-logger 배포 중). Central 업로드 토큰·GPG 서명키가 projectops cred(`sonatype-central-token`, `gpg-suh-logger`)에 있음
- npm 계정 cred(`npm-suhsaechan`) 있음. PyPI 계정은 없음
- 이름 `pro-sejong-auth`: PyPI, npm 모두 미사용

## 레포 구조
```
pro-sejong-auth/
├── java/                 기존 Spring Boot 라이브러리 (현재 루트 코드 이동)
├── python/               pip 패키지 (src 레이아웃)
├── node/                 npm 패키지 (TypeScript 소스 > ESM+CJS, 타입 포함)
├── spec/                 언어 공통 계약
│   ├── PORTAL_FLOW.md    포털 요청 순서·헤더·쿠키·판정 규칙 (단일 원본)
│   ├── RESULT_SCHEMA.md  결과 필드·에러 코드 표 (단일 원본)
│   └── fixtures/         포털 응답 샘플(개인정보 제거): DHC HTML, SIS JSON, 실패 응답
├── docs/                 기존 문서·자산
├── README.md / README.ko.md
└── .github/workflows/    언어별 CI·배포 (경로 필터)
```

## 공개 API (세 언어 동일한 의미)
| 기능 | Java (기존 유지) | Python | Node |
|---|---|---|---|
| 통합 | `authenticate(id, pw)` | `authenticate(id, pw)` | `authenticate(id, pw)` |
| DHC | `authenticateWithDHC` | `authenticate_dhc` | `authenticateDhc` |
| SIS | `authenticateWithSIS` | `authenticate_sis` | `authenticateSis` |
| 원본 포함 | `...Raw` 메서드 | `include_raw=True` 인자 | `{ includeRaw: true }` 옵션 |

- 결과 필드: Java와 같은 이름(Python은 snake_case, Node는 camelCase). 통합 결과의 연락처는 SIS 실패 시 `None`/`null`, SIS 결과의 학년·상태는 빈 문자열 (Java 현재 동작 기준)
- 에러: 언어별 예외 클래스(`SejongAuthError`) + Java와 같은 에러 코드 문자열(`AUTHENTICATION_FAILED` 등)
- 설정: 타임아웃(초), 재시도 횟수, 인증서 검증 여부. 기본값은 Java와 같음(10초, 3회, 검증 켬)
- Java의 정확한 동작이 기준이다. Python·Node는 `spec/`과 Java 구현을 보고 맞춘다

## 의존성 원칙
- Python: 표준 라이브러리만 사용 (설치 시 추가 패키지 없음), Python 3.9+
- Node: 런타임 의존성 0개 목표, Node 18+. HTML 표 파싱이 표준 기능만으로 지나치게 위험하면 작은 의존성 1개까지 허용(빌더 판단, 이유 기록)
- Java: 기존 유지

## 배포
- 공통: 세 언어가 **같은 버전 번호**를 쓴다(예: 1.3.0). GitHub Release(태그 `vX.Y.Z`)가 발행되면 세 저장소로 배포
- Java: Maven Central (기존 suh-logger 배포 방식 재사용). 기존 Nexus 배포도 전환기 동안 함께 유지 (lab 서버 등 기존 사용자 보호)
- Python: PyPI Trusted Publishing(GitHub OIDC, 토큰 저장 없음). 사용자가 PyPI 계정 생성·publisher 등록 1회 필요
- Node: npm, `NPM_TOKEN` GitHub Secret (provenance 포함)
- 기존 projectops 버전·배포 파이프라인(version.yml, deploy 브랜치, changelog)이 계속 동작해야 한다. Java 이동으로 바뀌는 경로는 그에 맞춰 수정

## 테스트
- 언어별 단위 테스트: `spec/fixtures`의 같은 샘플로 파서·흐름 판정 검증
- 실제 포털 테스트: 세 언어 모두 환경변수(`SEJONG_TEST_STUDENT_ID`, `SEJONG_TEST_PASSWORD`)가 있을 때만 실행, 없으면 건너뜀. CI에는 계정을 넣지 않음
- 같은 계정으로 세 언어 결과가 같은지 비교하는 교차 검증을 로컬에서 수행
- 배포 후 실제 저장소에서 설치(`pip install`, `npm install`, Gradle/Maven 의존성)해 인증까지 확인

## 문서
- README(영/한): 언어별 설치·사용 예시를 나란히
- Pages: 빠른 시작을 Java / Python / Node 탭으로, 검색어(세종대 로그인 API Python/Node 등) 반영
- 각 언어 폴더에 짧은 README (패키지 저장소 페이지에 표시됨)

## 설계자 판단 (리뷰 질문에 대한 결정, 사용자 확인 전 제안값)
1. **배포 트리거**: 지금 흐름 그대로 `deploy` 브랜치 push(= deploy PR 머지)에서 세 저장소로 배포. main push마다 붙는 자동 버전 태그로는 배포하지 않음(공개 저장소 버전은 지울 수 없음). 배포 후 GitHub Release 생성
2. **버전 동기화**: version.yml의 `project_types: ["spring", "python", "node"]`, `project_paths: {spring: "java", python: "python", node: "node"}`로 projectops VERSION-CONTROL이 세 파일을 같이 올리게 한다. 템플릿 공용 워크플로우(PROJECT-COMMON-*)는 고치지 않고, 언어별 배포는 이 레포 전용 워크플로우 파일(`PROJECT-SEJONG-*`)로 추가. 공용 워크플로우가 다국어를 못 맞추면 그때만 전용 워크플로우에서 version.yml을 읽어 맞춘다
3. **Java Central 배포**: suh-logger 방식 재사용. `com.vanniktech.maven.publish`로 Central(GPG 서명) + Nexus 동시 배포, 자격은 기존 `GRADLE_PROPERTIES` Secret에 `mavenCentralUsername`·`mavenCentralPassword`·`signingInMemoryKey`·`signingInMemoryKeyPassword` 추가(값이 없으면 Nexus만). GitHub Packages 배포는 유지
4. **Python·Node API 모양**
   - Python: 설치 이름 `pro-sejong-auth`, import 이름 `sejong_auth` (설치명과 import명이 다른 흔한 방식). 사용: `from sejong_auth import authenticate` 한 줄 함수 + 설정이 필요하면 `SejongAuthClient(timeout_seconds=10, max_retry=3, ssl_verification=True)`
   - Node: `import { authenticate, SejongAuthClient } from 'pro-sejong-auth'`. 함수는 Promise 반환. 설정은 `new SejongAuthClient({ timeoutSeconds, maxRetry, sslVerification })`
   - 한 줄 함수는 기본 설정 클라이언트를 내부에서 씀
5. **원본(raw) 옵션**: Java와 같게 DHC·SIS에만 있음. 통합 인증에는 없음
6. **부분 실패**: 세 배포는 서로 다른 작업(job)으로 분리해 실패한 것만 다시 실행. 버전 번호는 항상 세 언어가 같다(실패한 것을 같은 버전으로 재시도, 번호를 건너뛰지 않음)

## 빌더에게 맡기는 것
- 언어별 내부 구조, 파일 나누기, 테스트 프레임워크 선택(pytest, node:test 등)
- fixtures 개인정보 제거 방식
- CI 워크플로우 세부 구성
