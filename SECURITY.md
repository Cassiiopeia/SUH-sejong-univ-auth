# Security Policy

English | [한국어](#보안-정책)

This library handles Sejong University portal passwords, so security reports are taken seriously.

## Supported versions

Only the latest released version (see the Maven Central, PyPI and npm badges in the [README](README.md)) receives security fixes.

## Reporting a vulnerability

**Do not open a public issue.** Report privately through GitHub:

1. Go to the [Security tab](https://github.com/Cassiiopeia/pro-sejong-auth/security) of this repository
2. Click **Report a vulnerability** ([direct link](https://github.com/Cassiiopeia/pro-sejong-auth/security/advisories/new))
3. Describe the issue, affected version and steps to reproduce

Never include real student IDs, passwords or other personal data in a report. Use placeholders.

You can expect a first response within 7 days. Once a fix is released, the advisory is published with credit to the reporter unless you prefer to stay anonymous.

## Scope

In scope: this library's code, for example leaking passwords or personal data through logs, exceptions or results, unsafe TLS handling, or session mix-ups between calls.

Out of scope: vulnerabilities in the Sejong University portal itself. Report those to the university.

---

## 보안 정책

이 라이브러리는 세종대 포털 비밀번호를 다루므로 보안 제보를 중요하게 처리합니다.

- **지원 버전**: 가장 최근 배포 버전만 보안 수정을 받습니다.
- **신고 방법**: 공개 이슈로 올리지 말고 이 레포의 [Security 탭](https://github.com/Cassiiopeia/pro-sejong-auth/security)에서 **Report a vulnerability**([바로가기](https://github.com/Cassiiopeia/pro-sejong-auth/security/advisories/new))로 비공개 신고해 주세요. 영향 버전과 재현 방법을 적어 주세요.
- 실제 학번, 비밀번호, 개인정보는 절대 넣지 말고 예시값을 쓰세요.
- 7일 안에 첫 답변을 드립니다. 수정이 배포되면 제보자를 밝혀 공지합니다(원하면 익명).
- **범위**: 이 라이브러리 코드(로그, 예외, 결과를 통한 비밀번호나 개인정보 노출, 안전하지 않은 TLS 처리, 호출 간 세션 혼선 등). 세종대 포털 자체의 취약점은 학교에 신고해 주세요.
