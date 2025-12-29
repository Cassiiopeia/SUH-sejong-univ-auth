package kr.suhsaechan.sejong.auth.client;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.SocketTimeoutException;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import kr.suhsaechan.sejong.auth.config.SejongAuthProperties;
import kr.suhsaechan.sejong.auth.exception.SejongAuthErrorCode;
import kr.suhsaechan.sejong.auth.exception.SejongAuthException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.FormBody;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 세종대학교 포털 HTTP 클라이언트
 * - 로그인, SSO 리다이렉트, 페이지 요청 담당
 */
@Slf4j
public class SejongPortalClient {

  // HTTP 요청 관련 상수
  private static final String PORTAL_HOST = "portal.sejong.ac.kr";
  private static final String PORTAL_REFERER = "https://portal.sejong.ac.kr";
  private static final String DEFAULT_COOKIE = "chknos=false";

  private final SejongAuthProperties properties;

  public SejongPortalClient(SejongAuthProperties properties) {
    this.properties = properties;
  }

  /**
   * 세종포털 로그인 후 고전독서인증현황 페이지 HTML 반환
   *
   * @param studentId 학번
   * @param password 비밀번호
   * @return 고전독서인증현황 페이지 HTML
   * @throws SejongAuthException 인증 실패 시
   */
  public String authenticateAndFetchHtml(String studentId, String password) {
    try {
      OkHttpClient client = buildClient();

      // 1. 포털 로그인
      doPortalLogin(client, studentId, password);

      // 2. SSO 리다이렉트
      handleSsoRedirect(client);

      // 3. 고전독서인증현황 페이지 HTML 반환
      return fetchClassicStatusPage(client);

    } catch (SejongAuthException e) {
      throw e;
    } catch (IOException e) {
      log.error("세종포털 인증 중 IOException: {}", e.getMessage());
      throw new SejongAuthException(SejongAuthErrorCode.CONNECTION_FAILED, e);
    } catch (Exception e) {
      log.error("세종포털 인증 중 예외 발생: {}", e.getMessage());
      throw new SejongAuthException(SejongAuthErrorCode.CONNECTION_FAILED, e);
    }
  }

  /**
   * 세종포털 로그인 요청
   */
  private void doPortalLogin(OkHttpClient client, String studentId, String password) throws IOException {
    // rtUrl을 classic.sejong.ac.kr로 변경 (고전독서 사이트로 리다이렉트)
    RequestBody formBody = new FormBody.Builder()
        .add("mainLogin", "N")
        .add("rtUrl", "classic.sejong.ac.kr")
        .add("id", studentId)
        .add("password", password)
        .build();

    Request request = new Request.Builder()
        .url(properties.getPortalLoginUrl())
        .post(formBody)
        .header("Host", PORTAL_HOST)
        .header("Referer", PORTAL_REFERER)
        .header("Cookie", DEFAULT_COOKIE)
        .build();

    log.debug("세종포털 로그인 요청: URL={}, studentId={}", properties.getPortalLoginUrl(), studentId);

    try (Response response = executeWithRetry(client, request)) {
      // 응답 바디 읽기 (원본 Malsami 코드와 동일하게 로그인 실패 체크 제거)
      // 로그인 성공/실패 여부는 이후 SSO 및 데이터 페이지 요청에서 간접적으로 확인됨
      String responseBody = response.body() != null ? response.body().string() : "";

      log.debug("세종포털 로그인 응답: code={}, bodyLength={}", response.code(), responseBody.length());
      log.debug("세종포털 로그인 요청 완료: 응답코드={}", response.code());
    }
  }

  /**
   * SSO 리다이렉트 처리
   */
  private void handleSsoRedirect(OkHttpClient client) throws IOException {
    Request ssoRequest = new Request.Builder()
        .url(properties.getSsoRedirectUrl())
        .get()
        .build();

    try (Response ssoResponse = client.newCall(ssoRequest).execute()) {
      if (!ssoResponse.isSuccessful()) {
        throw new SejongAuthException(SejongAuthErrorCode.SESSION_ERROR,
            "SSO 리다이렉트 실패: " + ssoResponse.code());
      }
      log.debug("SSO 리다이렉트 완료: 응답코드={}", ssoResponse.code());
    }
  }

  /**
   * 고전독서인증현황 페이지 HTML 가져오기
   * - 401 응답: 로그인 실패 (학번/비밀번호 불일치)
   * - 그 외 실패: 데이터 조회 실패
   */
  private String fetchClassicStatusPage(OkHttpClient client) throws IOException {
    Request request = new Request.Builder()
        .url(properties.getClassicStatusUrl())
        .get()
        .build();

    try (Response response = client.newCall(request).execute()) {
      int responseCode = response.code();

      // 401 Unauthorized: 로그인 실패 (학번/비밀번호 불일치)
      if (responseCode == 401) {
        log.warn("세종포털 인증 실패: 401 Unauthorized - 학번 또는 비밀번호가 일치하지 않습니다.");
        throw new SejongAuthException(SejongAuthErrorCode.AUTHENTICATION_FAILED);
      }

      // 그 외 실패 응답
      if (response.body() == null || responseCode != 200) {
        throw new SejongAuthException(SejongAuthErrorCode.DATA_FETCH_FAILED,
            "고전독서 페이지 요청 실패: " + responseCode);
      }

      String html = response.body().string();
      log.debug("고전독서 페이지 요청 완료: 응답코드={}, HTML길이={}", responseCode, html.length());
      return html;
    }
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
        log.warn("요청 실패, 재시도 중... ({}회)", tryCount);
      } catch (SocketTimeoutException e) {
        tryCount++;
        log.warn("타임아웃 발생, 재시도 중... ({}회)", tryCount);
        if (tryCount >= maxRetry) {
          throw new SejongAuthException(SejongAuthErrorCode.CONNECTION_TIMEOUT, e);
        }
      }
    }

    throw new SejongAuthException(SejongAuthErrorCode.CONNECTION_FAILED,
        "최대 재시도 횟수 초과: " + maxRetry);
  }

  /**
   * OkHttpClient 생성
   * - SSL 검증 설정 적용
   * - 쿠키 자동 관리
   * - 타임아웃 설정
   */
  private OkHttpClient buildClient() {
    try {
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
        SSLContext sslContext = SSLContext.getInstance("SSL");
        X509TrustManager trustManager = createTrustAllManager();
        sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());
        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        builder.sslSocketFactory(sslSocketFactory, trustManager);
        builder.hostnameVerifier(createTrustAllHostnameVerifier());

        log.debug("SSL 검증 비활성화됨");
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
