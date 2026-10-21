📝 현재 문제점
---

- 라이브러리는 정상 동작(실제 포털 연동 테스트 15건 전부 통과)하지만, 오픈소스로서 처음 온 사람이 믿고 쓰기 어려운 상태
- `LICENSE` 파일이 없음. README와 Pages에는 MIT라고 적혀 있어 표기와 실제가 다르고, 법적으로는 누구도 사용할 수 없는 상태
- README가 한국어 전용이고 데모 캡처가 옛 이미지 1장뿐. 세종대 서비스라는 정체성(교표, 학교 색)이 드러나지 않음
- GitHub Pages(https://cassiiopeia.github.io/SUH-sejong-univ-auth/)가 README 내용을 길게 옮긴 텍스트 페이지라 첫 화면에서 무엇인지, 어떻게 써보는지 알기 어려움. 한국어 전용
- 직접 체험하려면 별도 사이트(lab.suhsaechan.kr/sejong-auth)로 가야 해서 Pages에서 바로 테스트할 수 없음
- 기여 방법(빌드, 테스트, 실연동 테스트 실행법) 안내가 없음
- 레포 topics 비어 있음

🛠️ 해결 방안 / 제안 기능
---

- MIT `LICENSE` 추가 (기존 표기와 일치)
- README 한국어 재작성 + 영문 README 추가, 상단에서 언어 전환
- 세종대학교 공식 교표와 학교 색(레드, 네이비) 반영, "세종대학교 공식 제품 아님" 고지 명시
- 실제 데모 인증 결과 캡처(개인정보 마스킹) 삽입
- GitHub Pages 전면 리디자인: 한국어/영어 전환, 라이트/다크, 사용법 단계 안내, 캡처, 페이지 안에서 바로 학번/비밀번호로 테스트하는 데모 폼
- 데모 폼은 lab 서버 인증 API를 호출 (lab 서버 쪽 CORS 허용은 suh-project-utility에서 함께 처리)
- `CONTRIBUTING.md` 추가
- 레포 topics, 설명 영문 병기

⚙️ 작업 내용
---

- LICENSE, README.md, README.en.md, CONTRIBUTING.md
- 문서용 이미지 자산(교표, 데모 캡처)
- gh-pages 브랜치 index.html, 공유 이미지(og.png), 파비콘
- 실제 포털 연동 테스트로 동작 재확인

🙋‍♂️ 담당자
---

- 백엔드: Cassiiopeia
- 프론트엔드: Cassiiopeia
- 디자인: Cassiiopeia
