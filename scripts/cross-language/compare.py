"""세 언어 결과 요약 비교: 인증 오류·필드 해시 차이 (다른 필드 이름만 출력, 다르면 exit 1)

사용: python3 compare.py <요약 폴더> [--wrong-password]
  요약 폴더: {java,python,node}.json, --wrong-password 면 {언어}-wrong.json·{언어}-sis-wrong.json 도 확인
"""

import json
import sys

LANGUAGES = ("java", "python", "node")


def load_summary(summary_directory: str, name: str) -> dict:
    with open(f"{summary_directory}/{name}.json", encoding="utf-8") as summary_file:
        return json.load(summary_file)


def has_success_mismatch(summary_directory: str) -> bool:
    """정상 인증: 세 언어 모두 성공 + 필드별 해시 일치"""
    results = {language: load_summary(summary_directory, language) for language in LANGUAGES}
    is_failed = False
    for language, summary in results.items():
        if "error" in summary:
            print(f"{language}: authentication error {summary['error']}")
            is_failed = True
    if is_failed:
        return True
    for field in results["java"]["fields"]:
        hashes = {language: results[language]["fields"].get(field) for language in LANGUAGES}
        status = "same" if len(set(hashes.values())) == 1 else "DIFFERENT"
        print(f"{field:15} {status}")
        is_failed |= status != "same"
    return is_failed


def has_wrong_password_mismatch(summary_directory: str) -> bool:
    """틀린 비밀번호: 통합·SIS 모두 AUTHENTICATION_FAILED"""
    wrong = {language: load_summary(summary_directory, f"{language}-wrong") for language in LANGUAGES}
    wrong.update({f"{language} sis": load_summary(summary_directory, f"{language}-sis-wrong") for language in LANGUAGES})
    is_failed = False
    for language, summary in wrong.items():
        is_expected = summary.get("error") == "AUTHENTICATION_FAILED"
        print(f"wrong password {language:10} {'AUTHENTICATION_FAILED' if is_expected else summary}")
        is_failed |= not is_expected
    return is_failed


def main() -> None:
    summary_directory = sys.argv[1]
    is_failed = has_success_mismatch(summary_directory)
    if "--wrong-password" in sys.argv:
        is_failed |= has_wrong_password_mismatch(summary_directory)
    sys.exit(1 if is_failed else 0)


if __name__ == "__main__":
    main()
