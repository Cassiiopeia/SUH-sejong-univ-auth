package kr.suhsaechan.sejong.auth.support;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 실제 포털 연동 테스트 계정 (application-dev.yml, gitignore 대상)
 * sejong.auth.test.student-id / sejong.auth.test.password
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "sejong.auth.test")
public class SejongTestAccountProperties {

  /** 테스트용 학번 */
  private String studentId;

  /** 테스트용 비밀번호 */
  private String password;
}
