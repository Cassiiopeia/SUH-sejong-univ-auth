package kr.suhsaechan.sejong.auth.parser;

import java.util.HashMap;
import java.util.Map;
import kr.suhsaechan.sejong.auth.exception.SejongAuthErrorCode;
import kr.suhsaechan.sejong.auth.exception.SejongAuthException;
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
   * HTML에서 학과명 파싱
   *
   * @param html 고전독서인증현황 페이지 HTML
   * @return 학과명
   * @throws SejongAuthException 파싱 실패 시
   */
  public String parseMajor(String html) {
    Map<String, String> data = parseTableData(html);
    return CommonUtil.defaultIfBlank(data.get("학과명"), "");
  }

  /**
   * HTML에서 학번 파싱
   *
   * @param html 고전독서인증현황 페이지 HTML
   * @return 학번
   * @throws SejongAuthException 파싱 실패 시
   */
  public String parseStudentId(String html) {
    Map<String, String> data = parseTableData(html);
    return CommonUtil.defaultIfBlank(data.get("학번"), "");
  }

  /**
   * HTML에서 이름 파싱
   *
   * @param html 고전독서인증현황 페이지 HTML
   * @return 이름
   * @throws SejongAuthException 파싱 실패 시
   */
  public String parseName(String html) {
    Map<String, String> data = parseTableData(html);
    return CommonUtil.defaultIfBlank(data.get("이름"), "");
  }

  /**
   * HTML에서 학년 파싱
   *
   * @param html 고전독서인증현황 페이지 HTML
   * @return 학년
   * @throws SejongAuthException 파싱 실패 시
   */
  public String parseGrade(String html) {
    Map<String, String> data = parseTableData(html);
    return CommonUtil.defaultIfBlank(data.get("학년"), "");
  }

  /**
   * HTML에서 재학 상태 파싱
   *
   * @param html 고전독서인증현황 페이지 HTML
   * @return 재학 상태
   * @throws SejongAuthException 파싱 실패 시
   */
  public String parseStatus(String html) {
    Map<String, String> data = parseTableData(html);
    return CommonUtil.defaultIfBlank(data.get("사용자 상태"), "");
  }

  /**
   * HTML에서 테이블 데이터 파싱 (내부 캐싱용)
   */
  private Map<String, String> parseTableData(String html) {
    try {
      Document doc = Jsoup.parse(html);
      Map<String, String> data = extractTableData(doc);

      if (data.isEmpty()) {
        throw new SejongAuthException(SejongAuthErrorCode.PARSE_ERROR,
            "사용자 정보 테이블을 찾을 수 없습니다.");
      }

      return data;
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
