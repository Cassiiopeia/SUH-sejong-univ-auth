<div align="center">

<img src="docs/assets/sejong-seal.png" alt="Sejong University seal" width="120">

# SUH Sejong Univ Auth

**Sejong University portal authentication for Spring Boot, in three lines of Java**

[한국어](README.md) | English

[![Nexus](https://img.shields.io/nexus/r/kr.suhsaechan/sejong-univ-auth?server=https%3A%2F%2Fnexus.suhsaechan.kr&label=Nexus&color=C20F2F)](https://nexus.suhsaechan.kr/#browse/browse:maven-releases:kr%2Fsuhsaechan%2Fsejong-univ-auth)
[![Java](https://img.shields.io/badge/Java-17+-324D60)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-324D60)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-324D60)](LICENSE)

### [▶ Try it live in your browser](https://cassiiopeia.github.io/SUH-sejong-univ-auth/#demo)

</div>

---

Verify a Sejong University student with just a student ID and password, and get their major, year,
classic-reading certification and contact info. The library handles the portal login, SSO redirects,
session cookies and HTML/JSON parsing.

```java
SejongAuthResult result = sejongAuthEngine.authenticate("studentId", "password");
result.getName();    // "홍길동"
result.getMajor();   // "컴퓨터공학과"
```

<p align="center">
  <a href="https://cassiiopeia.github.io/SUH-sejong-univ-auth/#demo">
    <img src="docs/assets/demo-en-dark.png" alt="Live authentication demo on the project page" width="820">
  </a>
  <br>
  <sub>Live demo on the project page. A real authentication result with personal fields replaced by sample values.</sub>
</p>

## Contents

- [Use cases](#use-cases)
- [Quick start](#quick-start)
- [Usage](#usage)
- [Result fields](#result-fields)
- [Error codes](#error-codes)
- [Configuration](#configuration)
- [FAQ](#faq)
- [Contributing](#contributing)

## Use cases

- **Verifying Sejong students** when they join a club, student council or campus community
- Checking major or year for campus events
- Study helpers that show classic-reading certification progress

| Source | Domain | Data |
|---|---|---|
| **DHC** (Daeyang Humanity College) | classic.sejong.ac.kr | Student info + classic-reading certification |
| **SIS** (Student Information System) | sjpt.sejong.ac.kr | Student info + email, phone, English name |

## Quick start

> Published to the **SUH Nexus repository**, not Maven Central. Add the repository first.

**1. Add the repository**

```groovy
// build.gradle
repositories {
    mavenCentral()
    maven { url "https://nexus.suhsaechan.kr/repository/maven-releases/" }
}
```

<details>
<summary>Maven</summary>

```xml
<repositories>
    <repository>
        <id>suh-nexus</id>
        <url>https://nexus.suhsaechan.kr/repository/maven-releases/</url>
    </repository>
</repositories>

<dependency>
    <groupId>kr.suhsaechan</groupId>
    <artifactId>sejong-univ-auth</artifactId>
    <version>1.2.7</version>
</dependency>
```

</details>

**2. Add the dependency** (see the Nexus badge above for the latest version)

```groovy
dependencies {
    implementation "kr.suhsaechan:sejong-univ-auth:1.2.7"
}
```

**3. Inject and call**

Spring Boot auto-configuration registers a `SuhSejongAuthEngine` bean. No extra configuration is needed.

```java
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final SuhSejongAuthEngine sejongAuthEngine;

    @PostMapping("/auth/sejong")
    public SejongAuthResult authenticate(@RequestParam String studentId,
                                         @RequestParam String password) {
        return sejongAuthEngine.authenticate(studentId, password);
    }
}
```

## Usage

Pick the method for the data you need. Integrated auth still returns student info when the SIS (contact) lookup fails.

| Method | Use | Returns |
|---|---|---|
| `authenticate(studentId, password)` | Integrated: student info + classic reading + contact | `SejongAuthResult` |
| `authenticateWithDHC(studentId, password)` | Student info + classic reading (guaranteed) | `SejongDhcAuthResult` |
| `authenticateWithSIS(studentId, password)` | Student info + contact (guaranteed) | `SejongSisAuthResult` |
| `authenticateWithDHCRaw(studentId, password)` | DHC + raw HTML (debugging) | `SejongDhcAuthResult` |
| `authenticateWithSISRaw(studentId, password)` | SIS + raw JSON (debugging) | `SejongSisAuthResult` |

**Check classic reading**

```java
SejongDhcAuthResult result = sejongAuthEngine.authenticateWithDHC(studentId, password);

for (ClassicCertification certification : result.getClassicReading().getCertifications()) {
    System.out.println(certification.getArea() + " "
        + certification.getCertifiedCount() + "/" + certification.getRequiredCount());
}
```

**Handle failures**: a failed authentication throws `SejongAuthException`.

```java
try {
    sejongAuthEngine.authenticate(studentId, password);
} catch (SejongAuthException exception) {
    switch (exception.getErrorCode()) {
        case AUTHENTICATION_FAILED -> // wrong student ID or password
        case CONNECTION_FAILED, CONNECTION_TIMEOUT -> // portal outage
        default -> // parsing or session error
    }
}
```

## Result fields

Fields of the integrated result `SejongAuthResult`. Text values come from the portal as-is (Korean).

| Field | Type | Description |
|---|---|---|
| `success` | boolean | Whether authentication succeeded |
| `studentId` | String | Student ID |
| `name` | String | Name |
| `major` | String | Major |
| `grade` | String | Year |
| `status` | String | Enrollment status (재학 enrolled, 휴학 on leave, 졸업 graduated) |
| `classicReading` | SejongClassicReading | Classic-reading certification, exam, contest and substitution records |
| `email` | String | Email (null if SIS fails) |
| `phoneNumber` | String | Phone, `010-1234-5678` format (null if SIS fails) |
| `englishName` | String | English name (null if SIS fails) |
| `authenticatedAt` | LocalDateTime | Authentication time |

- `SejongDhcAuthResult`: no contact fields; includes `rawHtml` with the Raw method
- `SejongSisAuthResult`: no classic-reading field, `grade` and `status` are null; includes `rawJson` with the Raw method

## Error codes

| Code | Meaning |
|---|---|
| `AUTHENTICATION_FAILED` | Student ID or password is wrong |
| `INVALID_INPUT` | Input is empty or malformed |
| `CONNECTION_FAILED` | Cannot reach the Sejong portal |
| `CONNECTION_TIMEOUT` | Portal timed out |
| `DATA_FETCH_FAILED` | Failed to fetch student info |
| `PARSE_ERROR` | Could not parse the portal response (structure changed?) |
| `SESSION_ERROR` | Portal session error |
| `SSL_CONFIGURATION_ERROR` | SSL configuration error |

## Configuration

All optional, in `application.yml`:

```yaml
sejong:
  auth:
    ssl-verification: true   # verify the portal certificate (default true)
    timeout-seconds: 10      # request timeout (default 10)
    max-retry: 3             # retry count (default 3)
```

Requirements: **Java 17+**, **Spring Boot 3.x**

For internals, see the [DHC login flow](docs/DHC_LOGIN_FLOW.md) and [SIS login flow](docs/SIS_LOGIN_FLOW.md) (Korean).

## FAQ

<details>
<summary><b>Is the password stored?</b></summary>

No. The library only uses it for the portal login request. The demo server behind the project page also returns the result without storing input.
In your own service, never log or persist passwords.
</details>

<details>
<summary><b>Is this an official Sejong University library?</b></summary>

No. It is a personal open-source project by a Sejong student and is not affiliated with the university.
</details>

<details>
<summary><b>Authentication suddenly fails</b></summary>

Try the [demo](https://cassiiopeia.github.io/SUH-sejong-univ-auth/#demo) first. If the demo also fails, the portal has probably changed.
Please open an [issue](https://github.com/Cassiiopeia/SUH-sejong-univ-auth/issues) with the error code.
</details>

## Contributing

Bug reports, portal-change fixes and docs improvements are all welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) for build and test steps.

## License

[MIT](LICENSE) © 2025 Suh Saechan

---

<sub>This project is not an official product of Sejong University and is not affiliated with it. The Sejong University seal belongs to Sejong University.</sub>
