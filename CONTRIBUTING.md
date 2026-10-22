# Contributing

English | [한국어](CONTRIBUTING.ko.md)

Bug reports, fixes for portal changes and docs improvements are all welcome. Issues and PRs in English or Korean are fine.

Please do not report security vulnerabilities in public issues. Use the private reporting described in [SECURITY.md](SECURITY.md).

## Start with an issue

- Bugs: include the error code (`SejongAuthException.getErrorCode()`) and the method you called. Never post student IDs, passwords or other personal data.
- If authentication broke because the portal changed, first check whether the [live demo](https://cassiiopeia.github.io/pro-sejong-auth/#demo) reproduces it.
- For larger changes, agree on the direction in an issue before opening a PR.

## Development setup

- JDK 17 or later
- The Gradle Wrapper is included (no separate install needed)

```bash
./gradlew build      # compile + unit tests
./gradlew test       # unit tests only
```

## Tests against the real portal

The `실제인증_*` tests in `SuhSejongAuthEngineTest` log in to the real Sejong portal, so they are `@Disabled` by default.

1. Create `src/test/resources/application-dev.yml` (it is in `.gitignore`, so it will not be committed)

   ```yaml
   sejong:
     auth:
       test:
         student-id: "your student ID"
         password: "your password"
   ```

2. Remove `@Disabled` locally, run the tests, and restore it before committing.

   ```bash
   ./gradlew test --tests '*SuhSejongAuthEngineTest'
   ```

> Several wrong passwords in a row can lock the portal account. If a run fails, find the cause before running it again.

## Pull requests

- Branch from `main`.
- Add tests when behavior changes. For changes to response parsing, add a parser test with an HTML or JSON sample (personal data removed).
- Make sure no password or personal data ends up in logs or exception messages.

## License

Contributions are released under the [MIT License](LICENSE).
