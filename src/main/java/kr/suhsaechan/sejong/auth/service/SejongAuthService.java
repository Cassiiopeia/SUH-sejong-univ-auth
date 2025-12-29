package kr.suhsaechan.sejong.auth.service;

import java.time.LocalDateTime;
import kr.suhsaechan.sejong.auth.client.SejongPortalClient;
import kr.suhsaechan.sejong.auth.exception.SejongAuthErrorCode;
import kr.suhsaechan.sejong.auth.exception.SejongAuthException;
import kr.suhsaechan.sejong.auth.model.SejongAuthResult;
import kr.suhsaechan.sejong.auth.model.SejongClassicReading;
import kr.suhsaechan.sejong.auth.model.SejongStudentInfo;
import kr.suhsaechan.sejong.auth.parser.SejongClassicReadingParser;
import kr.suhsaechan.sejong.auth.parser.SejongStudentInfoParser;
import kr.suhsaechan.sejong.auth.util.CommonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 세종대학교 인증 서비스
 * - 포털 로그인 및 학생 정보 조회 기능 제공
 */
@Slf4j
@RequiredArgsConstructor
public class SejongAuthService {

  private final SejongPortalClient portalClient;
  private final SejongStudentInfoParser studentInfoParser;
  private final SejongClassicReadingParser classicReadingParser;

  /**
   * 세종대학교 포털 인증 수행 (전체 정보)
   * - 기본 학생 정보 + 고전독서 인증 정보 반환
   *
   * @param studentId 학번
   * @param password 비밀번호
   * @return 인증 결과 (학생정보 + 고전독서 정보)
   * @throws SejongAuthException 인증 실패 시
   */
  public SejongAuthResult authenticate(String studentId, String password) {
    validateCredentials(studentId, password);
    log.info("세종대학교 인증 시작: studentId={}", studentId);

    // 1. 포털 로그인 및 HTML 가져오기
    String html = portalClient.authenticateAndFetchHtml(studentId, password);

    // 2. 학생 기본정보 파싱
    SejongStudentInfo studentInfo = studentInfoParser.parse(html);

    // 3. 고전독서 정보 파싱
    SejongClassicReading classicReading = classicReadingParser.parse(html);

    // 4. 결과 반환
    SejongAuthResult result = SejongAuthResult.builder()
        .success(true)
        .studentInfo(studentInfo)
        .classicReading(classicReading)
        .authenticatedAt(LocalDateTime.now())
        .build();

    log.info("세종대학교 인증 완료: studentId={}, name={}", studentId, studentInfo.getName());
    return result;
  }

  /**
   * 세종대학교 포털 인증 수행 (기본 정보만)
   * - 고전독서 정보 제외, 학생 기본 정보만 반환
   *
   * @param studentId 학번
   * @param password 비밀번호
   * @return 학생 기본정보
   * @throws SejongAuthException 인증 실패 시
   */
  public SejongStudentInfo authenticateBasic(String studentId, String password) {
    validateCredentials(studentId, password);
    log.info("세종대학교 기본 인증 시작: studentId={}", studentId);

    // 1. 포털 로그인 및 HTML 가져오기
    String html = portalClient.authenticateAndFetchHtml(studentId, password);

    // 2. 학생 기본정보 파싱
    SejongStudentInfo studentInfo = studentInfoParser.parse(html);

    log.info("세종대학교 기본 인증 완료: studentId={}, name={}", studentId, studentInfo.getName());
    return studentInfo;
  }

  /**
   * 세종대학교 포털 인증 수행 (원본 HTML 포함)
   * - 디버깅 용도로 원본 HTML도 함께 반환
   *
   * @param studentId 학번
   * @param password 비밀번호
   * @return 인증 결과 (원본 HTML 포함)
   * @throws SejongAuthException 인증 실패 시
   */
  public SejongAuthResult authenticateWithRawHtml(String studentId, String password) {
    validateCredentials(studentId, password);
    log.info("세종대학교 인증 시작 (원본 HTML 포함): studentId={}", studentId);

    // 1. 포털 로그인 및 HTML 가져오기
    String html = portalClient.authenticateAndFetchHtml(studentId, password);

    // 2. 학생 기본정보 파싱
    SejongStudentInfo studentInfo = studentInfoParser.parse(html);

    // 3. 고전독서 정보 파싱
    SejongClassicReading classicReading = classicReadingParser.parse(html);

    // 4. 결과 반환 (원본 HTML 포함)
    SejongAuthResult result = SejongAuthResult.builder()
        .success(true)
        .studentInfo(studentInfo)
        .classicReading(classicReading)
        .authenticatedAt(LocalDateTime.now())
        .rawHtml(html)
        .build();

    log.info("세종대학교 인증 완료 (원본 HTML 포함): studentId={}, name={}", studentId, studentInfo.getName());
    return result;
  }

  /**
   * 입력값 검증
   *
   * @param studentId 학번
   * @param password 비밀번호
   * @throws SejongAuthException 유효하지 않은 입력값
   */
  private void validateCredentials(String studentId, String password) {
    if (CommonUtil.isBlank(studentId)) {
      throw new SejongAuthException(SejongAuthErrorCode.INVALID_INPUT, "학번이 비어있습니다.");
    }
    if (CommonUtil.isBlank(password)) {
      throw new SejongAuthException(SejongAuthErrorCode.INVALID_INPUT, "비밀번호가 비어있습니다.");
    }
  }
}
