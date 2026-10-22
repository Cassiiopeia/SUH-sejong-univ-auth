// dist/cjs 를 CommonJS 로 표시 (루트 package.json 이 type: module)
import { writeFileSync } from 'node:fs';
writeFileSync(new URL('../dist/cjs/package.json', import.meta.url), '{"type":"commonjs"}\n');
