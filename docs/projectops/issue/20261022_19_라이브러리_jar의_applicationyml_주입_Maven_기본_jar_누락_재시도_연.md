🗒️ 설명
---

배포 전 전체 코드 점검에서 실제 결함과 정리 대상이 나왔다.

- **라이브러리 jar에 `application.yml`이 들어 있음**: 배포된 1.2.7 jar에 `spring.application.name: sejong-univ-auth`, `sejong.auth.ssl-verification: false`가 포함. 사용하는 앱의 설정 구성에 따라 앱 이름이 바뀌거나 인증서 검증이 꺼질 수 있음
- **Maven 사용자가 라이브러리를 받을 수 없음**: Nexus에 `sejong-univ-auth-{버전}-plain.jar`만 있고 기본 `sejong-univ-auth-{버전}.jar`가 없음 (Gradle은 메타데이터로 우연히 동작)
- **재시도 시 연결 누수**: 실패 응답을 닫지 않고 다시 요청
- 포털 로그인, 재시도, HTTP 클라이언트 생성 코드가 DHC, SIS 클라이언트에 통째로 복사됨. SIS 쪽은 포털 로그인 주소 설정값을 무시하고 하드코딩
- 인증 완료 로그에 학생 이름이 INFO로 남음 (사용하는 서비스 운영 로그에 개인정보)
- 빌드할 때마다 실제 포털에 접속하는 분석용 테스트 15개(성적 조회, JS 다운로드 등)가 남아 있음
- 운영 설정 클래스에 테스트 계정 설정이 섞여 있음, 쓰지 않는 유틸 메서드와 에러 코드
- README: SIS 결과의 학년, 상태를 null로 안내(실제 빈 문자열), TLS 설정이 JVM 전체에 적용된다는 안내 없음

🔄 재현 방법
---

1. Nexus에서 `kr/suhsaechan/sejong-univ-auth/1.2.7/sejong-univ-auth-1.2.7.jar` 요청 > 404
2. `sejong-univ-auth-1.2.7-plain.jar` 압축 목록 > `application.yml` 포함

✅ 예상 동작
---

- 라이브러리 jar는 사용하는 앱 설정을 바꾸지 않는다
- Maven, Gradle 모두 기본 jar 이름으로 받을 수 있다
- 재시도 중 연결이 새지 않는다
- 로그에 이름 등 개인정보를 남기지 않는다

⚙️ 환경 정보
---

- 라이브러리 1.2.7 (Nexus)
