package kr.suhsaechan.sejong.auth.client;

import java.io.IOException;
import kr.suhsaechan.sejong.auth.config.SejongAuthProperties;
import kr.suhsaechan.sejong.auth.exception.SejongAuthErrorCode;
import kr.suhsaechan.sejong.auth.exception.SejongAuthException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 세종대학교 포털 HTTP 클라이언트
 * - 로그인, SSO 리다이렉트, 페이지 요청 담당
 */
@Slf4j
public class SejongPortalClient {

  // 포털 로그인 후 SSO 대상: 고전독서 사이트
  private static final String CLASSIC_RETURN_URL = "classic.sejong.ac.kr";

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
      OkHttpClient client = SejongHttpSupport.buildClient(properties);

      // 1. 포털 로그인
      SejongHttpSupport.loginToPortal(client, properties, studentId, password, CLASSIC_RETURN_URL);

      // 2. SSO 리다이렉트
      handleSsoRedirect(client);

      // 3. 고전독서인증현황 페이지 HTML 반환
      return fetchClassicStatusPage(client);

    } catch (SejongAuthException e) {
      throw e;
    } catch (IOException e) {
      log.error("[sejong auth] portal authentication IOException : {}", e.getMessage());
      throw new SejongAuthException(SejongAuthErrorCode.CONNECTION_FAILED, e);
    } catch (Exception e) {
      log.error("[sejong auth] portal authentication error : {}", e.getMessage());
      throw new SejongAuthException(SejongAuthErrorCode.CONNECTION_FAILED, e);
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
            "SSO redirect failed : " + ssoResponse.code());
      }
      log.debug("[sejong auth] SSO redirect done : code={}", ssoResponse.code());
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
        log.warn("[sejong auth] portal authentication rejected 401 : student ID or password mismatch");
        throw new SejongAuthException(SejongAuthErrorCode.AUTHENTICATION_FAILED);
      }

      // 그 외 실패 응답
      if (response.body() == null || responseCode != 200) {
        throw new SejongAuthException(SejongAuthErrorCode.DATA_FETCH_FAILED,
            "Classic reading page request failed : " + responseCode);
      }

      String html = response.body().string();
      log.debug("[sejong auth] classic reading page loaded : code={}, htmlLength={}", responseCode, html.length());
      return html;
    }
  }
}
