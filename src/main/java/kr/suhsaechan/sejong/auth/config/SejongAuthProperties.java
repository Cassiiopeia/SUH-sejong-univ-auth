package kr.suhsaechan.sejong.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 세종대학교 인증 설정 프로퍼티
 *
 * application.yml 예시:
 * sejong:
 *   auth:
 *     ssl-verification: false
 *     timeout-seconds: 10
 *     max-retry: 3
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "sejong.auth")
public class SejongAuthProperties {

  /** SSL 인증서 검증 활성화 여부 (기본: true) */
  private boolean sslVerification = true;

  /** HTTP 요청 타임아웃 (초, 기본: 10) */
  private int timeoutSeconds = 10;

  /** 요청 실패 시 최대 재시도 횟수 (기본: 3) */
  private int maxRetry = 3;

  /** 세종포털 로그인 URL */
  private String portalLoginUrl = "https://portal.sejong.ac.kr/jsp/login/login_action.jsp";

  /** SSO 리다이렉트 URL */
  private String ssoRedirectUrl = "http://classic.sejong.ac.kr/_custom/sejong/sso/sso-return.jsp?returnUrl=https://classic.sejong.ac.kr/classic/index.do";

  /** 고전독서인증현황 페이지 URL */
  private String classicStatusUrl = "https://classic.sejong.ac.kr/classic/reading/status.do";

  /** 테스트용 설정 (dev 프로파일에서만 사용) */
  private TestConfig test = new TestConfig();

  /**
   * 테스트용 설정 내부 클래스
   *
   * application-dev.yml 예시:
   * sejong:
   *   auth:
   *     test:
   *       student-id: "학번"
   *       password: "비밀번호"
   */
  @Getter
  @Setter
  public static class TestConfig {
    /** 테스트용 학번 */
    private String studentId;

    /** 테스트용 비밀번호 */
    private String password;
  }
}
