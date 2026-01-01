package kr.suhsaechan.sejong.auth.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.suhsaechan.sejong.auth.exception.SejongAuthErrorCode;
import kr.suhsaechan.sejong.auth.exception.SejongAuthException;
import kr.suhsaechan.sejong.auth.model.SejongStudentInfo;
import kr.suhsaechan.sejong.auth.util.CommonUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 세종대학교 학사정보시스템(SIS) 응답 파서
 * - initUserInfo.do API 응답 JSON 파싱
 */
@Slf4j
public class SejongSisParser {

  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * JSON에서 학생 기본정보 파싱
   *
   * @param json initUserInfo.do API 응답 JSON
   * @return 학생 기본정보
   * @throws SejongAuthException 파싱 실패 시
   */
  public SejongStudentInfo parseStudentInfo(String json) {
    try {
      JsonNode root = objectMapper.readTree(json);

      // dm_UserInfo에서 기본 정보 추출
      JsonNode dmUserInfo = root.path("dm_UserInfo");
      // dm_UserInfoGam에서 학과 정보 추출
      JsonNode dmUserInfoGam = root.path("dm_UserInfoGam");
      // dm_UserInfoSch에서 추가 정보 추출
      JsonNode dmUserInfoSch = root.path("dm_UserInfoSch");

      if (dmUserInfo.isMissingNode()) {
        throw new SejongAuthException(SejongAuthErrorCode.PARSE_ERROR,
            "dm_UserInfo 필드를 찾을 수 없습니다.");
      }

      // 학번: dm_UserInfo.INTG_USR_NO
      String studentId = getTextValue(dmUserInfo, "INTG_USR_NO");

      // 이름: dm_UserInfo.INTG_USR_NM
      String name = getTextValue(dmUserInfo, "INTG_USR_NM");

      // 학과: dm_UserInfoGam.DEPT_NM (우선) 또는 dm_UserInfoSch.DEPT_NM
      String major = getTextValue(dmUserInfoGam, "DEPT_NM");
      if (CommonUtil.isBlank(major)) {
        major = getTextValue(dmUserInfoSch, "DEPT_NM");
      }

      // 학년: SIS에서는 직접 제공하지 않음, 빈 값
      String grade = "";

      // 재학 상태: dm_UserInfoGam.STATUS_DIV_CD를 해석하거나 빈 값
      // COA008001 = 재학, COA008002 = 휴학 등 (정확한 매핑은 추후 확인 필요)
      String status = "";

      SejongStudentInfo studentInfo = SejongStudentInfo.builder()
          .studentId(CommonUtil.defaultIfBlank(studentId, ""))
          .name(CommonUtil.defaultIfBlank(name, ""))
          .major(CommonUtil.defaultIfBlank(major, ""))
          .grade(grade)
          .status(status)
          .build();

      log.debug("SIS 학생 정보 파싱 완료: {}", studentInfo);
      return studentInfo;

    } catch (SejongAuthException e) {
      throw e;
    } catch (Exception e) {
      throw new SejongAuthException(SejongAuthErrorCode.PARSE_ERROR, e);
    }
  }

  /**
   * JSON에서 이메일 파싱
   *
   * @param json initUserInfo.do API 응답 JSON
   * @return 이메일 주소
   * @throws SejongAuthException 파싱 실패 시
   */
  public String parseEmail(String json) {
    try {
      JsonNode root = objectMapper.readTree(json);
      JsonNode dmUserInfoGam = root.path("dm_UserInfoGam");
      String email = getTextValue(dmUserInfoGam, "USER_EMAIL");
      return CommonUtil.defaultIfBlank(email, "");
    } catch (Exception e) {
      throw new SejongAuthException(SejongAuthErrorCode.PARSE_ERROR, e);
    }
  }

  /**
   * JSON에서 전화번호 파싱
   *
   * @param json initUserInfo.do API 응답 JSON
   * @return 전화번호 (010-1234-5678 형식)
   * @throws SejongAuthException 파싱 실패 시
   */
  public String parsePhoneNumber(String json) {
    try {
      JsonNode root = objectMapper.readTree(json);
      JsonNode dmUserInfoGam = root.path("dm_UserInfoGam");
      String phoneNumber = buildPhoneNumber(dmUserInfoGam);
      return CommonUtil.defaultIfBlank(phoneNumber, "");
    } catch (Exception e) {
      throw new SejongAuthException(SejongAuthErrorCode.PARSE_ERROR, e);
    }
  }

  /**
   * JSON에서 영어 이름 파싱
   *
   * @param json initUserInfo.do API 응답 JSON
   * @return 영어 이름
   * @throws SejongAuthException 파싱 실패 시
   */
  public String parseEnglishName(String json) {
    try {
      JsonNode root = objectMapper.readTree(json);
      JsonNode dmUserInfo = root.path("dm_UserInfo");
      JsonNode dmUserInfoSch = root.path("dm_UserInfoSch");

      String englishName = getTextValue(dmUserInfo, "INTG_ENG_NM");
      if (CommonUtil.isBlank(englishName)) {
        englishName = getTextValue(dmUserInfoSch, "NM_ENG");
      }
      return CommonUtil.defaultIfBlank(englishName, "");
    } catch (Exception e) {
      throw new SejongAuthException(SejongAuthErrorCode.PARSE_ERROR, e);
    }
  }

  /**
   * JsonNode에서 텍스트 값 추출
   */
  private String getTextValue(JsonNode node, String fieldName) {
    if (node == null || node.isMissingNode()) {
      return "";
    }
    JsonNode field = node.path(fieldName);
    if (field.isMissingNode() || field.isNull()) {
      return "";
    }
    return field.asText("");
  }

  /**
   * 전화번호 조합 (NO1-NO2-NO3)
   */
  private String buildPhoneNumber(JsonNode dmUserInfoGam) {
    if (dmUserInfoGam == null || dmUserInfoGam.isMissingNode()) {
      return "";
    }

    String no1 = getTextValue(dmUserInfoGam, "USER_PHONE_NO1");
    String no2 = getTextValue(dmUserInfoGam, "USER_PHONE_NO2");
    String no3 = getTextValue(dmUserInfoGam, "USER_PHONE_NO3");

    // 모두 비어있으면 빈 문자열
    if (CommonUtil.isBlank(no1) && CommonUtil.isBlank(no2) && CommonUtil.isBlank(no3)) {
      return "";
    }

    // 조합
    StringBuilder sb = new StringBuilder();
    if (CommonUtil.hasText(no1)) {
      sb.append(no1);
    }
    if (CommonUtil.hasText(no2)) {
      if (sb.length() > 0) sb.append("-");
      sb.append(no2);
    }
    if (CommonUtil.hasText(no3)) {
      if (sb.length() > 0) sb.append("-");
      sb.append(no3);
    }

    return sb.toString();
  }
}
