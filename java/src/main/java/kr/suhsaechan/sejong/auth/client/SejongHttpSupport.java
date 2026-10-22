package kr.suhsaechan.sejong.auth.client;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.SocketTimeoutException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import kr.suhsaechan.sejong.auth.config.SejongAuthProperties;
import kr.suhsaechan.sejong.auth.exception.SejongAuthErrorCode;
import kr.suhsaechan.sejong.auth.exception.SejongAuthException;
import kr.suhsaechan.sejong.auth.util.SejongTlsSupport;
import lombok.extern.slf4j.Slf4j;
import okhttp3.FormBody;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 세종 포털 HTTP 공통: 클라이언트 생성, 포털 로그인, 재시도 실행
 * DHC·SIS 클라이언트가 공유 (로그인 폼·쿠키·TLS 설정 단일 원본)
 */
@Slf4j
final class SejongHttpSupport {

  // 포털 로그인 요청 헤더
  private static final String PORTAL_HOST = "portal.sejong.ac.kr";
  static final String PORTAL_REFERER = "https://portal.sejong.ac.kr";
  // 포털이 요구하는 초기 쿠키 (없으면 로그인 폼 거부)
  private static final String DEFAULT_COOKIE = "chknos=false";

  private SejongHttpSupport() {
  }

  /**
   * OkHttpClient 생성: 레거시 cipher 보장 > 타임아웃 > 쿠키 저장 > (설정 시) SSL 검증 해제
   */
  static OkHttpClient buildClient(SejongAuthProperties properties) {
    try {
      // 세종포털은 레거시 cipher(TLS_RSA_WITH_AES_256_CBC_SHA)만 지원 → JSSE 초기화 전에 재허용 보장
      SejongTlsSupport.ensureLegacyCipherEnabled();

      int timeoutSeconds = properties.getTimeoutSeconds();
      CookieManager cookieManager = new CookieManager();
      cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

      OkHttpClient.Builder builder = new OkHttpClient.Builder()
          .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
          .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
          .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
          .cookieJar(new JavaNetCookieJar(cookieManager));

      if (!properties.isSslVerification()) {
        X509TrustManager trustAllManager = createTrustAllManager();
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{trustAllManager}, new SecureRandom());
        builder.sslSocketFactory(sslContext.getSocketFactory(), trustAllManager);
        builder.hostnameVerifier((hostname, session) -> true);
        log.debug("[sejong auth] SSL verification disabled");
      }

      return builder.build();
    } catch (Exception e) {
      throw new SejongAuthException(SejongAuthErrorCode.SSL_CONFIGURATION_ERROR, e);
    }
  }

  /**
   * 포털 로그인 (rtUrl: 로그인 후 SSO 로 넘어갈 대상 사이트)
   * 응답 본문으로는 성공 여부를 알 수 없음 > 이후 데이터 요청의 401 로 판별
   */
  static void loginToPortal(OkHttpClient client, SejongAuthProperties properties,
      String studentId, String password, String returnUrl) throws IOException {
    RequestBody formBody = new FormBody.Builder()
        .add("mainLogin", "N")
        .add("rtUrl", returnUrl)
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

    log.debug("[sejong auth] portal login request : rtUrl={}, studentId={}", returnUrl, studentId);
    try (Response response = executeWithRetry(client, request, properties.getMaxRetry())) {
      log.debug("[sejong auth] portal login response : code={}", response.code());
    }
  }

  /**
   * 요청 실행: 실패 응답·타임아웃 시 maxRetry 회까지 재시도 (실패 응답은 닫고 다음 시도)
   */
  static Response executeWithRetry(OkHttpClient client, Request request, int maxRetry) throws IOException {
    for (int attempt = 1; attempt <= maxRetry; attempt++) {
      try {
        Response response = client.newCall(request).execute();
        if (response.isSuccessful()) {
          return response;
        }
        response.close();
        log.warn("[sejong auth] request failed, retrying (attempt {}, code {})", attempt, response.code());
      } catch (SocketTimeoutException e) {
        log.warn("[sejong auth] timeout, retrying (attempt {})", attempt);
        if (attempt == maxRetry) {
          throw new SejongAuthException(SejongAuthErrorCode.CONNECTION_TIMEOUT, e);
        }
      }
    }
    throw new SejongAuthException(SejongAuthErrorCode.CONNECTION_FAILED, "Max retry count exceeded : " + maxRetry);
  }

  /**
   * 모든 인증서 신뢰 (ssl-verification=false 일 때만 사용)
   */
  private static X509TrustManager createTrustAllManager() {
    return new X509TrustManager() {
      @Override
      public void checkClientTrusted(X509Certificate[] chain, String authType) {
      }

      @Override
      public void checkServerTrusted(X509Certificate[] chain, String authType) {
      }

      @Override
      public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
      }
    };
  }
}
