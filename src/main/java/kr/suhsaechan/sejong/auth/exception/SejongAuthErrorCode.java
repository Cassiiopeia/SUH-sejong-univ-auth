package kr.suhsaechan.sejong.auth.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 세종대학교 인증 관련 에러 코드
 */
@Getter
@AllArgsConstructor
public enum SejongAuthErrorCode {

  // 연결 관련
  CONNECTION_FAILED("SEJONG_AUTH_001", "Cannot connect to the Sejong University portal."),
  CONNECTION_TIMEOUT("SEJONG_AUTH_002", "Connection to the Sejong University portal timed out."),

  // 인증 관련
  INVALID_CREDENTIALS("SEJONG_AUTH_003", "Invalid student ID or password."),
  AUTHENTICATION_FAILED("SEJONG_AUTH_009", "Sejong University portal authentication failed. Check the student ID and password."),
  SESSION_ERROR("SEJONG_AUTH_004", "An error occurred while handling the portal session."),
  INVALID_INPUT("SEJONG_AUTH_008", "Invalid input."),

  // 데이터 관련
  DATA_FETCH_FAILED("SEJONG_AUTH_005", "Failed to fetch student information."),
  PARSE_ERROR("SEJONG_AUTH_006", "Failed to parse student information."),

  // 설정 관련
  SSL_CONFIGURATION_ERROR("SEJONG_AUTH_007", "Failed to configure SSL.");

  private final String code;
  private final String message;
}
