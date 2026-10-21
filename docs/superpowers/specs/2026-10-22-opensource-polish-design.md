# 오픈소스 정비 설계 (2026-10-22)

## 배경
- 라이브러리 자체는 정상 동작 (실연동 테스트 15/15 통과, v1.2.8)
- 오픈소스 관점 결함
  - `LICENSE` 파일 없음 (README·Pages는 MIT 표기) → 법적으로 사용 불가 상태
  - README 한국어 전용, 데모 캡처 1장, topics·Release 없음, CONTRIBUTING 없음
  - GitHub Pages(gh-pages): 문서를 그대로 옮긴 긴 텍스트, 세종 브랜딩·캡처·데모 동선 없음, 한국어 전용
  - lab 데모(lab.suhsaechan.kr/sejong-auth)는 동작하지만 github.io에서 바로 체험 불가

## 결정 (사용자 확인)
1. 라이선스: MIT `LICENSE` 추가 (저작권자 Suh Saechan)
2. 데모: github.io 페이지에 인증 폼 내장 → lab 서버 `/api/sejong-auth/authenticate` 호출
3. 로고: 세종대 공식 교표(sejong.ac.kr) + 세종 레드 `#C20F2F` · 네이비 `#324D60`, "비공식" 고지 명시

## 변경 범위

### A. SUH-sejong-univ-auth (main)
- `LICENSE` (MIT)
- `README.md` 한국어 재작성 + `README.en.md` 영문, 상단 언어 전환 링크
  - 교표 로고 > 한 줄 가치 > 라이브 데모 버튼 > 결과 캡처 > 빠른 시작 > 사용 예 > API·에러 코드 > 비공식 고지
- `docs/assets/` 로고·캡처(개인정보 마스킹한 실제 데모 결과)
- `CONTRIBUTING.md`: 빌드·테스트·실연동 테스트 실행법

### B. gh-pages (Pages)
- `index.html` 전면 재작성: KO/EN 전환, 세종 브랜딩, 라이트·다크, 내장 데모 폼, 사용법 단계, 캡처
- `og.png` 재생성(교표 포함), 교표·파비콘 자산 추가

### C. suh-project-utility (lab 서버, main 직행 규칙)
- `CorsConfig`: `/api/sejong-auth/**` 에 `https://cassiiopeia.github.io` origin 허용 (자격 증명 없음)
- `WebSecurityConfig`: `/api/sejong-auth/**` CSRF 검사 제외 (세션 상태 변경 없는 무상태 인증 프록시)

### D. GitHub 메타 (승인 후)
- topics 추가, description 영문 병기

## 하지 않는 것
- 라이브러리 동작 코드 변경
- lab 서버 rate limit 신규 도입 (별도 이슈 제안)
- 학교 공식 협의·상표 사용 허가 절차
