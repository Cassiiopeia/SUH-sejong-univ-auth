// 실제 세종 포털 연동 (환경변수 SEJONG_TEST_STUDENT_ID, SEJONG_TEST_PASSWORD 있을 때만)
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { ErrorCode, authenticate, authenticateDhc, authenticateSis } from '../dist/esm/index.js';

const studentId = process.env.SEJONG_TEST_STUDENT_ID;
const password = process.env.SEJONG_TEST_PASSWORD;
const skip = !(studentId && password) && 'SEJONG_TEST_* not set';

test('liveIntegrated', { skip }, async () => {
  const result = await authenticate(studentId, password);
  assert.equal(result.studentId, studentId);
  assert.ok(result.major && result.name);
  assert.ok(result.classicReading.certifications.length > 0);
  assert.notEqual(result.email, null);
});

test('liveDhcAndSis', { skip }, async () => {
  assert.equal((await authenticateDhc(studentId, password)).studentId, studentId);
  assert.equal((await authenticateSis(studentId, password)).studentId, studentId);
});

test('liveWrongPassword', { skip }, async () => {
  await assert.rejects(authenticateDhc(studentId, `${password}-wrong`), (error) => error.code === ErrorCode.AUTHENTICATION_FAILED);
});
