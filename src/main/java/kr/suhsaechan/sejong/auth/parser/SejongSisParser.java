package kr.suhsaechan.sejong.auth.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.suhsaechan.sejong.auth.exception.SejongAuthErrorCode;
import kr.suhsaechan.sejong.auth.exception.SejongAuthException;
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
   * JSON에서 학과명 파싱
   *
   * @param json initUserInfo.do API 응답 JSON
   * @return 학과명
   * @throws SejongAuthException 파싱 실패 시
   */
  public String parseMajor(String json) {
    try {
      JsonNode root = objectMapper.readTree(json);
      JsonNode dmUserInfoGam = root.path("dm_UserInfoGam");
      JsonNode dmUserInfoSch = root.path("dm_UserInfoSch");

      String major = getTextValue(dmUserInfoGam, "DEPT_NM");
      if (CommonUtil.isBlank(major)) {
        major = getTextValue(dmUserInfoSch, "DEPT_NM");
      }
      return CommonUtil.defaultIfBlank(major, "");
    } catch (Exception e) {
      throw new SejongAuthException(SejongAuthErrorCode.PARSE_ERROR, e);
    }
  }

  /**
   * JSON에서 학번 파싱
   *
   * @param json initUserInfo.do API 응답 JSON
   * @return 학번
   * @throws SejongAuthException 파싱 실패 시
   */
  public String parseStudentId(String json) {
    try {
      JsonNode root = objectMapper.readTree(json);
      JsonNode dmUserInfo = root.path("dm_UserInfo");

      if (dmUserInfo.isMissingNode()) {
        throw new SejongAuthException(SejongAuthErrorCode.PARSE_ERROR,
            "dm_UserInfo 필드를 찾을 수 없습니다.");
      }

      String studentId = getTextValue(dmUserInfo, "INTG_USR_NO");
      return CommonUtil.defaultIfBlank(studentId, "");
    } catch (SejongAuthException e) {
      throw e;
    } catch (Exception e) {
      throw new SejongAuthException(SejongAuthErrorCode.PARSE_ERROR, e);
    }
  }

  /**
   * JSON에서 이름 파싱
   *
   * @param json initUserInfo.do API 응답 JSON
   * @return 이름
   * @throws SejongAuthException 파싱 실패 시
   */
  public String parseName(String json) {
    try {
      JsonNode root = objectMapper.readTree(json);
      JsonNode dmUserInfo = root.path("dm_UserInfo");

      String name = getTextValue(dmUserInfo, "INTG_USR_NM");
      return CommonUtil.defaultIfBlank(name, "");
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
