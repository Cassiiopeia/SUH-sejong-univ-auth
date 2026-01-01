package kr.suhsaechan.sejong.auth.model;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 학사정보시스템(SIS) 인증 결과
 * - sjpt.sejong.ac.kr에서 획득
 * - 연락처 정보 보장
 */
@Getter
@Builder
public class SejongSisAuthResult {

  /** 인증 성공 여부 */
  private final boolean success;

  /** 기본 학생 정보 */
  private final SejongStudentInfo studentInfo;

  /** 이메일 주소 */
  private final String email;

  /** 전화번호 */
  private final String phoneNumber;

  /** 영어 이름 */
  private final String englishName;

  /** 인증 시간 */
  private final LocalDateTime authenticatedAt;

  /** 원본 JSON (디버깅용, 기본 null) */
  private final String rawJson;

  @Override
  public String toString() {
    return String.format("SejongSisAuthResult{success=%s, studentInfo=%s, email='%s', phoneNumber='%s', englishName='%s', authenticatedAt=%s}",
        success, studentInfo, email, phoneNumber, englishName, authenticatedAt);
  }
}
