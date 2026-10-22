// spec/fixtures 파싱 결과 == 기대값 (Java, Python 과 같은 기준)
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { parseClassicReading, parseSisUserInfo, parseStudentInfo } from '../dist/esm/parsers.js';
import { readExpected, readFixture } from './helpers.mjs';

test('parseDhcFixture', () => {
  const html = readFixture('dhc_status.html');
  const expected = readExpected('expected_dhc.json');

  assert.deepEqual(parseStudentInfo(html), expected.studentInfo);
  assert.deepEqual(parseClassicReading(html), expected.classicReading);
});

test('parseSisFixture', () => {
  assert.deepEqual(parseSisUserInfo(readFixture('sis_user_info.json')), readExpected('expected_sis.json'));
});

test('decodesCommonNamedEntitiesAndIgnoresInvalidCodePoints', () => {
  // Python html.parser·Jsoup 과 같은 디코딩 (책 제목의 따옴표·말줄임표), 범위 밖 코드는 원문 유지
  const html = `<div class="b-con-box"><h4 class="b-h4-tit01">사용자 정보</h4><table class="b-board-table"><tbody>
    <tr><th>학과명</th><td>&lsquo;컴퓨터&rsquo; &ldquo;공학&rdquo;&hellip; &ndash;&mdash;&#x110000;</td></tr></tbody></table></div>`;
  assert.equal(parseStudentInfo(html).major, '‘컴퓨터’ “공학”… –—&#x110000;');
});
