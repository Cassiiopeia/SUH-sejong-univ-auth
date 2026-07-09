🗒️ 설명
---

세종대 포털 인증(DHC / SIS / 통합)이 모든 방식에서 `Received fatal alert: handshake_failure`로 실패한다. 최종 응답은 `CONNECTION_FAILED`.

원인은 TLS cipher suite 불일치다. 세종대 포털 서버(`portal` / `classic` / `sjpt.sejong.ac.kr`)는 TLS 1.2에서 `TLS_RSA_WITH_AES_256_CBC_SHA`(openssl 표기 `AES256-SHA`) cipher suite만 제공한다. 그런데 일부 JDK 17 배포판(예: Temurin 17.0.19)부터 `jdk.tls.disabledAlgorithms` 기본값에 `TLS_RSA_*`가 추가되어, 이 cipher가 JSSE(Java TLS 스택)의 지원 목록에서 제외된다. 그 결과 세종대 서버와 공통 cipher가 없어 TLS handshake 단계에서 거부된다.

`wget` / `openssl` 같은 OS TLS 스택은 이 제약이 없어 정상 연결되므로, "네트워크는 되는데 라이브러리만 실패"하는 형태로 나타나 진단이 헷갈린다.

📸 참고 자료
---

- 에러 로그: `k.s.s.auth.client.SejongPortalClient : 세종포털 인증 중 IOException: (handshake_failure) Received fatal alert: handshake_failure`
- 서버 TLS 스펙 (openssl 확인): `Protocol: TLSv1.2`, `Cipher: AES256-SHA` (= `TLS_RSA_WITH_AES_256_CBC_SHA`)
- 컨테이너 JDK: Temurin 17.0.19 → `jdk.tls.disabledAlgorithms`에 `TLS_RSA_*` 포함
- 영향 파일: `SejongPortalClient`, `SejongSisClient`

🔄 재현 방법
---

1. `jdk.tls.disabledAlgorithms`에 `TLS_RSA_*`가 포함된 JDK 17 런타임(예: Temurin 17.0.19)에서 실행
2. `authenticate(studentId, password)` 호출
3. `handshake_failure` 발생 → `CONNECTION_FAILED`로 인증 실패

✅ 예상 동작
---

- 정상 학번/비밀번호로 인증 시 `handshake_failure` 없이 학생 정보가 정상 반환되어야 함
- 해결: TLS 지원 유틸(`SejongTlsSupport`)에서 SSL 클래스 최초 로드 이전에 `jdk.tls.disabledAlgorithms`에서 `TLS_RSA_*`만 제거해 세종대 서버가 요구하는 cipher를 재허용. 각 HTTP 클라이언트(`SejongPortalClient` / `SejongSisClient`)의 `buildClient()`에서 소켓 생성 전에 호출
- 실제 세종대 포털 통신으로 인증 성공 검증 완료

⚙️ 환경 정보
---

- **JDK**: 17 (특히 Temurin 17.0.19 이상 — `TLS_RSA_*` 기본 차단 배포판)
- **대상 서버**: `portal.sejong.ac.kr`, `classic.sejong.ac.kr`, `sjpt.sejong.ac.kr` (TLS 1.2 / AES256-SHA 전용)
- **영향 범위**: 전체 인증 API (DHC / SIS / 통합)

🙋‍♂️ 담당자
---

- **백엔드**: Cassiiopeia
