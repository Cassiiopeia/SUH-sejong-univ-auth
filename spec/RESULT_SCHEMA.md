# Result schema and error codes (single source for all languages)

Java is the reference. Python uses snake_case, Node uses camelCase (same as Java).

## Operations

| Operation | Java | Python | Node |
|---|---|---|---|
| Integrated (DHC + SIS contacts) | `authenticate(id, pw)` | `authenticate(id, pw)` | `authenticate(id, pw)` |
| DHC | `authenticateWithDHC` / `authenticateWithDHCRaw` | `authenticate_dhc(id, pw, include_raw=False)` | `authenticateDhc(id, pw, { includeRaw })` |
| SIS | `authenticateWithSIS` / `authenticateWithSISRaw` | `authenticate_sis(id, pw, include_raw=False)` | `authenticateSis(id, pw, { includeRaw })` |

Input check first: blank student id or password raises `INVALID_INPUT` before any network call.

## Integrated result

| Java / Node | Python | Type | Rule |
|---|---|---|---|
| success | success | boolean | always true when returned |
| studentId | student_id | string | from DHC |
| name | name | string | from DHC |
| major | major | string | from DHC |
| grade | grade | string | from DHC |
| status | status | string | from DHC |
| classicReading | classic_reading | object | from DHC |
| email | email | string or null | from SIS; null if the SIS lookup fails for any reason |
| phoneNumber | phone_number | string or null | same |
| englishName | english_name | string or null | same |
| authenticatedAt | authenticated_at | datetime (local time) | |

A failing DHC step fails the whole call. A failing SIS step never fails the integrated call.

## DHC result

`success, studentId, name, major, grade, status, classicReading, authenticatedAt, rawHtml` (rawHtml only with the raw option, otherwise null)

## SIS result

`success, studentId, name, major, grade (always ""), status (always ""), email, phoneNumber, englishName, authenticatedAt, rawJson` (rawJson only with the raw option)

## classicReading

| Field | Item fields |
|---|---|
| certifications | area, requiredCount, certifiedCount |
| examRecords | semester, area, bookTitle, examDate, score, passStatus |
| subjectSubstitutions | semester, subjectName, area, bookTitle, completion |
| contestRecords | semester, contestName, area, bookTitle |
| curriculumRecords | semester, subjectName, area, bookTitle, completion |

Python item fields are snake_case (`required_count`, `book_title`, ...).

## Error codes

All languages raise one error type carrying `code` (string, Java enum name) and a message in English.

| Code | Meaning |
|---|---|
| `INVALID_INPUT` | Student ID or password is blank |
| `AUTHENTICATION_FAILED` | Wrong student ID or password (DHC: 401, SIS: 200 with `_SUBMIT_ERROR_` LOGOUT) |
| `CONNECTION_FAILED` | Network error, or login retries exhausted |
| `CONNECTION_TIMEOUT` | Timed out on the last login attempt |
| `SESSION_ERROR` | SSO step returned non-2xx |
| `DATA_FETCH_FAILED` | Data page returned non-200 or empty |
| `PARSE_ERROR` | Response structure not as expected |
| `SSL_CONFIGURATION_ERROR` | TLS setup failed |

Java also keeps the deprecated `INVALID_CREDENTIALS`, which is never thrown and does not exist in Python or Node.

## Defaults

| Setting | Java | Python | Node | Default |
|---|---|---|---|---|
| timeout | `sejong.auth.timeout-seconds` | `timeout_seconds` | `timeoutSeconds` | 10 |
| retries | `sejong.auth.max-retry` | `max_retry` | `maxRetry` | 3 |
| certificate check | `sejong.auth.ssl-verification` | `ssl_verification` | `sslVerification` | true |
