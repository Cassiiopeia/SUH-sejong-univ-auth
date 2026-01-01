package kr.suhsaechan.sejong.auth.model;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 대양휴머니티칼리지(DHC) 인증 결과
 * - classic.sejong.ac.kr에서 획득
 * - 고전독서 인증 정보 보장
 */
@Getter
@Builder
public class SejongDhcAuthResult {

  /** 인증 성공 여부 */
  private final boolean success;

  /** 기본 학생 정보 */
  private final SejongStudentInfo studentInfo;

  /** 고전독서 인증 정보 */
  private final SejongClassicReading classicReading;

  /** 인증 시간 */
  private final LocalDateTime authenticatedAt;

  /** 원본 HTML (디버깅용, 기본 null) */
  private final String rawHtml;

  @Override
  public String toString() {
    return String.format("SejongDhcAuthResult{success=%s, studentInfo=%s, classicReading=%s, authenticatedAt=%s}",
        success, studentInfo, classicReading, authenticatedAt);
  }
}
