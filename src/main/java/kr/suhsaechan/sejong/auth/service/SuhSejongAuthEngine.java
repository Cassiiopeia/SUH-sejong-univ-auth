package kr.suhsaechan.sejong.auth.service;

import java.time.LocalDateTime;
import kr.suhsaechan.sejong.auth.client.SejongPortalClient;
import kr.suhsaechan.sejong.auth.client.SejongSisClient;
import kr.suhsaechan.sejong.auth.exception.SejongAuthErrorCode;
import kr.suhsaechan.sejong.auth.exception.SejongAuthException;
import kr.suhsaechan.sejong.auth.model.ContactInfo;
import kr.suhsaechan.sejong.auth.model.SejongAuthResult;
import kr.suhsaechan.sejong.auth.model.SejongClassicReading;
import kr.suhsaechan.sejong.auth.model.SejongDhcAuthResult;
import kr.suhsaechan.sejong.auth.model.SejongSisAuthResult;
import kr.suhsaechan.sejong.auth.model.SejongStudentInfo;
import kr.suhsaechan.sejong.auth.parser.SejongClassicReadingParser;
import kr.suhsaechan.sejong.auth.parser.SejongSisParser;
import kr.suhsaechan.sejong.auth.parser.SejongStudentInfoParser;
import kr.suhsaechan.sejong.auth.util.CommonUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 세종대학교 인증 서비스
 * - 포털 로그인 및 학생 정보 조회 기능 제공
 * - DHC(대양휴머니티칼리지) 및 SIS(학사정보시스템) 데이터 소스 지원
 */
@Slf4j
public class SuhSejongAuthEngine {

  private final SejongPortalClient portalClient;
  private final SejongStudentInfoParser studentInfoParser;
  private final SejongClassicReadingParser classicReadingParser;
  private final SejongSisClient sisClient;
  private final SejongSisParser sisParser;

  public SuhSejongAuthEngine(
      SejongPortalClient portalClient,
      SejongStudentInfoParser studentInfoParser,
      SejongClassicReadingParser classicReadingParser,
      SejongSisClient sisClient,
      SejongSisParser sisParser) {
    this.portalClient = portalClient;
    this.studentInfoParser = studentInfoParser;
    this.classicReadingParser = classicReadingParser;
    this.sisClient = sisClient;
    this.sisParser = sisParser;
  }

  /**
   * 세종대학교 통합 인증 수행
   * - DHC(고전독서 정보) + SIS(연락처 정보) 모두 조회
   *
   * @param studentId 학번
   * @param password 비밀번호
   * @return 통합 인증 결과 (학생정보 + 고전독서 + 연락처)
   * @throws SejongAuthException 인증 실패 시
   */
  public SejongAuthResult authenticate(String studentId, String password) {
    validateCredentials(studentId, password);
    log.info("세종대학교 통합 인증 시작: studentId={}", studentId);

    // 1. DHC에서 정보 가져오기
    String html = portalClient.authenticateAndFetchHtml(studentId, password);
    SejongStudentInfo dhcStudentInfo = studentInfoParser.parse(html);
    SejongClassicReading classicReading = classicReadingParser.parse(html);

    // 2. SIS에서 연락처 정보 가져오기
    ContactInfo contactInfo = null;
    try {
      String json = sisClient.authenticateAndFetchJson(studentId, password);
      contactInfo = sisParser.parseContactInfo(json);
    } catch (Exception e) {
      log.warn("SIS 연락처 정보 조회 실패 (무시됨): {}", e.getMessage());
      // SIS 실패 시에도 DHC 정보만으로 결과 반환
    }

    // 3. 결과 반환
    SejongAuthResult result = SejongAuthResult.builder()
        .success(true)
        .studentInfo(dhcStudentInfo)
        .classicReading(classicReading)
        .contactInfo(contactInfo)
        .authenticatedAt(LocalDateTime.now())
        .build();

    log.info("세종대학교 통합 인증 완료: studentId={}, name={}", studentId, dhcStudentInfo.getName());
    return result;
  }

  /**
   * 대양휴머니티칼리지(DHC) 인증 수행
   * - classic.sejong.ac.kr에서 정보 조회
   * - 고전독서 인증 정보 보장
   *
   * @param studentId 학번
   * @param password 비밀번호
   * @return DHC 인증 결과 (학생정보 + 고전독서)
   * @throws SejongAuthException 인증 실패 시
   */
  public SejongDhcAuthResult authenticateWithDHC(String studentId, String password) {
    validateCredentials(studentId, password);
    log.info("세종대학교 DHC 인증 시작: studentId={}", studentId);

    // 1. 포털 로그인 및 HTML 가져오기
    String html = portalClient.authenticateAndFetchHtml(studentId, password);

    // 2. 학생 기본정보 파싱
    SejongStudentInfo studentInfo = studentInfoParser.parse(html);

    // 3. 고전독서 정보 파싱
    SejongClassicReading classicReading = classicReadingParser.parse(html);

    // 4. 결과 반환
    SejongDhcAuthResult result = SejongDhcAuthResult.builder()
        .success(true)
        .studentInfo(studentInfo)
        .classicReading(classicReading)
        .authenticatedAt(LocalDateTime.now())
        .build();

    log.info("세종대학교 DHC 인증 완료: studentId={}, name={}", studentId, studentInfo.getName());
    return result;
  }

  /**
   * 학사정보시스템(SIS) 인증 수행
   * - sjpt.sejong.ac.kr에서 정보 조회
   * - 연락처 정보 보장
   *
   * @param studentId 학번
   * @param password 비밀번호
   * @return SIS 인증 결과 (학생정보 + 연락처)
   * @throws SejongAuthException 인증 실패 시
   */
  public SejongSisAuthResult authenticateWithSIS(String studentId, String password) {
    validateCredentials(studentId, password);
    log.info("세종대학교 SIS 인증 시작: studentId={}", studentId);

    // 1. SIS 로그인 및 JSON 가져오기
    String json = sisClient.authenticateAndFetchJson(studentId, password);

    // 2. 학생 기본정보 파싱
    SejongStudentInfo studentInfo = sisParser.parseStudentInfo(json);

    // 3. 연락처 정보 파싱
    ContactInfo contactInfo = sisParser.parseContactInfo(json);

    // 4. 결과 반환
    SejongSisAuthResult result = SejongSisAuthResult.builder()
        .success(true)
        .studentInfo(studentInfo)
        .contactInfo(contactInfo)
        .authenticatedAt(LocalDateTime.now())
        .build();

    log.info("세종대학교 SIS 인증 완료: studentId={}, name={}", studentId, studentInfo.getName());
    return result;
  }

  /**
   * DHC 인증 수행 (원본 HTML 포함)
   * - 디버깅 용도로 원본 HTML도 함께 반환
   *
   * @param studentId 학번
   * @param password 비밀번호
   * @return DHC 인증 결과 (원본 HTML 포함)
   * @throws SejongAuthException 인증 실패 시
   */
  public SejongDhcAuthResult authenticateWithDHCRaw(String studentId, String password) {
    validateCredentials(studentId, password);
    log.info("세종대학교 DHC 인증 시작 (원본 HTML 포함): studentId={}", studentId);

    // 1. 포털 로그인 및 HTML 가져오기
    String html = portalClient.authenticateAndFetchHtml(studentId, password);

    // 2. 학생 기본정보 파싱
    SejongStudentInfo studentInfo = studentInfoParser.parse(html);

    // 3. 고전독서 정보 파싱
    SejongClassicReading classicReading = classicReadingParser.parse(html);

    // 4. 결과 반환 (원본 HTML 포함)
    SejongDhcAuthResult result = SejongDhcAuthResult.builder()
        .success(true)
        .studentInfo(studentInfo)
        .classicReading(classicReading)
        .authenticatedAt(LocalDateTime.now())
        .rawHtml(html)
        .build();

    log.info("세종대학교 DHC 인증 완료 (원본 HTML 포함): studentId={}, name={}", studentId, studentInfo.getName());
    return result;
  }

  /**
   * SIS 인증 수행 (원본 JSON 포함)
   * - 디버깅 용도로 원본 JSON도 함께 반환
   *
   * @param studentId 학번
   * @param password 비밀번호
   * @return SIS 인증 결과 (원본 JSON 포함)
   * @throws SejongAuthException 인증 실패 시
   */
  public SejongSisAuthResult authenticateWithSISRaw(String studentId, String password) {
    validateCredentials(studentId, password);
    log.info("세종대학교 SIS 인증 시작 (원본 JSON 포함): studentId={}", studentId);

    // 1. SIS 로그인 및 JSON 가져오기
    String json = sisClient.authenticateAndFetchJson(studentId, password);

    // 2. 학생 기본정보 파싱
    SejongStudentInfo studentInfo = sisParser.parseStudentInfo(json);

    // 3. 연락처 정보 파싱
    ContactInfo contactInfo = sisParser.parseContactInfo(json);

    // 4. 결과 반환 (원본 JSON 포함)
    SejongSisAuthResult result = SejongSisAuthResult.builder()
        .success(true)
        .studentInfo(studentInfo)
        .contactInfo(contactInfo)
        .authenticatedAt(LocalDateTime.now())
        .rawJson(json)
        .build();

    log.info("세종대학교 SIS 인증 완료 (원본 JSON 포함): studentId={}, name={}", studentId, studentInfo.getName());
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
