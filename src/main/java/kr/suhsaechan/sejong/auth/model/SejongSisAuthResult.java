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

  /** 연락처 정보 */
  private final ContactInfo contactInfo;

  /** 인증 시간 */
  private final LocalDateTime authenticatedAt;

  /** 원본 JSON (디버깅용, 기본 null) */
  private final String rawJson;

  @Override
  public String toString() {
    return String.format("SejongSisAuthResult{success=%s, studentInfo=%s, contactInfo=%s, authenticatedAt=%s}",
        success, studentInfo, contactInfo, authenticatedAt);
  }
}
