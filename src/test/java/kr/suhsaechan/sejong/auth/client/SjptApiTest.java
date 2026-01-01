package kr.suhsaechan.sejong.auth.client;

import kr.suhsaechan.sejong.auth.TestApplication;
import kr.suhsaechan.sejong.auth.config.SejongAuthProperties;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * sjpt.sejong.ac.kr API 연동 테스트
 *
 * 학적정보 시스템 API 접근 가능 여부 테스트
 * - SSO 로그인 후 sjpt 시스템 접근
 * - 비밀번호 재확인 (doCheck.do)
 * - 학적정보 조회 (doList.do)
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("dev")
@Slf4j
class SjptApiTest {

  @Autowired
  SejongAuthProperties sejongAuthProperties;

  private OkHttpClient client;
  private String testStudentId;
  private String testPassword;

  // sjpt 관련 URL
  private static final String PORTAL_LOGIN_URL = "https://portal.sejong.ac.kr/jsp/login/login_action.jsp";
  private static final String SJPT_SSO_URL = "https://sjpt.sejong.ac.kr/main/view/Login/doSsoLogin.do?p=";
  private static final String SJPT_CHECK_URL = "https://sjpt.sejong.ac.kr/sch/sch/sud/SudMasterBodyE/doCheck.do";
  private static final String SJPT_LIST_URL = "https://sjpt.sejong.ac.kr/sch/sch/sud/SudMasterBodyE/doList.do";

  @BeforeEach
  void setUp() {
    testStudentId = sejongAuthProperties.getTest().getStudentId();
    testPassword = sejongAuthProperties.getTest().getPassword();

    // OkHttp 클라이언트 생성 (로깅 인터셉터 포함)
    client = buildClientWithLogging();

    log.info("===========================================");
    log.info("테스트 설정 - 학번: {}", testStudentId);
    log.info("===========================================");
  }

  @Test
  @Disabled("실제 포털 연동 테스트 - sjpt는 WebSquare SPA로 순수 HTTP 클라이언트 접근 불가")
  void sjpt_전체플로우_테스트() throws IOException {
    log.info("============ sjpt 전체 플로우 테스트 시작 ============");

    // 1. 포털 로그인
    log.info("\n>>> STEP 1: 포털 로그인");
    doPortalLogin();

    // 2. sjpt SSO 페이지 접근
    log.info("\n>>> STEP 2: sjpt SSO 페이지 접근");
    String ssoResponse = accessSjptSsoPage();
    log.info("SSO 응답 길이: {} 바이트", ssoResponse.length());

    // 3. 비밀번호 재확인
    log.info("\n>>> STEP 3: 비밀번호 재확인 (doCheck.do)");
    String checkResponse = doPasswordCheck();
    log.info("doCheck 응답:\n{}", checkResponse);

    // 4. 학적정보 조회
    log.info("\n>>> STEP 4: 학적정보 조회 (doList.do)");
    String listResponse = doFetchStudentInfo();
    log.info("doList 응답:\n{}", listResponse);

    // 응답 분석
    assertNotNull(listResponse);
    assertTrue(listResponse.contains("dl_mainList") || listResponse.length() > 0,
        "학적정보 응답이 있어야 합니다");

    log.info("============ sjpt 전체 플로우 테스트 완료 ============");
  }

  @Test
  @Disabled("실제 포털 연동 테스트 - 로컬에서만 실행")
  void step1_포털로그인만_테스트() throws IOException {
    log.info("============ STEP 1: 포털 로그인 테스트 ============");

    String response = doPortalLogin();

    log.info("로그인 응답 길이: {} 바이트", response.length());
    log.info("로그인 응답 일부:\n{}",
        response.substring(0, Math.min(500, response.length())));

    assertNotNull(response);
  }

  @Test
  @Disabled("실제 포털 연동 테스트 - 로컬에서만 실행")
  void step2_SSO페이지_테스트() throws IOException {
    log.info("============ STEP 2: SSO 페이지 접근 테스트 ============");

    // 먼저 로그인
    doPortalLogin();

    // SSO 페이지 접근
    String response = accessSjptSsoPage();

    log.info("SSO 응답 길이: {} 바이트", response.length());
    log.info("SSO 전체 응답:\n{}", response);

    assertNotNull(response);
  }

  @Test
  @Disabled("실제 포털 연동 테스트 - 로컬에서만 실행")
  void step3_비밀번호확인_테스트() throws IOException {
    log.info("============ STEP 3: 비밀번호 확인 테스트 ============");

    // 먼저 로그인 + SSO
    doPortalLogin();
    accessSjptSsoPage();

    // 비밀번호 확인
    String response = doPasswordCheck();

    log.info("doCheck 응답:\n{}", response);

    assertNotNull(response);
  }

  @Test
  @Disabled("실제 포털 연동 테스트 - 로컬에서만 실행")
  void step4_학적정보조회_테스트() throws IOException {
    log.info("============ STEP 4: 학적정보 조회 테스트 ============");

    // 전체 플로우 실행
    doPortalLogin();
    accessSjptSsoPage();
    doPasswordCheck();

    // 학적정보 조회
    String response = doFetchStudentInfo();

    log.info("doList 응답:\n{}", response);

    assertNotNull(response);

    // 응답에 학생 정보가 있는지 확인
    if (response.contains("NM_ENG")) {
      log.info("✅ 영어이름(NM_ENG) 필드 발견!");
    }
    if (response.contains("NM_CHI")) {
      log.info("✅ 한자이름(NM_CHI) 필드 발견!");
    }
    if (response.contains("HIGH_SCH_NM")) {
      log.info("✅ 출신고교(HIGH_SCH_NM) 필드 발견!");
    }
  }

  // ==================== JavaScript 리버스 엔지니어링 테스트 ====================

  @Test
  @Disabled("JavaScript 분석 테스트 - 분석 완료")
  void JS분석_WebSquare_모든JS검색() throws IOException {
    log.info("============ WebSquare JavaScript 분석 ============");

    // 1. 먼저 로그인
    doPortalLogin();

    // 2. SSO 페이지 접근하여 쿠키 획득
    accessSjptSsoPage();

    // 3. 다양한 JavaScript 파일 검색
    String[] jsUrls = {
        "https://sjpt.sejong.ac.kr/websquare/javascript.wq?q=/bootloader",
        "https://sjpt.sejong.ac.kr/websquare/javascript.wq?q=/websquare",
        "https://sjpt.sejong.ac.kr/main/common_layout.xml",
        "https://sjpt.sejong.ac.kr/main/js/common.js",
        "https://sjpt.sejong.ac.kr/main/js/main.js",
        "https://sjpt.sejong.ac.kr/websquare/engine/websquare.js"
    };

    for (String jsUrl : jsUrls) {
      try {
        Request jsRequest = new Request.Builder()
            .url(jsUrl)
            .get()
            .header("Referer", SJPT_SSO_URL)
            .build();

        try (Response response = client.newCall(jsRequest).execute()) {
          String content = response.body() != null ? response.body().string() : "";
          log.info("\n>>> URL: {} - 상태: {}, 길이: {} 바이트", jsUrl, response.code(), content.length());

          // _runningSejong 검색
          if (content.contains("_runningSejong")) {
            log.info("✅ '_runningSejong' 발견!");
            int idx = content.indexOf("_runningSejong");
            int start = Math.max(0, idx - 300);
            int end = Math.min(content.length(), idx + 500);
            log.info("관련 코드:\n{}", content.substring(start, end));
          }

          // runningSejong (언더스코어 없이) 검색
          if (content.contains("runningSejong")) {
            log.info("✅ 'runningSejong' 발견!");
            int idx = content.indexOf("runningSejong");
            int start = Math.max(0, idx - 300);
            int end = Math.min(content.length(), idx + 500);
            log.info("관련 코드:\n{}", content.substring(start, end));
          }

          // uuid 생성 관련 검색
          if (content.contains("uuid") || content.contains("UUID") || content.contains("generateUUID")) {
            log.info("✅ UUID 관련 키워드 발견!");
          }

          // 세션 ID 관련 검색
          if (content.contains("sessionId") || content.contains("getSessionId")) {
            log.info("✅ sessionId 관련 키워드 발견!");
          }
        }
      } catch (Exception e) {
        log.info("URL: {} - 에러: {}", jsUrl, e.getMessage());
      }
    }

    // 4. 세션 정보 API 직접 호출 시도
    log.info("\n============ 세션 정보 API 탐색 ============");

    // WebSquare에서 사용자 정보를 가져오는 API
    String sessionApiUrl = "https://sjpt.sejong.ac.kr/main/ext/Login/getLoginUserInfo.do";
    try {
      Request request = new Request.Builder()
          .url(sessionApiUrl)
          .get()
          .header("Accept", "application/json")
          .header("Referer", SJPT_SSO_URL)
          .build();

      try (Response response = client.newCall(request).execute()) {
        String body = response.body() != null ? response.body().string() : "";
        log.info("getLoginUserInfo 응답: 상태={}, 본문={}", response.code(), body);

        // UUID 패턴 검색
        if (body.matches(".*[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}.*")) {
          log.info("✅ UUID 패턴 발견! 이게 _runningSejong 값일 수 있음");
        }
      }
    } catch (Exception e) {
      log.info("세션 API 에러: {}", e.getMessage());
    }
  }

  @Test
  @Disabled("JavaScript 분석 테스트")
  void JS분석_세션정보_API_탐색() throws IOException {
    log.info("============ 세션 정보 API 탐색 ============");

    // 1. 로그인
    doPortalLogin();

    // 2. SSO 페이지 접근
    accessSjptSsoPage();

    // 3. 세션/사용자 정보를 반환하는 API 탐색
    String[] possibleApis = {
        "https://sjpt.sejong.ac.kr/main/ext/Login/getLoginInfo.do",
        "https://sjpt.sejong.ac.kr/main/ext/Login/getSessionInfo.do",
        "https://sjpt.sejong.ac.kr/websquare/session.wq",
        "https://sjpt.sejong.ac.kr/main/common/getSession.do"
    };

    for (String apiUrl : possibleApis) {
      try {
        Request request = new Request.Builder()
            .url(apiUrl)
            .get()
            .header("Accept", "application/json")
            .header("Referer", SJPT_SSO_URL)
            .build();

        try (Response response = client.newCall(request).execute()) {
          String body = response.body() != null ? response.body().string() : "";
          log.info("API: {} - 상태: {}, 응답: {}", apiUrl, response.code(),
              body.substring(0, Math.min(500, body.length())));

          // UUID 형태의 값이 있는지 확인
          if (body.matches(".*[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}.*")) {
            log.info("✅ UUID 형태 발견! - 이게 _runningSejong일 수 있음");
          }
        }
      } catch (Exception e) {
        log.info("API: {} - 에러: {}", apiUrl, e.getMessage());
      }
    }
  }

  @Test
  @Disabled("SSO 페이지 HTML 상세 분석 - 분석 완료")
  void SSO페이지_HTML상세분석_runningSejong_출처확인() throws IOException {
    log.info("============ SSO 페이지 HTML 상세 분석 ============");

    // 1. 로그인
    doPortalLogin();

    // 2. SSO 페이지 접근
    String ssoHtml = accessSjptSsoPage();

    // 3. HTML에서 _runningSejong 검색
    if (ssoHtml.contains("_runningSejong")) {
      log.info("✅ SSO HTML에서 '_runningSejong' 발견!");
      int idx = ssoHtml.indexOf("_runningSejong");
      int start = Math.max(0, idx - 200);
      int end = Math.min(ssoHtml.length(), idx + 300);
      log.info("관련 코드:\n{}", ssoHtml.substring(start, end));
    } else {
      log.info("❌ SSO HTML에서 '_runningSejong' 없음");
    }

    // 4. runningSejong (언더스코어 없이) 검색
    if (ssoHtml.contains("runningSejong")) {
      log.info("✅ SSO HTML에서 'runningSejong' 발견!");
      int idx = ssoHtml.indexOf("runningSejong");
      int start = Math.max(0, idx - 200);
      int end = Math.min(ssoHtml.length(), idx + 300);
      log.info("관련 코드:\n{}", ssoHtml.substring(start, end));
    }

    // 5. UUID 패턴 검색 (xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx)
    java.util.regex.Pattern uuidPattern = java.util.regex.Pattern.compile(
        "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}",
        java.util.regex.Pattern.CASE_INSENSITIVE
    );
    java.util.regex.Matcher matcher = uuidPattern.matcher(ssoHtml);
    if (matcher.find()) {
      log.info("✅ UUID 패턴 발견: {}", matcher.group());
      // 컨텍스트 출력
      int idx = matcher.start();
      int start = Math.max(0, idx - 100);
      int end = Math.min(ssoHtml.length(), idx + 150);
      log.info("UUID 주변 컨텍스트:\n{}", ssoHtml.substring(start, end));
    } else {
      log.info("❌ UUID 패턴 없음");
    }

    // 6. WebSquare 초기화 관련 스크립트 분석
    log.info("\n--- WebSquare 관련 스크립트 분석 ---");
    if (ssoHtml.contains("WebSquare")) {
      log.info("✅ WebSquare 키워드 발견");
    }
    if (ssoHtml.contains("wq:initialize")) {
      log.info("✅ wq:initialize 발견");
    }

    // 7. JSON 형태의 설정 데이터 찾기
    java.util.regex.Pattern jsonPattern = java.util.regex.Pattern.compile(
        "\\{[^{}]*_run[^{}]*\\}",
        java.util.regex.Pattern.CASE_INSENSITIVE
    );
    matcher = jsonPattern.matcher(ssoHtml);
    while (matcher.find()) {
      log.info("✅ _run 관련 JSON 발견: {}", matcher.group());
    }

    // 8. 전체 HTML 출력 (분석용)
    log.info("\n=== SSO 페이지 전체 HTML ===\n{}", ssoHtml);
  }

  @Test
  @Disabled("common_layout.xml 분석 - 분석 완료: RUNNING_SEJONG 발견")
  void common_layout_xml_상세분석() throws IOException {
    log.info("============ common_layout.xml 분석 ============");

    // 1. 로그인
    doPortalLogin();

    // 2. SSO 페이지 접근
    accessSjptSsoPage();

    // 3. common_layout.xml 다운로드
    String layoutUrl = "https://sjpt.sejong.ac.kr/main/common_layout.xml";
    Request request = new Request.Builder()
        .url(layoutUrl)
        .get()
        .header("Referer", SJPT_SSO_URL)
        .build();

    try (Response response = client.newCall(request).execute()) {
      String content = response.body() != null ? response.body().string() : "";
      log.info("common_layout.xml 길이: {} 바이트", content.length());

      // _runningSejong 검색
      if (content.contains("_runningSejong")) {
        log.info("✅ '_runningSejong' 발견!");
        int idx = content.indexOf("_runningSejong");
        int start = Math.max(0, idx - 500);
        int end = Math.min(content.length(), idx + 500);
        log.info("관련 코드:\n{}", content.substring(start, end));
      }

      // runningSejong 검색
      if (content.contains("runningSejong")) {
        log.info("✅ 'runningSejong' 발견!");
        // 모든 발생 위치 출력
        int idx = 0;
        int count = 0;
        while ((idx = content.indexOf("runningSejong", idx)) != -1) {
          count++;
          int start = Math.max(0, idx - 300);
          int end = Math.min(content.length(), idx + 400);
          log.info(">>> 발생 #{} <<<\n{}", count, content.substring(start, end));
          log.info("─────────────────────────────────────");
          idx++;
        }
        log.info("총 {} 번 발견됨", count);
      }

      // UUID 생성 관련 검색
      if (content.contains("UUID") || content.contains("uuid")) {
        log.info("✅ UUID 관련 키워드 발견");
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(".{0,100}[Uu][Uu][Ii][Dd].{0,100}");
        java.util.regex.Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
          log.info("UUID 컨텍스트: {}", matcher.group());
        }
      }

      // generateSessionId, getSession 검색
      if (content.contains("Session") || content.contains("session")) {
        log.info("✅ Session 관련 키워드 발견");
      }

      // WebSquare 초기화 관련
      if (content.contains("onpageload") || content.contains("onPageLoad")) {
        log.info("✅ onpageload 발견 - 초기화 로직 있음");
      }

      // gcm (Global Context Manager) 검색
      if (content.contains("gcm") || content.contains("GCM")) {
        log.info("✅ gcm 관련 발견");
      }

      // 전체 내용 중 처음 10000자 출력
      log.info("\n=== common_layout.xml (처음 15000자) ===\n{}",
          content.substring(0, Math.min(15000, content.length())));
    }
  }

  @Test
  @Disabled("순수 HTTP로 학적정보 전체 플로우 - 불가: WebSquare pgmRole 세션 설정 필요")
  void 순수HTTP_학적정보_전체플로우_테스트() throws IOException {
    log.info("============ 순수 HTTP 학적정보 전체 플로우 테스트 ============");

    // STEP 1: 포털 로그인
    log.info("\n>>> STEP 1: 포털 로그인");
    doPortalLogin();

    // STEP 2: SSO 페이지 접근 (JSESSIONID 획득)
    log.info("\n>>> STEP 2: SSO 페이지 접근");
    accessSjptSsoPage();

    // STEP 3: doListUserMenuListTop.do 호출하여 RUNNING_SEJONG 획득
    log.info("\n>>> STEP 3: RUNNING_SEJONG 획득");
    String runningSejong = fetchRunningSejong();
    log.info("✅ RUNNING_SEJONG: {}", runningSejong);
    assertNotNull(runningSejong, "RUNNING_SEJONG 값이 있어야 합니다");

    // STEP 4: 좌측 메뉴 조회 (프로그램 역할 정보 획득)
    log.info("\n>>> STEP 4: 좌측 메뉴 조회 (doListUserMenuListLeft.do)");
    String leftMenuResponse = fetchLeftMenu("SELF_STUD", "SCH");
    log.info("좌측 메뉴 응답 길이: {}", leftMenuResponse.length());

    // STEP 5: 프로그램 초기화 호출 (학적정보 화면 진입)
    log.info("\n>>> STEP 5: 프로그램 초기화");
    initProgram(runningSejong);

    // STEP 6: doCheck.do 호출 (비밀번호 재확인)
    log.info("\n>>> STEP 6: 비밀번호 재확인 (doCheck.do)");
    String checkResponse = doPasswordCheckWithRunningSejong(runningSejong);
    log.info("doCheck 응답:\n{}", checkResponse);

    // STEP 7: doList.do 호출 (학적정보 조회)
    log.info("\n>>> STEP 7: 학적정보 조회 (doList.do)");
    String listResponse = doFetchStudentInfoWithRunningSejong(runningSejong);
    log.info("doList 응답:\n{}", listResponse);

    // 응답 검증
    assertNotNull(listResponse);
    assertTrue(listResponse.length() > 0, "응답이 있어야 합니다");

    // 학생 정보 필드 확인
    if (listResponse.contains("NM_ENG")) {
      log.info("✅✅✅ 영어이름(NM_ENG) 필드 발견!");
    }
    if (listResponse.contains("NM_CHI")) {
      log.info("✅✅✅ 한자이름(NM_CHI) 필드 발견!");
    }
    if (listResponse.contains("HIGH_SCH_NM")) {
      log.info("✅✅✅ 출신고교(HIGH_SCH_NM) 필드 발견!");
    }
    if (listResponse.contains("BIRTH_DATE") || listResponse.contains("BRTHDY")) {
      log.info("✅✅✅ 생년월일 필드 발견!");
    }

    log.info("\n============ 순수 HTTP 학적정보 전체 플로우 완료 ============");
  }

  /**
   * 좌측 메뉴 조회 (프로그램 역할 정보 획득)
   */
  private String fetchLeftMenu(String menuSysId, String systemDiv) throws IOException {
    String jsonPayload = String.format(
        "{\"MENU_SYS_ID\":\"%s\",\"SYSTEM_DIV\":\"%s\",\"MENU_SYS_NM\":\"\"}",
        menuSysId, systemDiv
    );

    RequestBody body = RequestBody.create(
        jsonPayload,
        MediaType.parse("application/json; charset=UTF-8")
    );

    String apiUrl = "https://sjpt.sejong.ac.kr/main/view/Menu/doListUserMenuListLeft.do";
    Request request = new Request.Builder()
        .url(apiUrl)
        .post(body)
        .header("Accept", "application/json")
        .header("Content-Type", "application/json; charset=UTF-8")
        .header("Referer", SJPT_SSO_URL)
        .header("Origin", "https://sjpt.sejong.ac.kr")
        .build();

    try (Response response = client.newCall(request).execute()) {
      String responseBody = response.body() != null ? response.body().string() : "";
      log.info("doListUserMenuListLeft 응답 - 상태: {}, 길이: {}", response.code(), responseBody.length());
      return responseBody;
    }
  }

  /**
   * 프로그램 초기화 (학적정보 화면 진입)
   */
  private void initProgram(String runningSejong) throws IOException {
    // WebSquare에서 화면을 열 때 호출하는 API
    // SudMasterBodyE.xml 로딩
    String xmlUrl = "https://sjpt.sejong.ac.kr/sch/sch/sud/SudMasterBodyE.xml";
    String addParam = generateAddParamWithRunningSejong(runningSejong);

    Request request = new Request.Builder()
        .url(xmlUrl + "?addParam=" + addParam)
        .get()
        .header("Accept", "application/xml")
        .header("Referer", SJPT_SSO_URL)
        .build();

    try (Response response = client.newCall(request).execute()) {
      String responseBody = response.body() != null ? response.body().string() : "";
      log.info("프로그램 XML 로딩 - 상태: {}, 길이: {}", response.code(), responseBody.length());
    }
  }

  /**
   * RUNNING_SEJONG 값을 획득
   */
  private String fetchRunningSejong() throws IOException {
    String jsonPayload = "{}";
    RequestBody body = RequestBody.create(
        jsonPayload,
        MediaType.parse("application/json; charset=UTF-8")
    );

    String apiUrl = "https://sjpt.sejong.ac.kr/main/view/Menu/doListUserMenuListTop.do";
    Request request = new Request.Builder()
        .url(apiUrl)
        .post(body)
        .header("Accept", "application/json")
        .header("Content-Type", "application/json; charset=UTF-8")
        .header("Referer", SJPT_SSO_URL)
        .header("Origin", "https://sjpt.sejong.ac.kr")
        .build();

    try (Response response = client.newCall(request).execute()) {
      String responseBody = response.body() != null ? response.body().string() : "";
      log.info("doListUserMenuListTop 응답 - 상태: {}", response.code());

      // RUNNING_SEJONG 추출
      java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
          "\"RUNNING_SEJONG\"\\s*:\\s*\"([^\"]+)\""
      );
      java.util.regex.Matcher matcher = pattern.matcher(responseBody);
      if (matcher.find()) {
        return matcher.group(1);
      }
      return null;
    }
  }

  /**
   * RUNNING_SEJONG 값을 사용하여 addParam 생성
   */
  private String generateAddParamWithRunningSejong(String runningSejong) {
    // 현재 시간 생성
    String loginDt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

    // JSON 생성
    String json = String.format(
        "{\"_runPgmKey\":\"SELF_STUDSELF_SUB_20SELF_MENU_10SudMasterBodyE\"," +
            "\"_runSysKey\":\"SCH\"," +
            "\"_runIntgUsrNo\":\"%s\"," +
            "\"_runPgLoginDt\":\"%s\"," +
            "\"_runningSejong\":\"%s\"}",
        testStudentId, loginDt, runningSejong
    );

    log.info("addParam JSON: {}", json);

    // URL 인코딩 후 Base64 인코딩
    String urlEncoded = URLEncoder.encode(json, StandardCharsets.UTF_8);
    return Base64.getEncoder().encodeToString(urlEncoded.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * 비밀번호 재확인 (RUNNING_SEJONG 사용)
   */
  private String doPasswordCheckWithRunningSejong(String runningSejong) throws IOException {
    String addParam = generateAddParamWithRunningSejong(runningSejong);

    String jsonPayload = String.format(
        "{\"dm_search\":{\"STUDENT_NO\":\"\",\"PASSWORD\":\"%s\"}}",
        testPassword
    );

    RequestBody body = RequestBody.create(
        jsonPayload,
        MediaType.parse("application/json; charset=UTF-8")
    );

    String urlWithParam = SJPT_CHECK_URL + "?addParam=" + addParam;

    Request request = new Request.Builder()
        .url(urlWithParam)
        .post(body)
        .header("Accept", "application/json")
        .header("Content-Type", "application/json; charset=UTF-8")
        .header("Referer", SJPT_SSO_URL)
        .header("Origin", "https://sjpt.sejong.ac.kr")
        .build();

    try (Response response = client.newCall(request).execute()) {
      String responseBody = response.body() != null ? response.body().string() : "";
      log.info("doCheck - 상태: {}, 응답길이: {}", response.code(), responseBody.length());
      return responseBody;
    }
  }

  /**
   * 학적정보 조회 (RUNNING_SEJONG 사용)
   */
  private String doFetchStudentInfoWithRunningSejong(String runningSejong) throws IOException {
    String addParam = generateAddParamWithRunningSejong(runningSejong);

    String jsonPayload = String.format(
        "{\"dm_search\":{\"ORGN_CLSF_CD\":\"20\",\"STUDENT_NO\":\"%s\",\"BRANCH\":\"\",\"GDT_JUDGE_CD\":\"\",\"GDT_YEAR\":\"\",\"GDT_SMT_CD\":\"\",\"SMT_CD\":\"\",\"TAB_NO\":\"\",\"YEAR\":\"\"}}",
        testStudentId
    );

    RequestBody body = RequestBody.create(
        jsonPayload,
        MediaType.parse("application/json; charset=UTF-8")
    );

    String urlWithParam = SJPT_LIST_URL + "?addParam=" + addParam;

    Request request = new Request.Builder()
        .url(urlWithParam)
        .post(body)
        .header("Accept", "application/json")
        .header("Content-Type", "application/json; charset=UTF-8")
        .header("Referer", SJPT_SSO_URL)
        .header("Origin", "https://sjpt.sejong.ac.kr")
        .build();

    try (Response response = client.newCall(request).execute()) {
      String responseBody = response.body() != null ? response.body().string() : "";
      log.info("doList - 상태: {}, 응답길이: {}", response.code(), responseBody.length());
      return responseBody;
    }
  }

  /**
   * ★★★ initUserInfo.do API를 통한 사용자 정보 조회 테스트 ★★★
   *
   * 결론: 순수 HTTP로 접근 가능한 API
   * - initUserInfo.do: 영어이름, 학과, 이메일 등 기본 정보 제공
   * - doCheck.do, doList.do: pgmRole 세션 필요하여 접근 불가
   */
  @Test
  void initUserInfo_사용자정보_조회_성공_테스트() throws IOException {
    log.info("============ initUserInfo.do 사용자 정보 조회 테스트 ============");

    // STEP 1: 포털 로그인
    log.info("\n>>> STEP 1: 포털 로그인");
    doPortalLogin();

    // STEP 2: SSO 페이지 접근 (JSESSIONID 획득)
    log.info("\n>>> STEP 2: SSO 페이지 접근");
    accessSjptSsoPage();

    // STEP 3: initUserInfo.do 호출 - 사용자 정보 획득
    log.info("\n>>> STEP 3: initUserInfo.do 호출");
    String initResponse = callInitUserInfo();

    // 응답 검증
    assertNotNull(initResponse, "initUserInfo 응답이 있어야 합니다");
    assertTrue(initResponse.length() > 100, "initUserInfo 응답이 충분히 있어야 합니다");

    log.info("\n========== initUserInfo.do 응답 분석 ==========");

    // 주요 필드 확인
    if (initResponse.contains("\"INTG_USR_NO\"")) {
      log.info("✅ 학번(INTG_USR_NO) 필드 발견");
    }
    if (initResponse.contains("\"INTG_USR_NM\"") || initResponse.contains("\"NM\"")) {
      log.info("✅ 이름(INTG_USR_NM/NM) 필드 발견");
    }
    if (initResponse.contains("\"INTG_ENG_NM\"") || initResponse.contains("\"NM_ENG\"")) {
      log.info("✅ 영어이름(INTG_ENG_NM/NM_ENG) 필드 발견");
    }
    if (initResponse.contains("\"DEPT_CD\"") || initResponse.contains("\"DEPT_NM\"")) {
      log.info("✅ 학과(DEPT_CD/DEPT_NM) 필드 발견");
    }
    if (initResponse.contains("\"EMAIL\"") || initResponse.contains("\"USER_EMAIL\"")) {
      log.info("✅ 이메일(EMAIL/USER_EMAIL) 필드 발견");
    }
    if (initResponse.contains("\"RUNNING_SEJONG\"")) {
      log.info("✅ RUNNING_SEJONG 필드 발견");
    }

    // 응답 출력
    log.info("\ninitUserInfo 전체 응답:\n{}", initResponse);

    log.info("\n========== 테스트 완료 ==========");
    log.info("📌 결론: initUserInfo.do를 통해 다음 정보 접근 가능:");
    log.info("   - 학번 (INTG_USR_NO)");
    log.info("   - 이름 (INTG_USR_NM, NM)");
    log.info("   - 영어이름 (INTG_ENG_NM, NM_ENG)");
    log.info("   - 학과코드/학과명 (DEPT_CD, DEPT_NM)");
    log.info("   - 이메일 (EMAIL, USER_EMAIL)");
    log.info("   - 로그인시간, IP 등 세션정보");
    log.info("\n❌ 접근 불가 정보 (pgmRole 세션 필요):");
    log.info("   - 한자이름 (NM_CHI)");
    log.info("   - 출신고교 (HIGH_SCH_NM)");
    log.info("   - 상세 학적정보");
  }

  @Test
  @Disabled("doCheck.do, doList.do 접근 불가 - pgmRole 세션 필요")
  void doCheckDoList_pgmRole_세션_필요_테스트() throws IOException {
    // 이 테스트는 pgmRole 세션 문제로 실패함을 문서화
    log.info("============ doCheck/doList pgmRole 세션 문제 문서화 ============");
    log.info("WebSquare 프레임워크의 서버 측 세션 관리(pgmRole) 때문에");
    log.info("순수 HTTP 클라이언트로는 doCheck.do와 doList.do에 접근 불가");
    log.info("브라우저 자동화(Playwright 등) 필요");
  }

  // ==================== 성적조회 API 테스트 (SugRecordQ) ====================

  /**
   * ★★★ SugRecordQ/doList.do API를 통한 성적 조회 테스트 ★★★
   *
   * 브라우저 네트워크 분석 결과:
   * - URL: https://sjpt.sejong.ac.kr/sch/sch/sug/SugRecordQ/doList.do
   * - 요청 본문: {"dm_search":{"ORGN_CLSF_CD":"20","YEAR":"","SMT_CD":"","RECORD_YN":"Y","STUDENT_NO":"학번","STUDENT_NM":"","YEAR_SMT":""}}
   * - 응답: 전체 수강 성적 데이터 (학기별 과목명, 학점, 성적, 평점 등)
   */
  @Test
  void SugRecordQ_성적조회_테스트() throws IOException {
    log.info("============ SugRecordQ/doList.do 성적 조회 테스트 ============");

    // STEP 1: 포털 로그인
    log.info("\n>>> STEP 1: 포털 로그인");
    doPortalLogin();

    // STEP 2: SSO 페이지 접근 (JSESSIONID 획득)
    log.info("\n>>> STEP 2: SSO 페이지 접근");
    accessSjptSsoPage();

    // STEP 3: initUserInfo.do 호출 - 세션 초기화 및 RUNNING_SEJONG 획득
    log.info("\n>>> STEP 3: initUserInfo.do 호출 - RUNNING_SEJONG 획득");
    String initResponse = callInitUserInfo();
    String runningSejong = extractRunningSejong(initResponse);
    log.info("RUNNING_SEJONG: {}", runningSejong);

    // STEP 4: SugRecordQ/doList.do 호출 - 성적 조회
    log.info("\n>>> STEP 4: SugRecordQ/doList.do 호출 - 성적 조회");
    String gradeResponse = callSugRecordQDoList(runningSejong);

    // 응답 검증 및 분석
    log.info("\n========== SugRecordQ/doList.do 응답 분석 ==========");
    log.info("응답 길이: {} 바이트", gradeResponse.length());
    log.info("응답 내용:\n{}", gradeResponse);

    // 주요 필드 확인
    if (gradeResponse.contains("dl_main")) {
      log.info("✅ dl_main (성적 데이터 배열) 필드 발견");
    }
    if (gradeResponse.contains("CURI_NM")) {
      log.info("✅ CURI_NM (과목명) 필드 발견");
    }
    if (gradeResponse.contains("GRADE")) {
      log.info("✅ GRADE (성적) 필드 발견");
    }
    if (gradeResponse.contains("MRKS")) {
      log.info("✅ MRKS (평점) 필드 발견");
    }
    if (gradeResponse.contains("CDT")) {
      log.info("✅ CDT (학점) 필드 발견");
    }

    // 에러 확인
    if (gradeResponse.contains("Exception") || gradeResponse.contains("error")) {
      log.warn("⚠️ 에러 응답 감지됨");
    }

    log.info("\n========== 테스트 완료 ==========");
  }

  /**
   * SugRecordQ/doYearsmt.do - 연도/학기 목록 조회
   */
  @Test
  void SugRecordQ_연도학기목록_조회_테스트() throws IOException {
    log.info("============ SugRecordQ/doYearsmt.do 연도/학기 목록 조회 테스트 ============");

    // 로그인 + SSO
    doPortalLogin();
    accessSjptSsoPage();

    // initUserInfo.do 호출
    String initResponse = callInitUserInfo();
    String runningSejong = extractRunningSejong(initResponse);

    // doYearsmt.do 호출
    log.info("\n>>> doYearsmt.do 호출");
    String yearSmtResponse = callSugRecordQDoYearsmt(runningSejong);

    log.info("doYearsmt 응답:\n{}", yearSmtResponse);
  }

  // ==================== XML 페이지 로드 후 API 호출 테스트 (pgmRole 우회 시도) ====================

  /**
   * ★★★ XML 페이지 로드 후 성적 조회 테스트 ★★★
   *
   * 가설: WebSquare에서 XML 페이지(화면)를 GET으로 먼저 로드하면
   * 서버 세션에 pgmRole이 설정되어 이후 API 호출이 성공할 수 있음
   *
   * 테스트 흐름:
   * 1. 포털 로그인
   * 2. SSO 페이지 접근
   * 3. initUserInfo.do 호출 (RUNNING_SEJONG 획득)
   * 4. ★ SugRecordQ.xml 페이지 GET 요청 (pgmRole 설정 시도)
   * 5. SugRecordQ/doList.do 호출
   */
  @Test
  void XML페이지로드_후_성적조회_테스트() throws IOException {
    log.info("============ XML 페이지 로드 후 성적 조회 테스트 ============");
    log.info("가설: XML 페이지 로드 시 서버에서 pgmRole 세션 설정");

    // STEP 1: 포털 로그인
    log.info("\n>>> STEP 1: 포털 로그인");
    doPortalLogin();

    // STEP 2: SSO 페이지 접근 (JSESSIONID 획득)
    log.info("\n>>> STEP 2: SSO 페이지 접근");
    accessSjptSsoPage();

    // STEP 3: initUserInfo.do 호출 - RUNNING_SEJONG 획득
    log.info("\n>>> STEP 3: initUserInfo.do 호출");
    String initResponse = callInitUserInfo();
    String runningSejong = extractRunningSejong(initResponse);
    log.info("RUNNING_SEJONG: {}", runningSejong);

    // STEP 4: ★★★ SugRecordQ.xml 페이지 GET 요청 ★★★
    log.info("\n>>> STEP 4: SugRecordQ.xml 페이지 로드 (pgmRole 설정 시도)");
    String xmlResponse = loadSugRecordQPage(runningSejong);
    log.info("XML 페이지 응답 길이: {} 바이트", xmlResponse.length());

    // XML 페이지 응답 분석
    if (xmlResponse.contains("pgmRole") || xmlResponse.contains("ROLE")) {
      log.info("✅ XML 응답에 pgmRole/ROLE 관련 키워드 발견");
    }

    // STEP 5: SugRecordQ/doList.do 호출
    log.info("\n>>> STEP 5: SugRecordQ/doList.do 호출");
    String gradeResponse = callSugRecordQDoList(runningSejong);

    // 결과 분석
    log.info("\n========== 결과 분석 ==========");
    log.info("응답 길이: {} 바이트", gradeResponse.length());
    log.info("응답 내용:\n{}", gradeResponse);

    // 성공 여부 판별
    if (gradeResponse.contains("Exception") || gradeResponse.contains("NullPointerException")) {
      log.warn("❌ 여전히 pgmRole 오류 발생 - XML 로드로는 해결 안됨");
    } else if (gradeResponse.contains("dl_main") || gradeResponse.contains("CURI_NM")) {
      log.info("✅✅✅ 성공! XML 페이지 로드로 pgmRole 문제 해결됨!");
    }
  }

  /**
   * XML 페이지 + 메뉴 API 조합 테스트
   *
   * 더 완전한 브라우저 플로우 시뮬레이션:
   * 1. 포털 로그인
   * 2. SSO 페이지 접근
   * 3. initUserInfo.do 호출
   * 4. doListUserMenuListTop.do 호출 (상단 메뉴)
   * 5. doListUserMenuListLeft.do 호출 (좌측 메뉴 - 프로그램 역할 정보)
   * 6. SugRecordQ.xml 페이지 로드
   * 7. SugRecordQ/doList.do 호출
   */
  @Test
  void 전체메뉴플로우_후_성적조회_테스트() throws IOException {
    log.info("============ 전체 메뉴 플로우 후 성적 조회 테스트 ============");

    // STEP 1: 포털 로그인
    log.info("\n>>> STEP 1: 포털 로그인");
    doPortalLogin();

    // STEP 2: SSO 페이지 접근
    log.info("\n>>> STEP 2: SSO 페이지 접근");
    accessSjptSsoPage();

    // STEP 3: initUserInfo.do 호출
    log.info("\n>>> STEP 3: initUserInfo.do 호출");
    String initResponse = callInitUserInfo();
    String runningSejong = extractRunningSejong(initResponse);
    log.info("RUNNING_SEJONG: {}", runningSejong);

    // STEP 4: doListUserMenuListTop.do 호출
    log.info("\n>>> STEP 4: doListUserMenuListTop.do 호출 (상단 메뉴)");
    String topMenuResponse = fetchRunningSejongFromMenuTop();
    log.info("상단 메뉴 응답 길이: {} 바이트", topMenuResponse != null ? topMenuResponse.length() : 0);

    // STEP 5: doListUserMenuListLeft.do 호출 (SELF_STUD, SCH)
    log.info("\n>>> STEP 5: doListUserMenuListLeft.do 호출 (좌측 메뉴)");
    String leftMenuResponse = fetchLeftMenuWithAddParam("SELF_STUD", "SCH", runningSejong);
    log.info("좌측 메뉴 응답 길이: {}", leftMenuResponse.length());

    // STEP 6: SugRecordQ.xml 페이지 로드
    log.info("\n>>> STEP 6: SugRecordQ.xml 페이지 로드");
    String xmlResponse = loadSugRecordQPage(runningSejong);
    log.info("XML 페이지 응답 길이: {} 바이트", xmlResponse.length());

    // STEP 7: SugRecordQ/doList.do 호출
    log.info("\n>>> STEP 7: SugRecordQ/doList.do 호출");
    String gradeResponse = callSugRecordQDoList(runningSejong);

    // 결과 분석
    log.info("\n========== 결과 분석 ==========");
    log.info("응답 내용:\n{}", gradeResponse);

    if (gradeResponse.contains("Exception") || gradeResponse.contains("NullPointerException")) {
      log.warn("❌ pgmRole 오류 발생");
    } else if (gradeResponse.contains("dl_main") || gradeResponse.contains("CURI_NM")) {
      log.info("✅✅✅ 성공!");
    }
  }

  /**
   * SugRecordQ.xml 페이지 로드 - WebSquare 엔진을 통해 로드
   */
  private String loadSugRecordQPage(String runningSejong) throws IOException {
    String addParam = generateAddParamForSugRecordQ(runningSejong);

    // WebSquare 엔진을 통해 페이지 로드 (websquare.wq 엔드포인트 사용)
    String pageUrl = "https://sjpt.sejong.ac.kr/websquare/websquare.wq?w2xPath=/sch/sch/sug/SugRecordQ.xml&addParam=" + addParam;

    Request request = new Request.Builder()
        .url(pageUrl)
        .get()
        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        .header("Referer", "https://sjpt.sejong.ac.kr/main/common_layout.xml")
        .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        .build();

    try (Response response = client.newCall(request).execute()) {
      String responseBody = response.body() != null ? response.body().string() : "";
      log.info("SugRecordQ (WebSquare) - 상태: {}, 응답길이: {}", response.code(), responseBody.length());

      // 응답 일부 출력 (처음 500자)
      if (responseBody.length() > 500) {
        log.info("응답 (처음 500자):\n{}", responseBody.substring(0, 500));
      } else {
        log.info("응답:\n{}", responseBody);
      }

      return responseBody;
    }
  }

  /**
   * doListUserMenuListTop.do 호출 - 문자열 응답 반환
   */
  private String fetchRunningSejongFromMenuTop() throws IOException {
    String jsonPayload = "{}";
    RequestBody body = RequestBody.create(
        jsonPayload,
        MediaType.parse("application/json; charset=UTF-8")
    );

    String apiUrl = "https://sjpt.sejong.ac.kr/main/view/Menu/doListUserMenuListTop.do";
    Request request = new Request.Builder()
        .url(apiUrl)
        .post(body)
        .header("Accept", "application/json")
        .header("Content-Type", "application/json; charset=UTF-8")
        .header("Referer", SJPT_SSO_URL)
        .header("Origin", "https://sjpt.sejong.ac.kr")
        .build();

    try (Response response = client.newCall(request).execute()) {
      String responseBody = response.body() != null ? response.body().string() : "";
      log.info("doListUserMenuListTop - 상태: {}, 응답길이: {}", response.code(), responseBody.length());
      return responseBody;
    }
  }

  /**
   * SchStudentBaseInfo/doStudent.do - 학생 기본정보 조회
   */
  @Test
  void SchStudentBaseInfo_학생기본정보_조회_테스트() throws IOException {
    log.info("============ SchStudentBaseInfo/doStudent.do 학생 기본정보 조회 테스트 ============");

    // 로그인 + SSO
    doPortalLogin();
    accessSjptSsoPage();

    // initUserInfo.do 호출
    String initResponse = callInitUserInfo();
    String runningSejong = extractRunningSejong(initResponse);

    // doStudent.do 호출
    log.info("\n>>> doStudent.do 호출");
    String studentResponse = callSchStudentBaseInfoDoStudent(runningSejong);

    log.info("doStudent 응답:\n{}", studentResponse);

    // 주요 필드 확인
    if (studentResponse.contains("NM") || studentResponse.contains("STUDENT_NM")) {
      log.info("✅ 학생 이름 필드 발견");
    }
    if (studentResponse.contains("DEPT")) {
      log.info("✅ 학과 정보 필드 발견");
    }
  }

  /**
   * SugRecordQ/doList.do 호출
   */
  private String callSugRecordQDoList(String runningSejong) throws IOException {
    // addParam 생성 - 성적조회용 프로그램 키
    String addParam = generateAddParamForSugRecordQ(runningSejong);

    // 요청 본문 - 브라우저에서 캡처한 형식
    String jsonPayload = String.format(
        "{\"dm_search\":{\"ORGN_CLSF_CD\":\"20\",\"YEAR\":\"\",\"SMT_CD\":\"\",\"RECORD_YN\":\"Y\",\"STUDENT_NO\":\"%s\",\"STUDENT_NM\":\"\",\"YEAR_SMT\":\"\"}}",
        testStudentId
    );

    RequestBody body = RequestBody.create(
        jsonPayload,
        MediaType.parse("application/json; charset=UTF-8")
    );

    String apiUrl = "https://sjpt.sejong.ac.kr/sch/sch/sug/SugRecordQ/doList.do?addParam=" + addParam;

    Request request = new Request.Builder()
        .url(apiUrl)
        .post(body)
        .header("Accept", "application/json")
        .header("Content-Type", "application/json; charset=UTF-8")
        .header("Referer", "https://sjpt.sejong.ac.kr/sch/sch/sug/SugRecordQ.xml")
        .header("Origin", "https://sjpt.sejong.ac.kr")
        .build();

    try (Response response = client.newCall(request).execute()) {
      String responseBody = response.body() != null ? response.body().string() : "";
      log.info("SugRecordQ/doList - 상태: {}, 응답길이: {}", response.code(), responseBody.length());
      return responseBody;
    }
  }

  /**
   * SugRecordQ/doYearsmt.do 호출
   */
  private String callSugRecordQDoYearsmt(String runningSejong) throws IOException {
    String addParam = generateAddParamForSugRecordQ(runningSejong);

    String jsonPayload = String.format(
        "{\"dm_search\":{\"RECORD_YN\":\"Y\",\"YEAR_SMT\":\"\",\"ORGN_CLSF_CD\":\"20\",\"STUDENT_NO\":\"%s\"}}",
        testStudentId
    );

    RequestBody body = RequestBody.create(
        jsonPayload,
        MediaType.parse("application/json; charset=UTF-8")
    );

    String apiUrl = "https://sjpt.sejong.ac.kr/sch/sch/sug/SugRecordQ/doYearsmt.do?addParam=" + addParam;

    Request request = new Request.Builder()
        .url(apiUrl)
        .post(body)
        .header("Accept", "application/json")
        .header("Content-Type", "application/json; charset=UTF-8")
        .header("Referer", "https://sjpt.sejong.ac.kr/sch/sch/sug/SugRecordQ.xml")
        .header("Origin", "https://sjpt.sejong.ac.kr")
        .build();

    try (Response response = client.newCall(request).execute()) {
      String responseBody = response.body() != null ? response.body().string() : "";
      log.info("SugRecordQ/doYearsmt - 상태: {}, 응답길이: {}", response.code(), responseBody.length());
      return responseBody;
    }
  }

  /**
   * SchStudentBaseInfo/doStudent.do 호출
   */
  private String callSchStudentBaseInfoDoStudent(String runningSejong) throws IOException {
    String addParam = generateAddParamForSchStudentBaseInfo(runningSejong);

    String jsonPayload = String.format(
        "{\"dm_reqKey\":{\"keyOrgnClsfCd\":\"20\",\"keyStudentNo\":\"%s\",\"keyStudentImagPath\":\"\",\"keyYear\":\"\",\"keySmtCd\":\"\"}}",
        testStudentId
    );

    RequestBody body = RequestBody.create(
        jsonPayload,
        MediaType.parse("application/json; charset=UTF-8")
    );

    String apiUrl = "https://sjpt.sejong.ac.kr/sch/sch/cmn/SchStudentBaseInfo/doStudent.do?addParam=" + addParam;

    Request request = new Request.Builder()
        .url(apiUrl)
        .post(body)
        .header("Accept", "application/json")
        .header("Content-Type", "application/json; charset=UTF-8")
        .header("Referer", "https://sjpt.sejong.ac.kr/sch/sch/sug/SugRecordQ.xml")
        .header("Origin", "https://sjpt.sejong.ac.kr")
        .build();

    try (Response response = client.newCall(request).execute()) {
      String responseBody = response.body() != null ? response.body().string() : "";
      log.info("SchStudentBaseInfo/doStudent - 상태: {}, 응답길이: {}", response.code(), responseBody.length());
      return responseBody;
    }
  }

  /**
   * SugRecordQ용 addParam 생성
   * _runPgmKey: SELF_STUDSELF_SUB_30SCH_SUG05_STUDSugRecordQ
   */
  private String generateAddParamForSugRecordQ(String runningSejong) {
    String loginDt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

    String json = String.format(
        "{\"_runPgmKey\":\"SELF_STUDSELF_SUB_30SCH_SUG05_STUDSugRecordQ\"," +
            "\"_runSysKey\":\"SCH\"," +
            "\"_runIntgUsrNo\":\"%s\"," +
            "\"_runPgLoginDt\":\"%s\"," +
            "\"_runningSejong\":\"%s\"}",
        testStudentId, loginDt, runningSejong != null ? runningSejong : ""
    );

    log.info("SugRecordQ addParam JSON: {}", json);

    String urlEncoded = URLEncoder.encode(json, StandardCharsets.UTF_8);
    return Base64.getEncoder().encodeToString(urlEncoded.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * SchStudentBaseInfo용 addParam 생성
   */
  private String generateAddParamForSchStudentBaseInfo(String runningSejong) {
    String loginDt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

    String json = String.format(
        "{\"_runPgmKey\":\"SELF_STUDSELF_SUB_30SCH_SUG05_STUDSugRecordQ\"," +
            "\"_runSysKey\":\"SCH\"," +
            "\"_runIntgUsrNo\":\"%s\"," +
            "\"_runPgLoginDt\":\"%s\"," +
            "\"_runningSejong\":\"%s\"}",
        testStudentId, loginDt, runningSejong != null ? runningSejong : ""
    );

    String urlEncoded = URLEncoder.encode(json, StandardCharsets.UTF_8);
    return Base64.getEncoder().encodeToString(urlEncoded.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * initUserInfo 응답에서 RUNNING_SEJONG 값 추출
   */
  private String extractRunningSejong(String initResponse) {
    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
        "\"RUNNING_SEJONG\"\\s*:\\s*\"([^\"]+)\""
    );
    java.util.regex.Matcher matcher = pattern.matcher(initResponse);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return null;
  }

  /**
   * 좌측 메뉴 조회 (addParam 포함)
   */
  private String fetchLeftMenuWithAddParam(String menuSysId, String systemDiv, String runningSejong) throws IOException {
    String addParam = generateAddParamForMenu(runningSejong, menuSysId, systemDiv);

    String jsonPayload = String.format(
        "{\"MENU_SYS_ID\":\"%s\",\"SYSTEM_DIV\":\"%s\",\"MENU_SYS_NM\":\"\"}",
        menuSysId, systemDiv
    );

    RequestBody body = RequestBody.create(
        jsonPayload,
        MediaType.parse("application/json; charset=UTF-8")
    );

    String apiUrl = "https://sjpt.sejong.ac.kr/main/view/Menu/doListUserMenuListLeft.do?addParam=" + addParam;
    Request request = new Request.Builder()
        .url(apiUrl)
        .post(body)
        .header("Accept", "application/json")
        .header("Content-Type", "application/json; charset=UTF-8")
        .header("Referer", SJPT_SSO_URL)
        .header("Origin", "https://sjpt.sejong.ac.kr")
        .build();

    try (Response response = client.newCall(request).execute()) {
      String responseBody = response.body() != null ? response.body().string() : "";
      log.info("doListUserMenuListLeft - 상태: {}, 길이: {}", response.code(), responseBody.length());
      return responseBody;
    }
  }

  /**
   * SudMasterBodyE.xml 페이지 로딩 (WebSquare에서 화면 열 때 호출)
   */
  private String loadSudMasterBodyEPage(String runningSejong) throws IOException {
    String addParam = generateAddParamWithRunningSejong(runningSejong);

    // WebSquare 페이지 요청
    String pageUrl = "https://sjpt.sejong.ac.kr/sch/sch/sud/SudMasterBodyE.xml?addParam=" + addParam;
    Request request = new Request.Builder()
        .url(pageUrl)
        .get()
        .header("Accept", "text/html,application/xhtml+xml,application/xml")
        .header("Referer", SJPT_SSO_URL)
        .build();

    try (Response response = client.newCall(request).execute()) {
      String responseBody = response.body() != null ? response.body().string() : "";
      log.info("SudMasterBodyE.xml - 상태: {}, 길이: {}", response.code(), responseBody.length());
      return responseBody;
    }
  }

  /**
   * 메뉴용 addParam 생성
   */
  private String generateAddParamForMenu(String runningSejong, String menuSysId, String systemDiv) {
    String loginDt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

    String json = String.format(
        "{\"_runPgmKey\":\"%s\"," +
            "\"_runSysKey\":\"%s\"," +
            "\"_runIntgUsrNo\":\"%s\"," +
            "\"_runPgLoginDt\":\"%s\"," +
            "\"_runningSejong\":\"%s\"}",
        menuSysId, systemDiv, testStudentId, loginDt, runningSejong
    );

    log.info("Menu addParam JSON: {}", json);

    String urlEncoded = URLEncoder.encode(json, StandardCharsets.UTF_8);
    return Base64.getEncoder().encodeToString(urlEncoded.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * initUserInfo.do 호출 - 세션 초기화 (pgmRole 설정)
   * 브라우저에서 SSO 로그인 직후 가장 먼저 호출하는 API
   */
  private String callInitUserInfo() throws IOException {
    // 브라우저처럼 빈 addParam으로 호출
    String emptyAddParam = generateEmptyAddParam();

    String jsonPayload = "{}";  // 빈 JSON

    RequestBody body = RequestBody.create(
        jsonPayload,
        MediaType.parse("application/json; charset=UTF-8")
    );

    String apiUrl = "https://sjpt.sejong.ac.kr/main/sys/UserInfo/initUserInfo.do?addParam=" + emptyAddParam;

    Request request = new Request.Builder()
        .url(apiUrl)
        .post(body)
        .header("Accept", "application/json")
        .header("Content-Type", "application/json; charset=UTF-8")
        .header("Referer", SJPT_SSO_URL)
        .header("Origin", "https://sjpt.sejong.ac.kr")
        .build();

    try (Response response = client.newCall(request).execute()) {
      String responseBody = response.body() != null ? response.body().string() : "";
      log.info("initUserInfo - 상태: {}, 응답길이: {}", response.code(), responseBody.length());
      return responseBody;
    }
  }

  /**
   * 빈 addParam 생성 (브라우저 초기 호출 시 사용)
   */
  private String generateEmptyAddParam() {
    // 브라우저가 처음 호출할 때 사용하는 빈 값의 addParam
    String json = "{\"_runIntgUsrNo\":\"\",\"_runPgLoginDt\":\"\",\"_runningSejong\":\"\"}";

    log.info("Empty addParam JSON: {}", json);

    // URL 인코딩 후 Base64 인코딩
    String urlEncoded = URLEncoder.encode(json, StandardCharsets.UTF_8);
    return Base64.getEncoder().encodeToString(urlEncoded.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  @Disabled("RUNNING_SEJONG 획득 테스트")
  void RUNNING_SEJONG_획득_테스트() throws IOException {
    log.info("============ RUNNING_SEJONG 획득 테스트 ============");

    // 1. 로그인
    doPortalLogin();

    // 2. SSO 페이지 접근
    accessSjptSsoPage();

    // 3. subUserMenuListTop API 호출 (dm_UserInfo에 RUNNING_SEJONG 값이 채워짐)
    log.info("\n>>> STEP 3: doListUserMenuListTop.do API 호출");

    // dm_CoMessage 형태로 요청 (ref="data:json,dm_CoMessage")
    String jsonPayload = "{}";  // 빈 JSON으로 시작

    RequestBody body = RequestBody.create(
        jsonPayload,
        MediaType.parse("application/json; charset=UTF-8")
    );

    String apiUrl = "https://sjpt.sejong.ac.kr/main/view/Menu/doListUserMenuListTop.do";
    Request request = new Request.Builder()
        .url(apiUrl)
        .post(body)
        .header("Accept", "application/json")
        .header("Content-Type", "application/json; charset=UTF-8")
        .header("Referer", SJPT_SSO_URL)
        .header("Origin", "https://sjpt.sejong.ac.kr")
        .build();

    try (Response response = client.newCall(request).execute()) {
      String responseBody = response.body() != null ? response.body().string() : "";
      log.info("doListUserMenuListTop 응답 - 상태: {}, 길이: {}", response.code(), responseBody.length());
      log.info("응답 본문:\n{}", responseBody);

      // RUNNING_SEJONG 또는 runningSejong 검색
      if (responseBody.contains("RUNNING_SEJONG")) {
        log.info("✅ 'RUNNING_SEJONG' 발견!");
        // JSON에서 값 추출 시도
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "\"RUNNING_SEJONG\"\\s*:\\s*\"([^\"]+)\""
        );
        java.util.regex.Matcher matcher = pattern.matcher(responseBody);
        if (matcher.find()) {
          String runningSejongValue = matcher.group(1);
          log.info("✅✅✅ RUNNING_SEJONG 값: {}", runningSejongValue);
        }
      }

      // UUID 패턴 검색
      java.util.regex.Pattern uuidPattern = java.util.regex.Pattern.compile(
          "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}",
          java.util.regex.Pattern.CASE_INSENSITIVE
      );
      java.util.regex.Matcher uuidMatcher = uuidPattern.matcher(responseBody);
      while (uuidMatcher.find()) {
        log.info("UUID 발견: {}", uuidMatcher.group());
      }

      // dm_UserInfo 관련 데이터 검색
      if (responseBody.contains("INTG_USR_NO") || responseBody.contains("INTG_USR_NM")) {
        log.info("✅ 사용자 정보 발견!");
      }
    }
  }

  @Test
  @Disabled("JavaScript 분석 테스트")
  void JS분석_쿠키에서_세션ID_추출() throws IOException {
    log.info("============ 쿠키 분석 ============");

    // 1. 로그인
    doPortalLogin();

    // 2. SSO 페이지 접근
    accessSjptSsoPage();

    // 3. 쿠키 출력 (CookieJar에서)
    log.info("현재 저장된 쿠키 확인 - 인터셉터 로그 참조");

    // 4. 개인정보 동의 API 호출 (SSO 페이지에서 호출하는 것)
    String privacyCheckUrl = "https://sjpt.sejong.ac.kr/main/ext/PsnInfoAffa/doAgreeCheckMain.do";
    Request request = new Request.Builder()
        .url(privacyCheckUrl)
        .get()
        .header("Accept", "application/json")
        .header("Referer", SJPT_SSO_URL)
        .build();

    try (Response response = client.newCall(request).execute()) {
      String body = response.body() != null ? response.body().string() : "";
      log.info("개인정보 동의 체크 응답: {}", body);

      // 이 응답에 세션 정보가 있을 수 있음
      if (body.contains("_runningSejong") || body.contains("sessionId") || body.contains("uuid")) {
        log.info("✅ 세션 정보 발견!");
      }
    }
  }

  // ==================== WebSquare JS 파일 다운로드 및 분석 ====================

  /**
   * ★★★ WebSquare JS 파일 다운로드 및 pgmRole 코드 분석 ★★★
   *
   * 목적: pgmRole이 어떻게 설정되는지 JavaScript 소스 분석
   *
   * 다운로드 대상 파일:
   * - com.UserRole.js: 사용자 역할/권한 관련 (pgmRole 설정 가능성 높음)
   * - com.UserInfo.js: 사용자 정보 조회 (initUserInfo.do 호출)
   * - com.Submit.js: 서버 요청 전송 관련
   * - gcm.js: Global Context Manager
   */
  @Test
  void WebSquare_JS파일_다운로드_및_분석() throws IOException {
    log.info("============ WebSquare JS 파일 다운로드 및 pgmRole 분석 ============");

    // STEP 1: 포털 로그인
    log.info("\n>>> STEP 1: 포털 로그인");
    doPortalLogin();

    // STEP 2: SSO 페이지 접근 (세션 쿠키 획득)
    log.info("\n>>> STEP 2: SSO 페이지 접근");
    accessSjptSsoPage();

    // STEP 3: JS 파일 다운로드
    log.info("\n>>> STEP 3: WebSquare JS 파일 다운로드");

    String[] jsFiles = {
        "/main/js/com/com.UserRole.js",
        "/main/js/com/com.UserInfo.js",
        "/main/js/com/com.Submit.js",
        "/main/js/com/gcm.js",
        "/main/js/com/com.Menu.js",
        "/main/js/com/com.Session.js"
    };

    for (String jsPath : jsFiles) {
      log.info("\n─────────────────────────────────────────");
      log.info(">>> 파일 다운로드: {}", jsPath);
      String jsContent = downloadWebSquareJsFile(jsPath);

      if (jsContent != null && !jsContent.isEmpty() && !jsContent.contains("404")) {
        log.info("✅ 다운로드 성공 - 크기: {} 바이트", jsContent.length());

        // pgmRole 관련 코드 검색
        analyzePgmRoleInJsContent(jsPath, jsContent);
      } else {
        log.warn("❌ 다운로드 실패 또는 404: {}", jsPath);
      }
    }

    log.info("\n============ JS 파일 분석 완료 ============");
  }

  /**
   * 개별 JS 파일을 다운로드하여 상세 분석
   */
  @Test
  void WebSquare_UserRole_JS_상세분석() throws IOException {
    log.info("============ com.UserRole.js 상세 분석 ============");

    // 로그인 + SSO
    doPortalLogin();
    accessSjptSsoPage();

    // com.UserRole.js 다운로드
    String jsContent = downloadWebSquareJsFile("/main/js/com/com.UserRole.js");

    if (jsContent != null && !jsContent.isEmpty()) {
      log.info("✅ com.UserRole.js 다운로드 성공 - 크기: {} 바이트", jsContent.length());

      // 전체 내용 출력 (분석용)
      log.info("\n========== com.UserRole.js 전체 소스 ==========\n{}", jsContent);

      // 핵심 키워드 검색
      String[] keywords = {
          "pgmRole", "PGM_ROLE", "runPgmRole", "setRole", "getRole",
          "ROLE_CD", "roleCode", "userRole", "menuRole",
          "addParam", "_runPgmKey", "_runningSejong",
          "doListUserMenuListLeft", "doListUserMenuListTop"
      };

      log.info("\n========== 키워드 검색 결과 ==========");
      for (String keyword : keywords) {
        if (jsContent.contains(keyword)) {
          log.info("✅ '{}' 발견!", keyword);
          // 해당 키워드 주변 컨텍스트 출력
          printContextAroundKeyword(jsContent, keyword, 200);
        }
      }
    } else {
      log.warn("❌ com.UserRole.js 다운로드 실패");
    }
  }

  /**
   * gcm.js (Global Context Manager) 상세 분석
   */
  @Test
  void WebSquare_gcm_JS_상세분석() throws IOException {
    log.info("============ gcm.js 상세 분석 ============");

    // 로그인 + SSO
    doPortalLogin();
    accessSjptSsoPage();

    // gcm.js 다운로드
    String jsContent = downloadWebSquareJsFile("/main/js/com/gcm.js");

    if (jsContent != null && !jsContent.isEmpty()) {
      log.info("✅ gcm.js 다운로드 성공 - 크기: {} 바이트", jsContent.length());

      // 전체 내용 출력 (분석용)
      log.info("\n========== gcm.js 전체 소스 ==========\n{}", jsContent);

      // pgmRole 관련 키워드 검색
      analyzePgmRoleInJsContent("/main/js/com/gcm.js", jsContent);
    } else {
      log.warn("❌ gcm.js 다운로드 실패");
    }
  }

  /**
   * config.xml 다운로드 (WebSquare 설정 파일)
   */
  @Test
  void WebSquare_config_xml_다운로드() throws IOException {
    log.info("============ WebSquare config.xml 다운로드 ============");

    // 로그인 + SSO
    doPortalLogin();
    accessSjptSsoPage();

    // config.xml 다운로드
    String configUrl = "https://sjpt.sejong.ac.kr/main/js/com/config.xml";
    Request request = new Request.Builder()
        .url(configUrl)
        .get()
        .header("Accept", "*/*")
        .header("Referer", "https://sjpt.sejong.ac.kr/main/common_layout.xml")
        .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
        .build();

    try (Response response = client.newCall(request).execute()) {
      String content = response.body() != null ? response.body().string() : "";
      log.info("config.xml - 상태: {}, 크기: {} 바이트", response.code(), content.length());

      if (response.code() == 200) {
        log.info("\n========== config.xml 내용 ==========\n{}", content);

        // JS 파일 경로 추출
        if (content.contains("src=")) {
          log.info("\n>>> JS 파일 경로 목록 (src 속성):");
          java.util.regex.Pattern srcPattern = java.util.regex.Pattern.compile("src=\"([^\"]+\\.js)\"");
          java.util.regex.Matcher matcher = srcPattern.matcher(content);
          while (matcher.find()) {
            log.info("  - {}", matcher.group(1));
          }
        }
      }
    }
  }

  /**
   * common_layout.xml 다운로드 및 JS 파일 경로 추출
   *
   * 목적: 실제 JS 파일들이 어느 경로에 있는지 확인
   */
  @Test
  void WebSquare_common_layout_분석() throws IOException {
    log.info("============ common_layout.xml 분석 및 JS 경로 추출 ============");

    // 로그인 + SSO
    doPortalLogin();
    accessSjptSsoPage();

    // common_layout.xml 다운로드
    String layoutUrl = "https://sjpt.sejong.ac.kr/main/common_layout.xml";
    Request request = new Request.Builder()
        .url(layoutUrl)
        .get()
        .header("Accept", "*/*")
        .header("Referer", "https://sjpt.sejong.ac.kr/main/view/Login/doSsoLogin.do?p=")
        .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
        .build();

    try (Response response = client.newCall(request).execute()) {
      String content = response.body() != null ? response.body().string() : "";
      log.info("common_layout.xml - 상태: {}, 크기: {} 바이트", response.code(), content.length());

      if (response.code() == 200) {
        // JS 파일 경로 추출 (src 속성)
        log.info("\n>>> JS 파일 경로 목록:");
        java.util.regex.Pattern srcPattern = java.util.regex.Pattern.compile("src\\s*=\\s*[\"']([^\"']+\\.(js|xml))[\"']");
        java.util.regex.Matcher matcher = srcPattern.matcher(content);
        java.util.Set<String> jsPaths = new java.util.TreeSet<>();
        while (matcher.find()) {
          jsPaths.add(matcher.group(1));
        }
        jsPaths.forEach(path -> log.info("  - {}", path));

        // pgmRole 관련 키워드 검색
        log.info("\n>>> pgmRole 관련 키워드 검색:");
        String[] keywords = {"pgmRole", "runPgmRole", "ROLE", "_runPgmKey", "runningSejong"};
        for (String keyword : keywords) {
          if (content.contains(keyword)) {
            log.info("✅ '{}' 발견!", keyword);
            printContextAroundKeyword(content, keyword, 200);
          }
        }

        // 전체 내용 출력 (처음 20000자)
        log.info("\n========== common_layout.xml (처음 20000자) ==========\n{}",
            content.substring(0, Math.min(20000, content.length())));
      }
    }
  }

  /**
   * ★★★ common_layout.xml 전체 소스 파일로 저장 ★★★
   *
   * 목적: common_layout.xml에 포함된 JavaScript 코드 전체 분석
   * - inline script로 포함된 JS 코드들을 파일로 저장
   * - pgmRole 관련 코드 위치 확인
   */
  @Test
  void WebSquare_common_layout_전체저장() throws IOException {
    log.info("============ common_layout.xml 전체 저장 및 분석 ============");

    // 로그인 + SSO
    doPortalLogin();
    accessSjptSsoPage();

    // common_layout.xml 다운로드
    String layoutUrl = "https://sjpt.sejong.ac.kr/main/common_layout.xml";
    Request request = new Request.Builder()
        .url(layoutUrl)
        .get()
        .header("Accept", "*/*")
        .header("Referer", "https://sjpt.sejong.ac.kr/main/view/Login/doSsoLogin.do?p=")
        .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
        .build();

    try (Response response = client.newCall(request).execute()) {
      String content = response.body() != null ? response.body().string() : "";
      log.info("common_layout.xml - 상태: {}, 크기: {} 바이트", response.code(), content.length());

      if (response.code() == 200) {
        // 파일로 저장
        java.nio.file.Path outputPath = java.nio.file.Paths.get("build", "common_layout.xml");
        java.nio.file.Files.createDirectories(outputPath.getParent());
        java.nio.file.Files.writeString(outputPath, content);
        log.info("✅ 파일 저장: {}", outputPath.toAbsolutePath());

        // pgmRole 관련 검색
        log.info("\n========== pgmRole 관련 코드 검색 ==========");
        String[] keywords = {
            "pgmRole", "PGM_ROLE", "runPgmRole", "setRole", "getRole",
            "ROLE_CD", "roleCode", "menuRole", "_runPgmKey",
            "RuningPgmRole", "getPgmRole"
        };

        for (String keyword : keywords) {
          if (content.contains(keyword)) {
            log.info("\n✅ '{}' 발견!", keyword);
            printAllOccurrences(content, keyword, 300);
          }
        }

        // com.UserRole 검색
        if (content.contains("com.UserRole")) {
          log.info("\n✅ 'com.UserRole' 발견!");
          printAllOccurrences(content, "com.UserRole", 300);
        }

        // com.js 파일 로드 패턴 검색
        log.info("\n========== JS 파일 로드 패턴 검색 ==========");
        java.util.regex.Pattern jsPattern = java.util.regex.Pattern.compile("\\$p\\.js\\([^)]+\\)");
        java.util.regex.Matcher jsMatcher = jsPattern.matcher(content);
        while (jsMatcher.find()) {
          log.info("  - {}", jsMatcher.group());
        }

        // left.xml, top.xml 검색
        if (content.contains("left.xml") || content.contains("top.xml")) {
          log.info("\n>>> left.xml, top.xml 참조 발견");
        }
      }
    }
  }

  /**
   * left.xml 다운로드 - 좌측 메뉴 관련 JS 분석
   */
  @Test
  void WebSquare_left_xml_분석() throws IOException {
    log.info("============ left.xml 분석 ============");

    // 로그인 + SSO
    doPortalLogin();
    accessSjptSsoPage();

    // left.xml 다운로드
    String leftUrl = "https://sjpt.sejong.ac.kr/main/left.xml";
    Request request = new Request.Builder()
        .url(leftUrl)
        .get()
        .header("Accept", "*/*")
        .header("Referer", "https://sjpt.sejong.ac.kr/main/common_layout.xml")
        .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
        .build();

    try (Response response = client.newCall(request).execute()) {
      String content = response.body() != null ? response.body().string() : "";
      log.info("left.xml - 상태: {}, 크기: {} 바이트", response.code(), content.length());

      if (response.code() == 200) {
        // 파일로 저장
        java.nio.file.Path outputPath = java.nio.file.Paths.get("build", "left.xml");
        java.nio.file.Files.createDirectories(outputPath.getParent());
        java.nio.file.Files.writeString(outputPath, content);
        log.info("✅ 파일 저장: {}", outputPath.toAbsolutePath());

        // pgmRole 관련 검색
        log.info("\n========== pgmRole 관련 코드 검색 ==========");
        String[] keywords = {
            "pgmRole", "PGM_ROLE", "runPgmRole", "setRole", "getRole",
            "ROLE_CD", "roleCode", "menuRole", "_runPgmKey", "openMenu", "openTab"
        };

        for (String keyword : keywords) {
          if (content.contains(keyword)) {
            log.info("\n✅ '{}' 발견!", keyword);
            printAllOccurrences(content, keyword, 300);
          }
        }

        // 전체 내용 출력 (분석용)
        log.info("\n========== left.xml 전체 소스 (처음 30000자) ==========\n{}",
            content.substring(0, Math.min(30000, content.length())));
      }
    }
  }

  /**
   * 키워드의 모든 발생 위치 출력
   */
  private void printAllOccurrences(String content, String keyword, int contextLength) {
    int idx = 0;
    int count = 0;
    while ((idx = content.indexOf(keyword, idx)) != -1) {
      count++;
      int start = Math.max(0, idx - contextLength);
      int end = Math.min(content.length(), idx + keyword.length() + contextLength);
      String context = content.substring(start, end).replace("\n", " ").replace("\r", "").replace("\t", " ");
      log.info("  [#{} 위치 {}] ...{}...", count, idx, context);
      idx++;
    }
    log.info("  총 {} 번 발견됨", count);
  }

  /**
   * ★★★ WebSquare javascript.wq를 통해 gcm.js 및 com 관련 JS 다운로드 ★★★
   *
   * WebSquare는 javascript.wq 엔드포인트를 통해 JS 파일들을 번들로 제공
   */
  @Test
  void WebSquare_javascript_wq_다운로드() throws IOException {
    log.info("============ WebSquare javascript.wq 다운로드 ============");

    // 로그인 + SSO
    doPortalLogin();
    accessSjptSsoPage();

    // javascript.wq를 통한 다양한 경로 시도
    String[] jsPaths = {
        "/websquare/javascript.wq?q=/com/gcm.js",
        "/websquare/javascript.wq?q=/com/com.Submit.js",
        "/websquare/javascript.wq?q=/com/com.UserInfo.js",
        "/websquare/javascript.wq?q=/com/com.UserRole.js",
        "/websquare/javascript.wq?q=/com/com.Main.js",
        "/websquare/javascript.wq?q=/main/js/com/gcm.js",
        "/websquare/javascript.wq?q=/main/js/com/com.UserInfo.js",
        "/websquare/ext/script.wq?q=/com/com.UserInfo.js",
        "/websquare/ext/script.wq?q=/com/com.UserRole.js"
    };

    for (String jsPath : jsPaths) {
      String url = "https://sjpt.sejong.ac.kr" + jsPath;
      log.info("\n>>> 시도: {}", url);

      Request request = new Request.Builder()
          .url(url)
          .get()
          .header("Accept", "*/*")
          .header("Referer", "https://sjpt.sejong.ac.kr/main/common_layout.xml")
          .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
          .build();

      try (Response response = client.newCall(request).execute()) {
        String content = response.body() != null ? response.body().string() : "";
        log.info("  상태: {}, 크기: {} 바이트", response.code(), content.length());

        if (response.code() == 200 && content.length() > 100 && !content.contains("<html")) {
          log.info("  ✅ JS 코드 발견!");

          // pgmRole 검색
          if (content.contains("pgmRole") || content.contains("runPgmRole")) {
            log.info("  ✅✅ pgmRole 관련 코드 발견!");
            printContextAroundKeyword(content, "pgmRole", 300);
          }

          // 내용 일부 출력
          log.info("  내용 (처음 1000자):\n{}", content.substring(0, Math.min(1000, content.length())));
        }
      } catch (Exception e) {
        log.info("  에러: {}", e.getMessage());
      }
    }
  }

  /**
   * WebSquare 엔진의 JavaScript 번들 다운로드 시도
   *
   * WebSquare는 여러 JS 파일들을 번들로 제공할 수 있음
   */
  @Test
  void WebSquare_엔진_JS_다운로드() throws IOException {
    log.info("============ WebSquare 엔진 JavaScript 다운로드 ============");

    // 로그인 + SSO
    doPortalLogin();
    accessSjptSsoPage();

    // 가능한 WebSquare JS 경로들 시도
    String[] possiblePaths = {
        "/websquare/engine/websquare.js",
        "/websquare/websquare.js",
        "/websquare/javascript.wq?q=/bootloader",
        "/websquare/javascript.wq?q=/websquare",
        "/websquare/wsext/websquare_ext.js",
        "/main/js/websquare.js",
        "/main/js/common.js",
        "/main/js/gcm.js"
    };

    for (String path : possiblePaths) {
      String url = "https://sjpt.sejong.ac.kr" + path;
      log.info("\n>>> 시도: {}", url);

      Request request = new Request.Builder()
          .url(url)
          .get()
          .header("Accept", "*/*")
          .header("Referer", "https://sjpt.sejong.ac.kr/main/common_layout.xml")
          .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
          .build();

      try (Response response = client.newCall(request).execute()) {
        String content = response.body() != null ? response.body().string() : "";
        log.info("  상태: {}, 크기: {} 바이트", response.code(), content.length());

        if (response.code() == 200 && content.length() > 500) {
          // pgmRole 검색
          if (content.contains("pgmRole") || content.contains("runPgmRole")) {
            log.info("  ✅ pgmRole 관련 코드 발견!");
            printContextAroundKeyword(content, "pgmRole", 200);
          }

          // 너무 길면 처음 2000자만 출력
          if (content.length() > 2000) {
            log.info("  내용 (처음 2000자):\n{}", content.substring(0, 2000));
          } else {
            log.info("  내용:\n{}", content);
          }
        }
      } catch (Exception e) {
        log.info("  에러: {}", e.getMessage());
      }
    }
  }

  /**
   * WebSquare JS 파일 다운로드 헬퍼
   */
  private String downloadWebSquareJsFile(String jsPath) throws IOException {
    String fullUrl = "https://sjpt.sejong.ac.kr" + jsPath;

    Request request = new Request.Builder()
        .url(fullUrl)
        .get()
        .header("Accept", "*/*")
        .header("Referer", "https://sjpt.sejong.ac.kr/main/common_layout.xml")
        .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        .build();

    try (Response response = client.newCall(request).execute()) {
      String content = response.body() != null ? response.body().string() : "";
      log.info("{} - 상태: {}, 크기: {} 바이트", jsPath, response.code(), content.length());

      if (response.code() == 200) {
        return content;
      } else {
        return null;
      }
    }
  }

  /**
   * JS 내용에서 pgmRole 관련 코드 분석
   */
  private void analyzePgmRoleInJsContent(String fileName, String content) {
    log.info("\n>>> pgmRole 관련 코드 분석: {}", fileName);

    // pgmRole 관련 키워드
    String[] pgmRoleKeywords = {
        "pgmRole", "PGM_ROLE", "runPgmRole", "RuningPgmRole",
        "ROLE", "role", "setRole", "getRole", "menuRole"
    };

    for (String keyword : pgmRoleKeywords) {
      if (content.contains(keyword)) {
        log.info("✅ '{}' 발견!", keyword);
        printContextAroundKeyword(content, keyword, 150);
      }
    }

    // 세션/프로그램 키 관련
    String[] sessionKeywords = {
        "_runPgmKey", "_runningSejong", "addParam", "RUNNING_SEJONG"
    };

    for (String keyword : sessionKeywords) {
      if (content.contains(keyword)) {
        log.info("✅ 세션 키워드 '{}' 발견!", keyword);
      }
    }

    // API 호출 관련
    if (content.contains(".do")) {
      log.info("✅ .do API 호출 발견 - 서버 API 호출 코드 있음");
      // API URL 패턴 추출
      java.util.regex.Pattern apiPattern = java.util.regex.Pattern.compile("/[a-zA-Z/]+\\.do");
      java.util.regex.Matcher matcher = apiPattern.matcher(content);
      java.util.Set<String> apis = new java.util.HashSet<>();
      while (matcher.find()) {
        apis.add(matcher.group());
      }
      if (!apis.isEmpty()) {
        log.info(">>> 발견된 API 엔드포인트:");
        apis.forEach(api -> log.info("    - {}", api));
      }
    }
  }

  /**
   * 키워드 주변 컨텍스트 출력
   */
  private void printContextAroundKeyword(String content, String keyword, int contextLength) {
    int idx = 0;
    int count = 0;
    while ((idx = content.indexOf(keyword, idx)) != -1 && count < 3) {
      count++;
      int start = Math.max(0, idx - contextLength);
      int end = Math.min(content.length(), idx + keyword.length() + contextLength);
      log.info("  [#{} 위치 {}] ...{}...", count, idx, content.substring(start, end).replace("\n", " ").replace("\r", ""));
      idx++;
    }
    if (count == 0) {
      log.info("  (컨텍스트 없음)");
    }
  }

  // ==================== Helper Methods ====================

  /**
   * 포털 로그인 수행
   */
  private String doPortalLogin() throws IOException {
    // rtUrl을 sjpt로 설정
    RequestBody formBody = new FormBody.Builder()
        .add("mainLogin", "N")
        .add("rtUrl", "sjpt.sejong.ac.kr/main/view/Login/doSsoLogin.do?p=")
        .add("id", testStudentId)
        .add("password", testPassword)
        .build();

    Request request = new Request.Builder()
        .url(PORTAL_LOGIN_URL)
        .post(formBody)
        .header("Host", "portal.sejong.ac.kr")
        .header("Referer", "https://portal.sejong.ac.kr")
        .header("Cookie", "chknos=false")
        .build();

    try (Response response = client.newCall(request).execute()) {
      String body = response.body() != null ? response.body().string() : "";
      log.info("포털 로그인 - 상태: {}, 응답길이: {}", response.code(), body.length());
      return body;
    }
  }

  /**
   * sjpt SSO 페이지 접근
   */
  private String accessSjptSsoPage() throws IOException {
    Request request = new Request.Builder()
        .url(SJPT_SSO_URL)
        .get()
        .header("Referer", "https://portal.sejong.ac.kr")
        .build();

    try (Response response = client.newCall(request).execute()) {
      String body = response.body() != null ? response.body().string() : "";
      log.info("SSO 페이지 - 상태: {}, 응답길이: {}", response.code(), body.length());
      return body;
    }
  }

  /**
   * 비밀번호 재확인 (doCheck.do)
   */
  private String doPasswordCheck() throws IOException {
    // addParam 생성
    String addParam = generateAddParam();
    log.info("생성된 addParam: {}", addParam);

    // JSON payload 생성
    String jsonPayload = String.format(
        "{\"dm_search\":{\"STUDENT_NO\":\"\",\"PASSWORD\":\"%s\"}}",
        testPassword
    );

    RequestBody body = RequestBody.create(
        jsonPayload,
        MediaType.parse("application/json; charset=UTF-8")
    );

    String urlWithParam = SJPT_CHECK_URL + "?addParam=" + addParam;

    Request request = new Request.Builder()
        .url(urlWithParam)
        .post(body)
        .header("Accept", "application/json")
        .header("Content-Type", "application/json; charset=UTF-8")
        .header("Referer", SJPT_SSO_URL)
        .header("Origin", "https://sjpt.sejong.ac.kr")
        .build();

    try (Response response = client.newCall(request).execute()) {
      String responseBody = response.body() != null ? response.body().string() : "";
      log.info("doCheck - 상태: {}, 응답길이: {}", response.code(), responseBody.length());
      return responseBody;
    }
  }

  /**
   * addParam 생성 (Base64 + URL 인코딩)
   */
  private String generateAddParam() {
    // 현재 시간 생성
    String loginDt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    // UUID 생성
    String sessionId = UUID.randomUUID().toString();

    // JSON 생성
    String json = String.format(
        "{\"_runPgmKey\":\"SELF_STUDSELF_SUB_20SELF_MENU_10SudMasterBodyE\"," +
            "\"_runSysKey\":\"SCH\"," +
            "\"_runIntgUsrNo\":\"%s\"," +
            "\"_runPgLoginDt\":\"%s\"," +
            "\"_runningSejong\":\"%s\"}",
        testStudentId, loginDt, sessionId
    );

    log.info("addParam JSON: {}", json);

    // URL 인코딩 후 Base64 인코딩
    String urlEncoded = URLEncoder.encode(json, StandardCharsets.UTF_8);
    return Base64.getEncoder().encodeToString(urlEncoded.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * 학적정보 조회 (doList.do)
   */
  private String doFetchStudentInfo() throws IOException {
    // addParam 생성
    String addParam = generateAddParam();

    // JSON payload 생성
    String jsonPayload = String.format(
        "{\"dm_search\":{\"ORGN_CLSF_CD\":\"20\",\"STUDENT_NO\":\"%s\",\"BRANCH\":\"\",\"GDT_JUDGE_CD\":\"\",\"GDT_YEAR\":\"\",\"GDT_SMT_CD\":\"\",\"SMT_CD\":\"\",\"TAB_NO\":\"\",\"YEAR\":\"\"}}",
        testStudentId
    );

    RequestBody body = RequestBody.create(
        jsonPayload,
        MediaType.parse("application/json; charset=UTF-8")
    );

    String urlWithParam = SJPT_LIST_URL + "?addParam=" + addParam;

    Request request = new Request.Builder()
        .url(urlWithParam)
        .post(body)
        .header("Accept", "application/json")
        .header("Content-Type", "application/json; charset=UTF-8")
        .header("Referer", SJPT_SSO_URL)
        .header("Origin", "https://sjpt.sejong.ac.kr")
        .build();

    try (Response response = client.newCall(request).execute()) {
      String responseBody = response.body() != null ? response.body().string() : "";
      log.info("doList - 상태: {}, 응답길이: {}", response.code(), responseBody.length());
      return responseBody;
    }
  }

  /**
   * 로깅 인터셉터가 포함된 OkHttp 클라이언트 생성
   */
  private OkHttpClient buildClientWithLogging() {
    try {
      OkHttpClient.Builder builder = new OkHttpClient.Builder();

      // 타임아웃 설정
      builder.connectTimeout(30, TimeUnit.SECONDS);
      builder.readTimeout(30, TimeUnit.SECONDS);
      builder.writeTimeout(30, TimeUnit.SECONDS);

      // 쿠키 관리
      CookieManager cookieManager = new CookieManager();
      cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
      builder.cookieJar(new JavaNetCookieJar(cookieManager));

      // 리다이렉트 따라가기
      builder.followRedirects(true);
      builder.followSslRedirects(true);

      // SSL 검증 비활성화
      SSLContext sslContext = SSLContext.getInstance("SSL");
      X509TrustManager trustManager = new X509TrustManager() {
        @Override
        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
        @Override
        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
          return new java.security.cert.X509Certificate[0];
        }
      };
      sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());
      builder.sslSocketFactory(sslContext.getSocketFactory(), trustManager);
      builder.hostnameVerifier((hostname, session) -> true);

      // 로깅 인터셉터 추가
      builder.addInterceptor(chain -> {
        Request request = chain.request();
        long startTime = System.currentTimeMillis();

        log.info("───────────────────────────────────────");
        log.info(">>> REQUEST: {} {}", request.method(), request.url());
        log.info(">>> Headers:");
        request.headers().forEach(pair ->
            log.info("    {}: {}", pair.getFirst(), pair.getSecond()));

        Response response = chain.proceed(request);
        long duration = System.currentTimeMillis() - startTime;

        log.info("<<< RESPONSE: {} {} ({}ms)", response.code(), response.message(), duration);
        log.info("<<< Headers:");
        response.headers().forEach(pair ->
            log.info("    {}: {}", pair.getFirst(), pair.getSecond()));
        log.info("───────────────────────────────────────");

        return response;
      });

      return builder.build();

    } catch (Exception e) {
      throw new RuntimeException("Failed to create OkHttpClient", e);
    }
  }
}
