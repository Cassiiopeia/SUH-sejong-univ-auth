package kr.suhsaechan.sejong.auth.client;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import kr.suhsaechan.sejong.auth.config.SejongAuthProperties;
import kr.suhsaechan.sejong.auth.exception.SejongAuthErrorCode;
import kr.suhsaechan.sejong.auth.exception.SejongAuthException;
import kr.suhsaechan.sejong.auth.util.SejongTlsSupport;
import lombok.extern.slf4j.Slf4j;
import okhttp3.FormBody;
import okhttp3.JavaNetCookieJar;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 세종대학교 학사정보시스템(SIS) HTTP 클라이언트
 * - sjpt.sejong.ac.kr에서 학생 연락처 정보 조회
 */
@Slf4j
public class SejongSisClient {

  // HTTP 요청 관련 상수
  private static final String PORTAL_HOST = "portal.sejong.ac.kr";
  private static final String PORTAL_REFERER = "https://portal.sejong.ac.kr";
  private static final String PORTAL_LOGIN_URL = "https://portal.sejong.ac.kr/jsp/login/login_action.jsp";
  private static final String DEFAULT_COOKIE = "chknos=false";

  private static final String SJPT_SSO_URL = "https://sjpt.sejong.ac.kr/main/view/Login/doSsoLogin.do?p=";
  private static final String SJPT_INIT_USER_INFO_URL = "https://sjpt.sejong.ac.kr/main/sys/UserInfo/initUserInfo.do";

  private final SejongAuthProperties properties;

  public SejongSisClient(SejongAuthProperties properties) {
    this.properties = properties;
  }

  /**
   * 세종포털 로그인 후 학사정보시스템에서 사용자 정보 JSON 반환
   *
   * @param studentId 학번
   * @param password 비밀번호
   * @return initUserInfo.do API 응답 JSON
   * @throws SejongAuthException 인증 실패 시
   */
  public String authenticateAndFetchJson(String studentId, String password) {
    try {
      OkHttpClient client = buildClient();

      // 1. 포털 로그인 (sjpt로 리다이렉트 설정)
      doPortalLogin(client, studentId, password);

      // 2. SSO 페이지 접근
      accessSsoPage(client);

      // 3. initUserInfo.do API 호출
      return fetchInitUserInfo(client);

    } catch (SejongAuthException e) {
      throw e;
    } catch (IOException e) {
      log.error("[sejong auth] SIS authentication IOException : {}", e.getMessage());
      throw new SejongAuthException(SejongAuthErrorCode.CONNECTION_FAILED, e);
    } catch (Exception e) {
      log.error("[sejong auth] SIS authentication error : {}", e.getMessage());
      throw new SejongAuthException(SejongAuthErrorCode.CONNECTION_FAILED, e);
    }
  }

  /**
   * 세종포털 로그인 요청 (sjpt로 리다이렉트)
   */
  private void doPortalLogin(OkHttpClient client, String studentId, String password) throws IOException {
    RequestBody formBody = new FormBody.Builder()
        .add("mainLogin", "N")
        .add("rtUrl", "sjpt.sejong.ac.kr/main/view/Login/doSsoLogin.do?p=")
        .add("id", studentId)
        .add("password", password)
        .build();

    Request request = new Request.Builder()
        .url(PORTAL_LOGIN_URL)
        .post(formBody)
        .header("Host", PORTAL_HOST)
        .header("Referer", PORTAL_REFERER)
        .header("Cookie", DEFAULT_COOKIE)
        .build();

    log.debug("[sejong auth] portal login request (SIS) : url={}, studentId={}", PORTAL_LOGIN_URL, studentId);

    try (Response response = executeWithRetry(client, request)) {
      String responseBody = response.body() != null ? response.body().string() : "";
      log.debug("[sejong auth] portal login response : code={}, bodyLength={}", response.code(), responseBody.length());
    }
  }

  /**
   * SSO 페이지 접근
   */
  private void accessSsoPage(OkHttpClient client) throws IOException {
    Request request = new Request.Builder()
        .url(SJPT_SSO_URL)
        .get()
        .header("Referer", PORTAL_REFERER)
        .build();

    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new SejongAuthException(SejongAuthErrorCode.SESSION_ERROR,
            "SIS SSO page request failed : " + response.code());
      }
      log.debug("[sejong auth] SIS SSO page loaded : code={}", response.code());
    }
  }

  /**
   * initUserInfo.do API 호출
   * - 401 응답: 로그인 실패 (학번/비밀번호 불일치)
   */
  private String fetchInitUserInfo(OkHttpClient client) throws IOException {
    String addParam = generateEmptyAddParam();
    String apiUrl = SJPT_INIT_USER_INFO_URL + "?addParam=" + addParam;

    RequestBody body = RequestBody.create(
        "{}",
        MediaType.parse("application/json; charset=UTF-8")
    );

    Request request = new Request.Builder()
        .url(apiUrl)
        .post(body)
        .header("Accept", "application/json")
        .header("Content-Type", "application/json; charset=UTF-8")
        .header("Referer", SJPT_SSO_URL)
        .header("Origin", "https://sjpt.sejong.ac.kr")
        .build();

    try (Response response = client.newCall(request).execute()) {
      int responseCode = response.code();

      // 401 Unauthorized: 로그인 실패
      if (responseCode == 401) {
        log.warn("[sejong auth] SIS authentication rejected 401 : unauthorized");
        throw new SejongAuthException(SejongAuthErrorCode.AUTHENTICATION_FAILED);
      }

      if (response.body() == null || responseCode != 200) {
        throw new SejongAuthException(SejongAuthErrorCode.DATA_FETCH_FAILED,
            "initUserInfo API request failed : " + responseCode);
      }

      String json = response.body().string();
      log.debug("[sejong auth] initUserInfo response : code={}, jsonLength={}", responseCode, json.length());
      return json;
    }
  }

  /**
   * 빈 addParam 생성 (브라우저 초기 호출 시 사용)
   */
  private String generateEmptyAddParam() {
    String json = "{\"_runIntgUsrNo\":\"\",\"_runPgLoginDt\":\"\",\"_runningSejong\":\"\"}";
    String urlEncoded = URLEncoder.encode(json, StandardCharsets.UTF_8);
    return Base64.getEncoder().encodeToString(urlEncoded.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * 재시도 로직이 포함된 요청 실행
   */
  private Response executeWithRetry(OkHttpClient client, Request request) throws IOException {
    Response response = null;
    int tryCount = 0;
    int maxRetry = properties.getMaxRetry();

    while (tryCount < maxRetry) {
      try {
        response = client.newCall(request).execute();
        if (response.isSuccessful()) {
          return response;
        }
        tryCount++;
        log.warn("[sejong auth] request failed, retrying (attempt {})", tryCount);
      } catch (SocketTimeoutException e) {
        tryCount++;
        log.warn("[sejong auth] timeout, retrying (attempt {})", tryCount);
        if (tryCount >= maxRetry) {
          throw new SejongAuthException(SejongAuthErrorCode.CONNECTION_TIMEOUT, e);
        }
      }
    }

    throw new SejongAuthException(SejongAuthErrorCode.CONNECTION_FAILED,
        "Max retry count exceeded : " + maxRetry);
  }

  /**
   * OkHttpClient 생성
   */
  private OkHttpClient buildClient() {
    try {
      // 세종포털은 레거시 cipher(TLS_RSA_WITH_AES_256_CBC_SHA)만 지원 → JSSE 초기화 전에 재허용 보장
      SejongTlsSupport.ensureLegacyCipherEnabled();

      OkHttpClient.Builder builder = new OkHttpClient.Builder();

      // 타임아웃 설정
      int timeout = properties.getTimeoutSeconds();
      builder.connectTimeout(timeout, TimeUnit.SECONDS);
      builder.readTimeout(timeout, TimeUnit.SECONDS);
      builder.writeTimeout(timeout, TimeUnit.SECONDS);

      // 쿠키 관리
      CookieManager cookieManager = new CookieManager();
      cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
      builder.cookieJar(new JavaNetCookieJar(cookieManager));

      // SSL 검증 비활성화 (설정에 따라)
      if (!properties.isSslVerification()) {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        X509TrustManager trustManager = createTrustAllManager();
        sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());
        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        builder.sslSocketFactory(sslSocketFactory, trustManager);
        builder.hostnameVerifier(createTrustAllHostnameVerifier());

        log.debug("[sejong auth] SSL verification disabled (SIS)");
      }

      return builder.build();

    } catch (Exception e) {
      throw new SejongAuthException(SejongAuthErrorCode.SSL_CONFIGURATION_ERROR, e);
    }
  }

  /**
   * 모든 인증서를 신뢰하는 TrustManager 생성
   */
  private X509TrustManager createTrustAllManager() {
    return new X509TrustManager() {
      @Override
      public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
      }

      @Override
      public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
      }

      @Override
      public java.security.cert.X509Certificate[] getAcceptedIssuers() {
        return new java.security.cert.X509Certificate[0];
      }
    };
  }

  /**
   * 모든 호스트를 신뢰하는 HostnameVerifier 생성
   */
  private HostnameVerifier createTrustAllHostnameVerifier() {
    return (hostname, session) -> true;
  }
}
