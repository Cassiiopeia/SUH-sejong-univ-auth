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
