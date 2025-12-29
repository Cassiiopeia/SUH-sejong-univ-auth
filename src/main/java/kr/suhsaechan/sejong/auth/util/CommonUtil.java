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
   * 문자열이 null이거나 빈 문자열인지 확인
   *
   * @param str 검사할 문자열
   * @return null이거나 빈 문자열이면 true
   */
  public static boolean isEmpty(String str) {
    return str == null || str.isEmpty();
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
   * null이면 기본값 반환
   *
   * @param value 검사할 값
   * @param defaultValue 기본값
   * @param <T> 값의 타입
   * @return value가 null이면 defaultValue, 아니면 value
   */
  public static <T> T defaultIfNull(T value, T defaultValue) {
    return value != null ? value : defaultValue;
  }

  /**
   * 문자열이 빈 값이면 기본값 반환
   *
   * @param str 검사할 문자열
   * @param defaultValue 기본값
   * @return 빈 값이면 defaultValue, 아니면 str
   */
  public static String defaultIfEmpty(String str, String defaultValue) {
    return isEmpty(str) ? defaultValue : str;
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
   * 문자열 안전하게 trim (null이면 null 반환)
   *
   * @param str 대상 문자열
   * @return trim된 문자열 또는 null
   */
  public static String trimSafely(String str) {
    return str != null ? str.trim() : null;
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

  /**
   * 문자열 안전하게 trim (빈 문자열이면 null 반환)
   *
   * @param str 대상 문자열
   * @return trim된 문자열 (빈 문자열이면 null)
   */
  public static String trimToNull(String str) {
    if (str == null) {
      return null;
    }
    String trimmed = str.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  /**
   * 정수를 안전하게 파싱 (실패 시 기본값 반환)
   *
   * @param str 파싱할 문자열
   * @param defaultValue 파싱 실패 시 기본값
   * @return 파싱된 정수 또는 기본값
   */
  public static int parseIntSafely(String str, int defaultValue) {
    if (isBlank(str)) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(str.trim());
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  /**
   * Long을 안전하게 파싱 (실패 시 기본값 반환)
   *
   * @param str 파싱할 문자열
   * @param defaultValue 파싱 실패 시 기본값
   * @return 파싱된 Long 또는 기본값
   */
  public static long parseLongSafely(String str, long defaultValue) {
    if (isBlank(str)) {
      return defaultValue;
    }
    try {
      return Long.parseLong(str.trim());
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  /**
   * Double을 안전하게 파싱 (실패 시 기본값 반환)
   *
   * @param str 파싱할 문자열
   * @param defaultValue 파싱 실패 시 기본값
   * @return 파싱된 Double 또는 기본값
   */
  public static double parseDoubleSafely(String str, double defaultValue) {
    if (isBlank(str)) {
      return defaultValue;
    }
    try {
      return Double.parseDouble(str.trim());
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }
}
