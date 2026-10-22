# pro-sejong-auth (Python)

**Sejong University login API for Python** · 세종대 로그인 API

Verify a Sejong University student with a student ID and password through the portal SSO, and get their major, year, classic-reading certification and contact info. No dependencies beyond the standard library.

```bash
pip install pro-sejong-auth
```

```python
from sejong_auth import authenticate, SejongAuthError, ErrorCode

try:
    result = authenticate("21011234", "password")
    print(result.name, result.major, result.grade)
except SejongAuthError as error:
    if error.code == ErrorCode.AUTHENTICATION_FAILED:
        print("Wrong student ID or password")
    elif error.code == ErrorCode.ACCOUNT_LOCKED:
        print("Portal account locked: reset the password on the portal")
```

| Function | Returns |
|---|---|
| `authenticate(id, pw)` | student info + classic reading + contact (contact is `None` if that lookup fails) |
| `authenticate_dhc(id, pw, include_raw=False)` | student info + classic reading |
| `authenticate_sis(id, pw, include_raw=False)` | student info + email, phone, English name |

Settings: `SejongAuthClient(timeout_seconds=10, max_retry=3, ssl_verification=True)`.

Error codes (`error.code`): `AUTHENTICATION_FAILED`, `ACCOUNT_LOCKED`, `ACCOUNT_UNAVAILABLE`, `INVALID_INPUT`, `CONNECTION_FAILED`, `CONNECTION_TIMEOUT`, `SESSION_ERROR`, `DATA_FETCH_FAILED`, `PARSE_ERROR`, `SSL_CONFIGURATION_ERROR`. Branch on the code, not the message.

> **Account lock.** The Sejong portal locks an account after repeated wrong passwords (about 5). Only a password reset at <https://portal.sejong.ac.kr/jsp/inquiry/pwinquiry.jsp> unlocks it. The library never retries a rejected login. If anyone can type a student ID into your service, limit failed attempts per student ID.

Requires Python 3.9+. Not an official Sejong University product.

Docs, live demo and Java / Node versions: https://cassiiopeia.github.io/pro-sejong-auth/
