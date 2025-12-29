package kr.suhsaechan.sejong.auth.model;

import lombok.Builder;
import lombok.Getter;

/**
 * 세종대학교 학생 기본 정보
 */
@Getter
@Builder
public class SejongStudentInfo {

  /** 학과명 (예: 컴퓨터공학과) */
  private final String major;

  /** 학번 (예: 20171234) */
  private final String studentId;

  /** 이름 (예: 홍길동) */
  private final String name;

  /** 학년 (예: 4학년) */
  private final String grade;

  /** 재학 상태 (예: 재학, 휴학, 졸업) */
  private final String status;

  @Override
  public String toString() {
    return String.format("SejongStudentInfo{major='%s', studentId='%s', name='%s', grade='%s', status='%s'}",
        major, studentId, name, grade, status);
  }
}
