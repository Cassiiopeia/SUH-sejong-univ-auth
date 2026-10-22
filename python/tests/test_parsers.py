"""spec/fixtures 파싱 결과 == 기대값 (Java, Node 와 같은 기준)"""

import re

from conftest import read_expected

from sejong_auth.parsers import parse_classic_reading, parse_sis_user_info, parse_student_info


def to_snake_case(name: str) -> str:
    return re.sub(r"(?<!^)(?=[A-Z])", "_", name).lower()


def snake_case_keys(value):
    if isinstance(value, dict):
        return {to_snake_case(key): snake_case_keys(item) for key, item in value.items()}
    if isinstance(value, list):
        return [snake_case_keys(item) for item in value]
    return value


def test_parse_dhc_fixture(dhc_html):
    expected = snake_case_keys(read_expected("expected_dhc.json"))

    assert parse_student_info(dhc_html).to_dict() == expected["student_info"]
    assert parse_classic_reading(dhc_html).to_dict() == expected["classic_reading"]


def test_parse_sis_fixture(sis_json):
    expected = snake_case_keys(read_expected("expected_sis.json"))

    assert parse_sis_user_info(sis_json).to_dict() == expected


def test_decodes_common_named_entities():
    # Node·Jsoup 과 같은 기대값
    html = ('<div class="b-con-box"><h4 class="b-h4-tit01">사용자 정보</h4><table class="b-board-table"><tbody>'
            '<tr><th>학과명</th><td>&lsquo;컴퓨터&rsquo; &ldquo;공학&rdquo;&hellip; &ndash;&mdash;</td></tr></tbody></table></div>')
    assert parse_student_info(html).major == "‘컴퓨터’ “공학”… –—"
