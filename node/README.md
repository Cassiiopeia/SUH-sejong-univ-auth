# pro-sejong-auth (Node.js)

**Sejong University login API for Node.js** · 세종대 로그인 API

Verify a Sejong University student with a student ID and password through the portal SSO, and get their major, year, classic-reading certification and contact info. Zero runtime dependencies, TypeScript types included, ESM and CommonJS.

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
  }
}
```

| Function | Returns |
|---|---|
| `authenticate(id, pw)` | student info + classic reading + contact (contact is `null` if that lookup fails) |
| `authenticateDhc(id, pw, { includeRaw })` | student info + classic reading |
| `authenticateSis(id, pw, { includeRaw })` | student info + email, phone, English name |

Settings: `new SejongAuthClient({ timeoutSeconds: 10, maxRetry: 3, sslVerification: true })`. Server-side only (Node 18+), not for browsers.

Not an official Sejong University product.

Docs, live demo and Java / Python versions: https://cassiiopeia.github.io/pro-sejong-auth/
