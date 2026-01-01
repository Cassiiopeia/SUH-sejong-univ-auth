package kr.suhsaechan.sejong.auth.model;

import lombok.Builder;
import lombok.Getter;

/**
 * 학생 연락처 정보 (학사정보시스템에서 획득)
 */
@Getter
@Builder
public class ContactInfo {

  /** 이메일 주소 */
  private final String email;

  /** 전화번호 */
  private final String phoneNumber;

  /** 영어 이름 */
  private final String englishName;

  @Override
  public String toString() {
    return String.format("ContactInfo{email='%s', phoneNumber='%s', englishName='%s'}",
        email, phoneNumber, englishName);
  }
}
