#!/usr/bin/env bash
# 세 언어(Java, Python, Node) 실제 포털 통합 인증 결과 교차 검증
#   필드별 해시만 비교 (개인정보 출력 없음), 틀린 비밀번호는 세 언어 모두 AUTHENTICATION_FAILED 여야 함
#   사용: SEJONG_TEST_STUDENT_ID=... SEJONG_TEST_PASSWORD=... scripts/verify-live.sh
set -euo pipefail
cd "$(dirname "$0")/.."

: "${SEJONG_TEST_STUDENT_ID:?SEJONG_TEST_STUDENT_ID not set}"
: "${SEJONG_TEST_PASSWORD:?SEJONG_TEST_PASSWORD not set}"

# 1. 언어별 결과 수집
./gradlew -q :sejong-univ-auth:test --tests '*CrossLanguageSummaryTest' --rerun-tasks
(cd node && npm run --silent build)
OUT=java/build/cross-language
python3 scripts/cross-language/summarize.py > "$OUT/python.json"
python3 scripts/cross-language/summarize.py --wrong-password > "$OUT/python-wrong.json"
node scripts/cross-language/summarize.mjs > "$OUT/node.json"
node scripts/cross-language/summarize.mjs --wrong-password > "$OUT/node-wrong.json"

# SIS 단독 틀린 비밀번호 (200 + _SUBMIT_ERROR_ 경로)
python3 -c "import os,sys,json; sys.path.insert(0,'python/src'); from sejong_auth import authenticate_sis, SejongAuthError
try: authenticate_sis(os.environ['SEJONG_TEST_STUDENT_ID'], os.environ['SEJONG_TEST_PASSWORD']+'-wrong'); print(json.dumps({}))
except SejongAuthError as e: print(json.dumps({'error': e.code.value}))" > "$OUT/python-sis-wrong.json"
node --input-type=module -e "import { authenticateSis } from './node/dist/esm/index.js';
try { await authenticateSis(process.env.SEJONG_TEST_STUDENT_ID, process.env.SEJONG_TEST_PASSWORD + '-wrong'); console.log('{}'); }
catch (error) { console.log(JSON.stringify({ error: error.code })); }" > "$OUT/node-sis-wrong.json"

# 2. 비교 (다르면 다른 필드 이름만 출력 후 exit 1)
python3 - "$OUT" <<'PY'
import json, sys
out = sys.argv[1]
load = lambda name: json.load(open(f"{out}/{name}.json"))
results = {language: load(language) for language in ("java", "python", "node")}
wrong = {language: load(f"{language}-wrong") for language in ("java", "python", "node")}
wrong.update({f"{language} sis": load(f"{language}-sis-wrong") for language in ("java", "python", "node")})
is_failed = False
for language, summary in results.items():
    if "error" in summary:
        print(f"{language}: authentication error {summary['error']}"); is_failed = True
if not is_failed:
    for field in results["java"]["fields"]:
        hashes = {language: results[language]["fields"].get(field) for language in results}
        status = "same" if len(set(hashes.values())) == 1 else "DIFFERENT"
        print(f"{field:15} {status}")
        is_failed |= status != "same"
for language, summary in wrong.items():
    is_expected = summary.get("error") == "AUTHENTICATION_FAILED"
    print(f"wrong password {language:10} {'AUTHENTICATION_FAILED' if is_expected else summary}")
    is_failed |= not is_expected
sys.exit(1 if is_failed else 0)
PY
echo "cross-language verification passed"
