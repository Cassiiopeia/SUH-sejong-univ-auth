package kr.suhsaechan.sejong.auth.parser;

import java.util.HashMap;
import java.util.Map;
import kr.suhsaechan.sejong.auth.exception.SejongAuthErrorCode;
import kr.suhsaechan.sejong.auth.exception.SejongAuthException;
import kr.suhsaechan.sejong.auth.model.SejongStudentInfo;
import kr.suhsaechan.sejong.auth.util.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * 세종대학교 학생 기본 정보 파서
 * - 고전독서인증현황 페이지에서 사용자 정보 테이블 파싱
 */
@Slf4j
public class SejongStudentInfoParser {

  private static final String USER_INFO_SELECTOR =
      ".b-con-box:has(h4.b-h4-tit01:contains(사용자 정보)) table.b-board-table tbody tr";

  /**
   * HTML에서 학생 기본정보 파싱
   *
   * @param html 고전독서인증현황 페이지 HTML
   * @return 학생 기본정보
   * @throws SejongAuthException 파싱 실패 시
   */
  public SejongStudentInfo parse(String html) {
    try {
      Document doc = Jsoup.parse(html);
      Map<String, String> data = extractTableData(doc);

      if (data.isEmpty()) {
        throw new SejongAuthException(SejongAuthErrorCode.PARSE_ERROR,
            "사용자 정보 테이블을 찾을 수 없습니다.");
      }

      SejongStudentInfo studentInfo = SejongStudentInfo.builder()
          .major(CommonUtil.defaultIfBlank(data.get("학과명"), ""))
          .studentId(CommonUtil.defaultIfBlank(data.get("학번"), ""))
          .name(CommonUtil.defaultIfBlank(data.get("이름"), ""))
          .grade(CommonUtil.defaultIfBlank(data.get("학년"), ""))
          .status(CommonUtil.defaultIfBlank(data.get("사용자 상태"), ""))
          .build();

      log.debug("학생 정보 파싱 완료: {}", studentInfo);
      return studentInfo;

    } catch (SejongAuthException e) {
      throw e;
    } catch (Exception e) {
      throw new SejongAuthException(SejongAuthErrorCode.PARSE_ERROR, e);
    }
  }

  /**
   * 테이블에서 라벨-값 쌍 추출
   */
  private Map<String, String> extractTableData(Document doc) {
    Map<String, String> data = new HashMap<>();

    doc.select(USER_INFO_SELECTOR).forEach(row -> {
      String label = CommonUtil.trimToEmpty(row.select("th").text());
      String value = CommonUtil.trimToEmpty(row.select("td").text());
      if (CommonUtil.hasText(label)) {
        data.put(label, value);
      }
    });

    return data;
  }
}
