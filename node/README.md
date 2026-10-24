# pro-sejong-auth (Node.js)

**Sejong University login API for Node.js** · 세종대 로그인 API

Verify a Sejong University student with a student ID and password through the portal SSO, and get their major, year, classic-reading certification and contact info. Zero runtime dependencies, TypeScript types included, ESM and CommonJS.

**[▶ Try the live demo](https://cassiiopeia.github.io/pro-sejong-auth/#demo)** · one portal login per call (about 0.7 s) · same results as the Java and Python packages · stops at the first wrong password · checked against the live portal every day

```bash
npm install pro-sejong-auth
```

```js
import { authenticate, SejongAuthError, ErrorCode } from 'pro-sejong-auth';

try {
  const result = await authenticate('21011234', 'password');
  console.log(result.name, result.major, result.grade);
} catch (error) {
  if (error instanceof SejongAuthError && error.code === ErrorCode.AUTHENTICATION_FAILED) {
    console.log('Wrong student ID or password');
  } else if (error instanceof SejongAuthError && error.code === ErrorCode.ACCOUNT_LOCKED) {
    console.log('Portal account locked: reset the password on the portal');
  }
}
```

| Function | Returns |
|---|---|
| `authenticate(id, pw)` | student info + classic reading + contact (contact is `null` if that lookup fails) |
| `authenticateDhc(id, pw, { includeRaw })` | student info + classic reading |
| `authenticateSis(id, pw, { includeRaw })` | student info + email, phone, English name |

Settings: `new SejongAuthClient({ timeoutSeconds: 10, maxRetry: 3, sslVerification: true })`. Server-side only (Node 18+), not for browsers.

Error codes (`error.code`): `AUTHENTICATION_FAILED`, `ACCOUNT_LOCKED`, `ACCOUNT_UNAVAILABLE`, `INVALID_INPUT`, `CONNECTION_FAILED`, `CONNECTION_TIMEOUT`, `SESSION_ERROR`, `DATA_FETCH_FAILED`, `PARSE_ERROR`, `SSL_CONFIGURATION_ERROR`. Branch on the code, not the message.

> **Account lock.** The Sejong portal locks an account after repeated wrong passwords (about 5). Only a password reset at <https://portal.sejong.ac.kr/jsp/inquiry/pwinquiry.jsp> unlocks it. The library never retries a rejected login. If anyone can type a student ID into your service, limit failed attempts per student ID.

Not an official Sejong University product.

Docs, live demo and Java / Python versions: https://cassiiopeia.github.io/pro-sejong-auth/
