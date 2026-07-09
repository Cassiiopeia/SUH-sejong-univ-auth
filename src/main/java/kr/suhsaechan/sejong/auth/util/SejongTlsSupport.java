package kr.suhsaechan.sejong.auth.util;

import java.security.Security;
import lombok.extern.slf4j.Slf4j;

/**
 * 세종대 포털 TLS 호환 지원 유틸리티
 *
 * <p>세종대 포털(portal/classic/sjpt.sejong.ac.kr)은 TLS 1.2 + {@code TLS_RSA_WITH_AES_256_CBC_SHA}
 * (openssl 표기 {@code AES256-SHA}) cipher suite만 제공한다. 그런데 일부 JDK 17 배포판(예: Temurin
 * 17.0.19)부터 {@code jdk.tls.disabledAlgorithms} 기본값에 {@code TLS_RSA_*} 가 추가되어, 해당 cipher가
 * JSSE의 지원 목록에서 제외된다. 그 결과 세종대 서버와 공통 cipher가 없어
 * {@code Received fatal alert: handshake_failure} 로 모든 인증이 실패한다.
 * (wget/openssl 같은 OS TLS 스택은 이 제약이 없어 정상 연결되므로 진단이 헷갈릴 수 있다.)
 *
 * <p>해결: {@code jdk.tls.disabledAlgorithms} 에서 {@code TLS_RSA_*} 만 제거해 해당 cipher를 다시
 * 허용한다. 이 property는 JSSE가 SSLContext를 최초 초기화할 때 한 번만 읽어 캐싱하므로, 반드시
 * <b>어떤 SSL 클래스도 로드되기 전에</b> 바꿔야 한다. 그래서 static 초기화 블록에 두고, 각 HTTP
 * 클라이언트가 소켓을 만들기 전에 {@link #ensureLegacyCipherEnabled()} 를 호출해 이 클래스가 먼저
 * 로드되도록 강제한다.
 *
 * <p>보안 참고: 이 조정은 JVM 전역으로 {@code TLS_RSA_*} cipher를 재허용한다. 세종대 서버가 이 cipher
 * 만 지원하므로 인증 기능을 위해서는 불가피한 트레이드오프다.
 */
@Slf4j
public final class SejongTlsSupport {

  private static final String DISABLED_ALGORITHMS_KEY = "jdk.tls.disabledAlgorithms";
  private static final boolean APPLIED = removeLegacyRsaFromDisabledAlgorithms();

  private SejongTlsSupport() {
  }

  /**
   * 세종대 포털이 요구하는 레거시 RSA cipher가 활성화되도록 보장한다.
   * 실제 작업은 클래스 로드 시점의 static 초기화에서 1회 수행되며, 이 메서드는 그 클래스 로딩을
   * 소켓 생성 이전으로 앞당기기 위한 트리거다.
   */
  public static void ensureLegacyCipherEnabled() {
    // static 초기화(APPLIED) 실행을 보장하기 위한 no-op 트리거
    if (!APPLIED) {
      log.debug("세종대 TLS 레거시 cipher 재허용이 이미 적용되어 있거나 불필요합니다.");
    }
  }

  private static boolean removeLegacyRsaFromDisabledAlgorithms() {
    String disabled = Security.getProperty(DISABLED_ALGORITHMS_KEY);
    if (disabled == null || !disabled.toUpperCase().contains("TLS_RSA_")) {
      return false;
    }

    // "TLS_RSA_*" 항목만 제거 (앞뒤 콤마·공백 정리)
    String cleaned = disabled.replaceAll("(?i)\\s*,?\\s*TLS_RSA_\\*", "").replaceAll("^\\s*,\\s*", "").trim();
    Security.setProperty(DISABLED_ALGORITHMS_KEY, cleaned);
    log.info("세종대 포털 호환을 위해 TLS_RSA_* cipher 재허용 적용 (jdk.tls.disabledAlgorithms 조정)");
    return true;
  }
}
