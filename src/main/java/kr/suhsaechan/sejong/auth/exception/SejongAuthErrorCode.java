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
  CONNECTION_FAILED("SEJONG_AUTH_001", "세종대학교 포털에 연결할 수 없습니다."),
  CONNECTION_TIMEOUT("SEJONG_AUTH_002", "세종대학교 포털 연결 시간이 초과되었습니다."),

  // 인증 관련
  INVALID_CREDENTIALS("SEJONG_AUTH_003", "학번 또는 비밀번호가 올바르지 않습니다."),
  AUTHENTICATION_FAILED("SEJONG_AUTH_009", "세종대학교 포털 인증에 실패했습니다. 학번과 비밀번호를 확인해주세요."),
  SESSION_ERROR("SEJONG_AUTH_004", "세션 처리 중 오류가 발생했습니다."),
  INVALID_INPUT("SEJONG_AUTH_008", "입력값이 유효하지 않습니다."),

  // 데이터 관련
  DATA_FETCH_FAILED("SEJONG_AUTH_005", "학생 정보를 가져오는데 실패했습니다."),
  PARSE_ERROR("SEJONG_AUTH_006", "학생 정보 파싱 중 오류가 발생했습니다."),

  // 설정 관련
  SSL_CONFIGURATION_ERROR("SEJONG_AUTH_007", "SSL 설정 중 오류가 발생했습니다.");

  private final String code;
  private final String message;
}
