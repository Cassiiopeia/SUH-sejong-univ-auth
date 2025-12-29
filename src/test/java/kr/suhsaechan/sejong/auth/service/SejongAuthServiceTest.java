package kr.suhsaechan.sejong.auth.service;

import kr.suhsaechan.sejong.auth.TestApplication;
import kr.suhsaechan.sejong.auth.config.SejongAuthProperties;
import kr.suhsaechan.sejong.auth.exception.SejongAuthErrorCode;
import kr.suhsaechan.sejong.auth.exception.SejongAuthException;
import kr.suhsaechan.sejong.auth.model.SejongAuthResult;
import kr.suhsaechan.sejong.auth.model.SejongStudentInfo;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 세종대학교 인증 서비스 스프링 테스트
 *
 * 실제 포털 연동 테스트는 @Disabled 처리되어 있습니다.
 * 로컬에서 테스트하려면 @Disabled를 제거하고 학번/비밀번호를 입력하세요.
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("dev")
@Slf4j
class SejongAuthServiceTest {

  @Autowired
  SejongAuthService sejongAuthService;

  @Autowired
  SejongAuthProperties sejongAuthProperties;

  @Test
  public void mainTest() {
    log.info("============ 테스트시작 ============");

    sejongAuthService_테스트();

    log.info("============ 테스트종료 ============");
  }

  public void sejongAuthService_테스트() {
    // TODO: 테스트 로직 작성
    log.info("테스트 실행중");
    assertNotNull(sejongAuthService, "SejongAuthService가 정상적으로 주입되어야 합니다.");
    log.info("SejongAuthService 빈 주입 확인 완료");
  }

  @Test
  void 입력값검증_학번_빈값() {
    log.info("============ 입력값 검증 테스트 - 학번 빈값 ============");

    SejongAuthException exception = assertThrows(SejongAuthException.class, () -> {
      sejongAuthService.authenticate("", "password");
    });

    assertEquals(SejongAuthErrorCode.INVALID_INPUT, exception.getErrorCode());
    log.info("예외 발생 확인: {}", exception.getMessage());
  }

  @Test
  void 입력값검증_비밀번호_빈값() {
    log.info("============ 입력값 검증 테스트 - 비밀번호 빈값 ============");

    SejongAuthException exception = assertThrows(SejongAuthException.class, () -> {
      sejongAuthService.authenticate("20012345", "");
    });

    assertEquals(SejongAuthErrorCode.INVALID_INPUT, exception.getErrorCode());
    log.info("예외 발생 확인: {}", exception.getMessage());
  }

  @Test
  void 입력값검증_학번_null() {
    log.info("============ 입력값 검증 테스트 - 학번 null ============");

    SejongAuthException exception = assertThrows(SejongAuthException.class, () -> {
      sejongAuthService.authenticate(null, "password");
    });

    assertEquals(SejongAuthErrorCode.INVALID_INPUT, exception.getErrorCode());
    log.info("예외 발생 확인: {}", exception.getMessage());
  }

  @Test
  @Disabled("실제 포털 연동 테스트 - 로컬에서만 실행")
  void 실제인증_전체정보() {
    log.info("============ 실제 포털 인증 테스트 - 전체 정보 ============");

    String testStudentId = sejongAuthProperties.getTest().getStudentId();
    String testPassword = sejongAuthProperties.getTest().getPassword();

    assertNotNull(testStudentId, "테스트용 학번이 설정되지 않았습니다.");
    assertNotNull(testPassword, "테스트용 비밀번호가 설정되지 않았습니다.");
    assertFalse(testStudentId.isEmpty(), "테스트용 학번이 설정되지 않았습니다.");
    assertFalse(testPassword.isEmpty(), "테스트용 비밀번호가 설정되지 않았습니다.");

    SejongAuthResult result = sejongAuthService.authenticate(testStudentId, testPassword);

    assertTrue(result.isSuccess());
    assertNotNull(result.getStudentInfo());
    assertNotNull(result.getClassicReading());
    assertNotNull(result.getAuthenticatedAt());

    SejongStudentInfo info = result.getStudentInfo();
    log.info("=== 인증 결과 ===");
    log.info("학과: {}", info.getMajor());
    log.info("학번: {}", info.getStudentId());
    log.info("이름: {}", info.getName());
    log.info("학년: {}", info.getGrade());
    log.info("상태: {}", info.getStatus());
  }

  @Test
  @Disabled("실제 포털 연동 테스트 - 로컬에서만 실행")
  void 실제인증_기본정보만() {
    log.info("============ 실제 포털 인증 테스트 - 기본 정보만 ============");

    String testStudentId = sejongAuthProperties.getTest().getStudentId();
    String testPassword = sejongAuthProperties.getTest().getPassword();

    assertNotNull(testStudentId, "테스트용 학번이 설정되지 않았습니다.");
    assertNotNull(testPassword, "테스트용 비밀번호가 설정되지 않았습니다.");
    assertFalse(testStudentId.isEmpty(), "테스트용 학번이 설정되지 않았습니다.");
    assertFalse(testPassword.isEmpty(), "테스트용 비밀번호가 설정되지 않았습니다.");

    SejongStudentInfo info = sejongAuthService.authenticateBasic(testStudentId, testPassword);

    assertNotNull(info);
    assertNotNull(info.getMajor());
    assertNotNull(info.getStudentId());
    assertNotNull(info.getName());

    log.info("=== 기본 인증 결과 ===");
    log.info("학과: {}", info.getMajor());
    log.info("학번: {}", info.getStudentId());
    log.info("이름: {}", info.getName());
  }

  @Test
  @Disabled("실제 포털 연동 테스트 - 로컬에서만 실행")
  void 실제인증_원본HTML포함() {
    log.info("============ 실제 포털 인증 테스트 - 원본 HTML 포함 ============");

    String testStudentId = sejongAuthProperties.getTest().getStudentId();
    String testPassword = sejongAuthProperties.getTest().getPassword();

    assertNotNull(testStudentId, "테스트용 학번이 설정되지 않았습니다.");
    assertNotNull(testPassword, "테스트용 비밀번호가 설정되지 않았습니다.");
    assertFalse(testStudentId.isEmpty(), "테스트용 학번이 설정되지 않았습니다.");
    assertFalse(testPassword.isEmpty(), "테스트용 비밀번호가 설정되지 않았습니다.");

    SejongAuthResult result = sejongAuthService.authenticateWithRawHtml(testStudentId, testPassword);

    assertTrue(result.isSuccess());
    assertNotNull(result.getRawHtml());
    assertTrue(result.getRawHtml().length() > 0);

    log.info("=== 원본 HTML 포함 인증 결과 ===");
    log.info("HTML 길이: {} 바이트", result.getRawHtml().length());
  }

  @Test
  @Disabled("실제 포털 연동 테스트 - 로컬에서만 실행")
  void 실제인증_잘못된비밀번호() {
    log.info("============ 실제 포털 인증 테스트 - 잘못된 비밀번호 ============");

    String testStudentId = sejongAuthProperties.getTest().getStudentId();
    assertNotNull(testStudentId, "테스트용 학번이 설정되지 않았습니다.");
    assertFalse(testStudentId.isEmpty(), "테스트용 학번이 설정되지 않았습니다.");

    // 잘못된 비밀번호로 테스트
    String wrongPassword = "wrongpassword123";

    SejongAuthException exception = assertThrows(SejongAuthException.class, () -> {
      sejongAuthService.authenticate(testStudentId, wrongPassword);
    });

    assertEquals(SejongAuthErrorCode.AUTHENTICATION_FAILED, exception.getErrorCode());
    log.info("=== 예외 발생 확인 ===");
    log.info("에러 코드: {}", exception.getErrorCode());
    log.info("메시지: {}", exception.getMessage());
  }
}
