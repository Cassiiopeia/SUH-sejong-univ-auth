# 기여하기 (Contributing)

버그 제보, 포털 변경 대응, 문서 개선 모두 환영합니다.
English speakers: issues and PRs in English are welcome too.

## 이슈 먼저

- 버그: 에러 코드(`SejongAuthException.getErrorCode()`)와 사용한 메서드를 적어 주세요. 학번·비밀번호·개인정보는 절대 올리지 마세요.
- 포털이 바뀌어 인증이 깨졌다면 [데모](https://cassiiopeia.github.io/SUH-sejong-univ-auth/#demo)로 먼저 재현 여부를 확인해 주세요.
- 큰 변경은 PR 전에 이슈로 방향을 맞춥니다.

## 개발 환경

- JDK 17 이상
- Gradle Wrapper 포함 (별도 설치 불필요)

```bash
./gradlew build      # 컴파일 + 단위 테스트
./gradlew test       # 단위 테스트만
```

## 실제 포털 연동 테스트

`SuhSejongAuthEngineTest`의 `실제인증_*` 테스트는 실제 세종대 포털에 로그인하므로 기본으로 `@Disabled` 처리돼 있습니다.

1. `src/test/resources/application-dev.yml` 생성 (`.gitignore` 대상이라 커밋되지 않음)

   ```yaml
   sejong:
     auth:
       test:
         student-id: "본인 학번"
         password: "본인 비밀번호"
   ```

2. 로컬에서만 `@Disabled`를 빼고 실행한 뒤, 커밋 전에 되돌립니다.

   ```bash
   ./gradlew test --tests '*SuhSejongAuthEngineTest'
   ```

> 비밀번호를 여러 번 틀리면 포털 계정이 잠길 수 있습니다. 실패하면 반복 실행하지 말고 원인을 먼저 확인하세요.

## PR

- 브랜치는 `main` 기준으로 만듭니다.
- 동작이 바뀌면 테스트를 함께 추가합니다. 포털 응답 파싱 변경은 원본 HTML·JSON 샘플(개인정보 제거)로 파서 테스트를 남겨 주세요.
- 로그·예외 메시지에 비밀번호나 개인정보가 남지 않는지 확인합니다.

## 라이선스

기여한 코드는 [MIT 라이선스](LICENSE)로 배포됩니다.
