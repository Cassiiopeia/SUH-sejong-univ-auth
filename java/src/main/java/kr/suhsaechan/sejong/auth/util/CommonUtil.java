package kr.suhsaechan.sejong.auth.util;

/**
 * 공통 유틸리티 클래스
 * - null 체크, 기본값 반환, 문자열 처리 등
 */
public final class CommonUtil {

  private CommonUtil() {
    // 유틸리티 클래스 인스턴스화 방지
  }

  /**
   * 문자열이 null이거나 공백만 있는지 확인
   *
   * @param str 검사할 문자열
   * @return null이거나 공백만 있으면 true
   */
  public static boolean isBlank(String str) {
    return str == null || str.trim().isEmpty();
  }

  /**
   * 문자열이 유효한지 확인 (null이 아니고 공백이 아닌 문자 포함)
   *
   * @param str 검사할 문자열
   * @return 유효하면 true
   */
  public static boolean hasText(String str) {
    return str != null && !str.trim().isEmpty();
  }

  /**
   * 문자열이 공백이면 기본값 반환
   *
   * @param str 검사할 문자열
   * @param defaultValue 기본값
   * @return 공백이면 defaultValue, 아니면 str
   */
  public static String defaultIfBlank(String str, String defaultValue) {
    return isBlank(str) ? defaultValue : str;
  }

  /**
   * 문자열 안전하게 trim (null이면 빈 문자열 반환)
   *
   * @param str 대상 문자열
   * @return trim된 문자열 (null이면 빈 문자열)
   */
  public static String trimToEmpty(String str) {
    return str != null ? str.trim() : "";
  }
}
