# pro-sejong-auth 다국어 모노레포 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 같은 세종대 로그인 기능을 Java(Maven Central), Python(PyPI), Node(npm) 패키지로 한 레포에서 제공하고, 세 언어 모두 실제 포털 인증까지 검증해 배포한다.

**Architecture:** 루트에 Gradle 진입점(gradlew, settings.gradle, 버전)을 남기고 Java 라이브러리는 `java/` 하위 프로젝트로 옮겨 projectops 템플릿 워크플로우를 그대로 쓴다. Python·Node는 각 폴더의 네이티브 구현이며, 포털 흐름·결과 스키마·응답 샘플은 `spec/` 한 곳을 단일 원본으로 공유한다.

**Tech Stack:** Java 17 + Spring Boot 3.5 + OkHttp/jsoup(기존), vanniktech maven-publish; Python 3.9+ 표준 라이브러리, hatchling(빌드 전용), pytest; Node 18+ TypeScript, tsc(ESM+CJS), node:test.

**Spec:** `docs/superpowers/specs/2026-10-22-multi-language-monorepo-design.md`

## Global Constraints

- Maven 좌표 불변: `kr.suhsaechan:sejong-univ-auth`, Java 공개 API(메서드·결과 클래스·에러 코드) 불변
- 패키지 이름: PyPI `pro-sejong-auth`(import `sejong_auth`), npm `pro-sejong-auth`
- 세 언어 같은 버전 번호(version.yml 단일 원본)
- Python 런타임 의존성 0개, Python >=3.9. Node 런타임 의존성 0개 목표, Node >=18
- 기본값: timeout 10초, maxRetry 3, sslVerification true
- 에러 코드 문자열은 Java `SejongAuthErrorCode` 이름 그대로
- 포털 TLS: cipher `AES256-SHA` 허용 필요 (Python `DEFAULT:AES256-SHA:@SECLEVEL=1`, Node `DEFAULT:AES256-SHA:@SECLEVEL=0`), Node는 User-Agent 헤더 필수
- 로그·예외에 비밀번호·이름·연락처 금지(학번만)
- 실제 포털 테스트는 환경변수 `SEJONG_TEST_STUDENT_ID`, `SEJONG_TEST_PASSWORD` 있을 때만, CI에는 넣지 않음
- 배포 트리거: deploy 브랜치 push. 공용 템플릿 워크플로우(PROJECT-COMMON-*, PROJECT-SPRING-*)는 수정하지 않음
- 커밋은 `/pro-commit`, 이슈 번호 연결, push 전 `/pro-code-consult`

## Review Focus

1. 틀린 비밀번호 → 세 언어 모두 `AUTHENTICATION_FAILED` (파싱 에러로 새지 않음) — Task 3·4 단위 테스트(401 fixture)
2. 빈 학번·비밀번호 → 네트워크 호출 전에 `INVALID_INPUT` — Task 3·4 테스트
3. 통합 인증에서 SIS만 실패 → 성공 결과 + 연락처 `None`/`null` — Task 3·4 테스트(가짜 전송 계층)
4. 포털 응답 지연 → `CONNECTION_TIMEOUT`, 재시도 후 실패 → `CONNECTION_FAILED` — Task 3·4 테스트
5. 한글 HTML·JSON 디코딩(학과명, 고전독서 영역명)이 깨지지 않음 — Task 2 fixture 기반 파서 테스트

---

### Task 1: Java를 `java/` 하위 프로젝트로 이동 + 버전 단일화

**Files:**
- Move: `src/` → `java/src/`
- Create: `java/build.gradle` (기존 build.gradle 내용 중 플러그인·의존성·publishing)
- Modify: `build.gradle` (루트: `group = 'kr.suhsaechan'`, `version = '<현재값>'`만, `subprojects`에 전파), `settings.gradle` (`rootProject.name = 'pro-sejong-auth'`, `include 'sejong-univ-auth'`, `project(':sejong-univ-auth').projectDir = file('java')`)
- Modify: `version.yml` (`project_types: ["spring", "python", "node"]`, `project_paths: {spring: ".", python: "python", node: "node"}`)
- Modify: `.gitignore` 경로(`java/src/test/resources/application-dev.yml`)

**Interfaces:**
- Produces: `./gradlew :sejong-univ-auth:test`, 루트 `./gradlew publish`가 기존처럼 동작, artifactId `sejong-univ-auth`

- [ ] Step 1: `git mv` 로 이동, 루트/하위 build.gradle 분리
- [ ] Step 2: `./gradlew build` → 모든 기존 테스트 통과, `java/build/libs/sejong-univ-auth-<ver>.jar` 생성, POM artifactId `sejong-univ-auth` 확인(`generatePomFileForMavenPublication`)
- [ ] Step 3: `.github/scripts/version_manager.sh get` → 현재 버전 출력 (python/node 파일 생성 후 Task 3·4에서 sync 재확인)
- [ ] Step 4: 실제 포털 테스트(Disabled 해제 init script) 15/15 통과
- [ ] Step 5: Commit

### Task 2: `spec/` 공통 계약과 응답 샘플

**Files:**
- Create: `spec/PORTAL_FLOW.md` (Java 클라이언트 기준 요청 순서·URL·헤더·쿠키·rtUrl·성공/실패 판정, TLS cipher·User-Agent 요구)
- Create: `spec/RESULT_SCHEMA.md` (통합·DHC·SIS 결과 필드 표: Java/Python/Node 이름, 타입, 빈 값 규칙, 에러 코드 표)
- Create: `spec/fixtures/dhc_status.html` (실제 고전독서 페이지에서 개인정보를 예시값으로 치환: 학과 컴퓨터공학과, 학번 21011234, 이름 홍길동, 학년 3, 상태 재학, 고전독서 표는 구조 유지)
- Create: `spec/fixtures/sis_user_info.json` (initUserInfo 응답, 개인정보 예시값: 이메일 student@example.com, 전화 010-1234-5678, 영문 Hong Gildong)
- Create: `spec/fixtures/expected_dhc.json`, `spec/fixtures/expected_sis.json` (위 샘플 파싱 기대값, 세 언어 테스트가 같은 기대값 사용)
- Create: `java/src/test/java/kr/suhsaechan/sejong/auth/parser/SpecFixtureParserTest.java`

- [ ] Step 1: 실제 응답 수집(로컬, 계정 사용) 후 개인정보 치환. 치환 전 원본은 커밋하지 않음
- [ ] Step 2: Java 테스트 `dhc_fixture_matches_expected` / `sis_fixture_matches_expected`: fixture 파싱 결과가 expected JSON과 필드별 일치 (고전독서 certifications 영역명·건수 포함)
- [ ] Step 3: `./gradlew test` PASS
- [ ] Step 4: Commit

### Task 3: Python 패키지 `pro-sejong-auth`

**Files:**
- Create: `python/pyproject.toml` (name `pro-sejong-auth`, version 루트와 동일, requires-python `>=3.9`, dependencies `[]`, build backend hatchling, packages `src/sejong_auth`, MIT, URLs)
- Create: `python/src/sejong_auth/__init__.py` (공개: `authenticate`, `authenticate_dhc`, `authenticate_sis`, `SejongAuthClient`, 결과 dataclass 3종, `SejongAuthError`, `ErrorCode`)
- Create: `python/src/sejong_auth/errors.py`, `models.py`, `transport.py`(urllib + CookieJar + SSLContext, 재시도), `parsers.py`(html.parser 기반 표 파싱, json), `client.py`
- Create: `python/tests/test_parsers.py`, `test_client.py`(가짜 transport), `test_live.py`(환경변수 있을 때만)
- Create: `python/README.md`

**Interfaces:**
- Produces:
  - `SejongAuthClient(timeout_seconds: float = 10, max_retry: int = 3, ssl_verification: bool = True)`
  - `client.authenticate(student_id: str, password: str) -> SejongAuthResult`
  - `client.authenticate_dhc(student_id, password, include_raw: bool = False) -> SejongDhcAuthResult`
  - `client.authenticate_sis(student_id, password, include_raw: bool = False) -> SejongSisAuthResult`
  - 모듈 함수 `authenticate/authenticate_dhc/authenticate_sis`는 기본 클라이언트 사용
  - 결과 필드: `success, student_id, name, major, grade, status, classic_reading, email, phone_number, english_name, authenticated_at(datetime), raw_html/raw_json`
  - `SejongAuthError(code: ErrorCode, message: str)`, `ErrorCode` = str Enum, 값은 Java 이름

- [ ] Step 1: 실패 테스트 작성 — `test_parse_dhc_fixture`(expected_dhc.json 일치), `test_parse_sis_fixture`, `test_empty_input_raises_invalid_input_without_network`, `test_wrong_password_401_raises_authentication_failed`, `test_integrated_sis_failure_returns_dhc_with_null_contacts`, `test_timeout_raises_connection_timeout`, `test_retry_exhausted_raises_connection_failed`
- [ ] Step 2: `cd python && python -m pytest` → FAIL
- [ ] Step 3: 구현 (Java 흐름과 같은 순서·헤더, User-Agent `pro-sejong-auth-python/<version>`)
- [ ] Step 4: pytest PASS, `python -m build` 로 wheel·sdist 생성, 새 venv에 wheel 설치 후 import 확인
- [ ] Step 5: 실제 포털 `SEJONG_TEST_*` 설정 후 `test_live.py` 통과 (통합·DHC·SIS·틀린 비밀번호)
- [ ] Step 6: version.yml sync 확인(`version_manager.sh` 가 pyproject 버전도 갱신하는지), Commit

### Task 4: Node 패키지 `pro-sejong-auth`

**Files:**
- Create: `node/package.json` (name `pro-sejong-auth`, version 동일, `engines.node >=18`, `exports` ESM/CJS/types, `files: ["dist"]`, dependencies `{}`, devDependencies typescript, @types/node)
- Create: `node/tsconfig.json`, `node/tsconfig.cjs.json`
- Create: `node/src/index.ts`, `errors.ts`, `models.ts`, `transport.ts`(node:https + 쿠키 jar + 리다이렉트 + 재시도), `parsers.ts`(표 파싱), `client.ts`
- Create: `node/test/*.test.ts` (node:test, fixtures 사용), `node/test/live.test.ts`
- Create: `node/README.md`

**Interfaces:**
- Produces:
  - `new SejongAuthClient({ timeoutSeconds = 10, maxRetry = 3, sslVerification = true })`
  - `client.authenticate(studentId, password): Promise<SejongAuthResult>`
  - `client.authenticateDhc(studentId, password, { includeRaw = false })`, `client.authenticateSis(...)`
  - 모듈 함수 `authenticate/authenticateDhc/authenticateSis`
  - 결과 필드 camelCase(Java와 같은 이름), `authenticatedAt: Date`
  - `class SejongAuthError extends Error { code: ErrorCode }`, `ErrorCode` 문자열 유니온 + 상수 객체

- [ ] Step 1: Python Step 1과 같은 7개 테스트(이름은 camelCase)
- [ ] Step 2: `cd node && npm test` → FAIL
- [ ] Step 3: 구현 (User-Agent `pro-sejong-auth-node/<version>`, cipher·SECLEVEL 설정)
- [ ] Step 4: `npm test` PASS, `npm pack` 결과를 임시 폴더에 설치해 ESM `import`와 CJS `require` 모두 확인
- [ ] Step 5: 실제 포털 live 테스트 통과
- [ ] Step 6: version.yml sync 확인, Commit

### Task 5: 세 언어 교차 검증 스크립트

**Files:**
- Create: `scripts/verify-live.sh` (환경변수 계정으로 Java·Python·Node 각각 통합 인증 실행 > 결과를 `student_id, major, grade, status, classic certifications 합계`만 JSON으로 출력 > 세 결과 비교, 다르면 exit 1). 개인정보는 출력하지 않고 해시 비교
- Create: Java/Python/Node 각 언어의 작은 실행 진입점(테스트 소스 또는 example) — 스크립트가 호출

- [ ] Step 1: 스크립트 작성, 로컬 실행 → 세 언어 일치 출력
- [ ] Step 2: 틀린 비밀번호 케이스도 세 언어 모두 `AUTHENTICATION_FAILED`
- [ ] Step 3: Commit

### Task 6: Java Maven Central 배포

**Files:**
- Modify: `java/build.gradle` (`com.vanniktech.maven.publish` 적용: `coordinates('kr.suhsaechan','sejong-univ-auth', version)`, `publishToMavenCentral(automaticRelease: true)`, `signAllPublications()`는 signing 키 있을 때만, POM 메타는 기존 값, Nexus·GitHubPackages 저장소 유지)
- 참고: `/Users/suhsaechan/Desktop/Programming/project/suh-logger/build-logic/src/main/groovy/suh.publish.gradle`, `docs/maintainers/releasing.md`
- GitHub Secret `GRADLE_PROPERTIES` 갱신: 기존 nexus 값 + `mavenCentralUsername`, `mavenCentralPassword`(cred `sonatype-central-token`), `signingInMemoryKey`(줄바꿈 `\\n` 이중 이스케이프), `signingInMemoryKeyPassword`(cred `gpg-suh-logger`). `/pro-github secrets set` 사용

- [ ] Step 1: 로컬 `./gradlew publishToMavenLocal` 로 서명 없이 산출물(jar, sources, javadoc, pom) 확인
- [ ] Step 2: 키 있는 환경 dry-run(`./gradlew publishToMavenCentral --dry-run`) 태스크 그래프 확인
- [ ] Step 3: Secret 갱신 후 Commit

### Task 7: Python·Node CI·배포 워크플로우

**Files:**
- Create: `.github/workflows/PROJECT-SEJONG-PYTHON-CI.yml` (paths `python/**`, `spec/**`; matrix 3.9·3.12·3.13; pytest)
- Create: `.github/workflows/PROJECT-SEJONG-NODE-CI.yml` (paths `node/**`, `spec/**`; matrix Node 18·20·22; npm test, build)
- Create: `.github/workflows/PROJECT-SEJONG-PYPI-PUBLISH.yml` (on push deploy; build → `pypa/gh-action-pypi-publish` trusted publishing, `permissions: id-token: write`, environment `pypi`)
- Create: `.github/workflows/PROJECT-SEJONG-NPM-PUBLISH.yml` (on push deploy; build → `npm publish --provenance --access public`, `NODE_AUTH_TOKEN` = secret `NPM_TOKEN`, `permissions: id-token: write`)
- PyPI: pending publisher 등록(owner Cassiiopeia, repo pro-sejong-auth, workflow `PROJECT-SEJONG-PYPI-PUBLISH.yml`, environment `pypi`) — chan4760 이메일 인증 후 웹에서
- npm: cred `npm-suhsaechan`으로 granular automation token 발급 → Secret `NPM_TOKEN`

- [ ] Step 1: CI 워크플로우 push → PR/브랜치에서 통과
- [ ] Step 2: 배포 워크플로우는 버전이 이미 있으면 건너뛰는 가드(PyPI `skip-existing`, npm `npm view` 체크) 포함
- [ ] Step 3: Commit

### Task 8: 문서·Pages

**Files:**
- Modify: `README.md`, `README.ko.md` (설치·사용을 Java / Python / Node 세 블록, Maven Central 기준으로 Nexus 저장소 단계 제거, 지원 언어 배지)
- Modify: `CONTRIBUTING.md`, `CONTRIBUTING.ko.md` (언어별 빌드·테스트 명령, spec/ 갱신 규칙: 포털 변경 시 세 언어 같이 수정)
- Modify: gh-pages `index.html` (빠른 시작을 Java/Python/Node 탭, 사용법 코드 탭, 메타·구조화 데이터 `programmingLanguage: [Java, Python, TypeScript]`, 검색어 "세종대 로그인 API Python", "Node")
- Modify: `spec/RESULT_SCHEMA.md` 링크

- [ ] Step 1: 로컬 Pages 확인(탭 전환, 모바일, 콘솔 오류 0)
- [ ] Step 2: Commit (main, gh-pages)

### Task 9: 배포와 설치 검증

- [ ] Step 1: `/pro-code-consult` 점검 → push → `/pro-changelog-deploy` (deploy PR)
- [ ] Step 2: Central·PyPI·npm·Nexus·GitHub Packages 배포 워크플로우 성공 확인
- [ ] Step 3: 깨끗한 임시 폴더에서 설치 검증: Gradle(저장소 설정 없이 mavenCentral만), Maven `dependency:get`, `pip install pro-sejong-auth`, `npm install pro-sejong-auth` → 각자 실제 포털 통합 인증 성공
- [ ] Step 4: GitHub Release 생성(세 언어 설치 방법), 이슈 보고서·완료 처리
