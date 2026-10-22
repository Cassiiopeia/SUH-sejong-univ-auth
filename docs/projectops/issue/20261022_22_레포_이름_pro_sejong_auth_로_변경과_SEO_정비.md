📝 현재 문제점
---

- 레포 이름 `SUH-sejong-univ-auth`가 사용자의 다른 레포 이름 규칙(`pro-` 접두사)과 맞지 않음
- 검색 노출 정비가 부족함: 페이지 제목·설명이 검색어("세종대 로그인 API", "Sejong University login Java" 등)를 충분히 담지 못하고, 언어별 대체 주소(hreflang), robots.txt, 구조화 데이터 보강이 없음

🛠️ 해결 방안 / 제안 기능
---

- 레포 이름을 `pro-sejong-auth`로 변경
- README, CONTRIBUTING, SECURITY, 빌드 설정(POM url, scm), GitHub Pages의 레포 주소를 새 이름으로 갱신, About 홈페이지 주소 변경
- 라이브러리 좌표 `kr.suhsaechan:sejong-univ-auth`는 그대로 유지 (기존 사용자 영향 없음)
- SEO 정비: 검색어 중심 제목·설명(한/영), canonical, hreflang, 구조화 데이터(SoftwareSourceCode, FAQ), sitemap, robots.txt, 레포 About·topics 보강

⚙️ 작업 내용
---

- GitHub 레포 이름 변경과 링크 갱신
- Pages SEO 메타·구조화 데이터·sitemap·robots 정비
- 변경 후 Pages, 데모, 이미지, 구조화 데이터 동작 확인
- 참고: 옛 Pages 주소(cassiiopeia.github.io/SUH-sejong-univ-auth)는 GitHub이 넘겨주지 않아 404가 됨

🙋‍♂️ 담당자
---

- 백엔드: Cassiiopeia
