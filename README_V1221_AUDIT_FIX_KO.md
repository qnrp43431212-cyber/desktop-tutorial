# MURIM LIFE v1.22.1 — 소스 오류 정밀 점검 및 수정본

**대상:** Minecraft 1.21.1 · NeoForge 21.1.235 · Java 21 · Epic Fight 21.17.3.1
**모드 ID:** `murimlife` (기존과 동일, 단일 통합 모드)
**빌드 버전:** `0.1.22.1-audit-fix`

> 이 ZIP은 JAR가 아니라 **개발 소스(MDK)** 입니다. 별도로 Gradle 빌드해 나온 JAR 한 개를 `mods` 폴더에 설치해야 합니다. 기존 세이브 월드 백업 후 테스트하십시오. **이 환경에서는 Gradle 배포판 다운로드에 실패하여 NeoForge 전체 빌드 및 Minecraft 구동 검증을 하지 못했습니다.**

## 고친 문제

| 우선순위 | 위치 | 확인된 문제 | 변경 |
|---|---|---|---|
| 치명적 | `MurimHumanoid.java` | `WEAPON_STYLE` 필드 2개, getter/setter 중복 선언, 같은 동기화 데이터 2회 정의 | 1개만 유지, 컴파일 충돌 제거 |
| 치명적 | `MurimNetwork.java` | `TalkRequest` record 중복 선언 및 동일 C2S 패킷 2회 등록 | record와 핸들러 각각 1개로 정리 |
| 치명적 | `ClientEvents.java` | `TALK` 키 필드 중복 선언, 키 등록·입력 처리 2회 | 각각 1회로 정리 |
| 치명적 | `NpcArenaService.java` | `finishByNegotiation()` 동일 메서드 중복 선언 | 1개만 유지 |
| 높음 | `MurimLife.java` | 대화 서버 틱/피해 이벤트 리스너 각 2회 등록 | 한 번씩만 등록 |
| 높음 | `NpcAdminCommands.java` | 대화 명령 등록 2회 | 한 번만 등록 |
| 높음 | `MurimHumanoid.java` | 무기 NBT 저장/불러오기 및 대화 일시정지 복원 분기 중복 | 중복 제거, 기존 저장 필드명 유지 |
| 높음 | `DialogueRules` / `MurimDialogueService` | 서로 공격하는 쌍만 무효화해야 하는데 자기 피해도 무효화될 수 있음 | 진짜 플레이어↔NPC 상호 공격에만 한정 |
| 중간 | `ClientEvents.java` | 바닐라 기본 조준 거리에 묶여 `6블록` 대화 요청 안내와 불일치 | 블록에 가로막히는 별도 6블록 NPC 조준 추가 |
| 중간 | `MurimDialogueService.java` | 오래된 NPC 대화 요청 타임스탬프가 서버 가동 중 계속 증가 | 주기적 오래된 기록 정리 및 이미 다른 적과 싸우는 NPC 재확인 |
| 중간 | `NpcResidentService.java` | 매 사이트마다 전체 NPC 수 및 후보 배열 반복 순회 | 스폰 틱마다 한 번 카운트, 사이트별 후보 캐시 |
| 중간 | 기존 QA | v1.19의 직업 수 83과 v1.16의 NBT 10개로 검사해 v1.22 자료에서 실패 | 현재 직업 87개 확인, 구버전보다 늘어난 구조물 지원 및 독립 v1.22 QA 추가 |
| 중간 | `BUILD_WINDOWS.cmd` | 성공 메시지가 v1.21 JAR을 가리킴 | 실제 v1.22.1 JAR 이름으로 정정 |

## 독립 검사 결과

- Java 47개 파일 전체 구문 검사 및 필드·메서드·중첩 타입 중복 검사 통과.
- 순수 Java 전투/움직임 18개 테스트 통과.
- v1.22의 대화·선택 만료·거리·피로도·쌍방 피해·무기 스타일 관련 38개 추가 테스트 통과.
- 기존 규칙 엔진(`scripts/test_rules_v16.sh`) 및 261개 무공 자료, 275개 별호, 112개 등급 무기 자료 검사 통과.
- 87개 NPC 직업, 96개 프로필, 864개 배치 후보, 17개 자연 생성 템플릿 검사 통과.
- 구조물 NBT 635개, 무림 스킨 442개, 대화 문장 324개를 확인. **원본 v1.21 기록에 수록된 PNG/NBT 1,647개와 바이트 단위 SHA-256 일치**.
- NPC 대화·패킷·명령·리스너에 대한 중복 등록 재발 방지 검사 추가.

## 여전히 미검증 / 추후 작업

1. **실제 Gradle `build`, NeoForge 타입 검사, Epic Fight 전투 AI 클래스 연결, 클라이언트 접속/멀티플레이/지속성 검증**. 현재 환경에는 Gradle 9.2.1 배포판 캐시가 없고 `services.gradle.org` DNS 접근이 실패했습니다. 따라서 JAR의 실행 성공을 보장할 수 없습니다.
2. 대화 중 재접속·몹 청크 언로드·차원 이동·대량 NPC·외부 애드온과 연동하는 경우는 실제 게임 시험 필요.
3. 무공 261개의 **완전한 게임 내 독립 동작**, 무기 16계열의 고유 애니메이션, 30개 민간 직업의 개별 생활 AI, 진정한 NPC 자율 사회, 정규 무림대회는 이 소스가 완성했다고 보장할 수 없습니다.
4. 건축물 635개 NBT는 보존했으나 **모든 지형에서 자연 생성의 위치/회전/중복을 실제 검증하지는 않았습니다.** 자동 세계 생성과 대규모 NPC 기능은 기본 비활성입니다.
5. 기존 v1.19/v1.20/v1.21 리포트는 과거 스냅샷이므로 최신 엔티티(MurimHumanoid)를 설명하지 않는 문장이 남아 있을 수 있습니다. 최신 상태는 이 문서 및 `README_V122_DIALOGUE_WEAPON_AI_KO.md` 참고.

## 실행 순서 (Windows)

1. ZIP 압축 해제 후 `QA_V122_WINDOWS.cmd` 실행(정적 QA, Java 21 JDK·Python 3 필요).
2. `BUILD_WINDOWS.cmd` 실행(온라인 의존성 다운로드 가능해야 함). **성공 시 예상 파일:** `build\libs\murimlife-0.1.22.1-audit-fix.jar`.
3. Minecraft **1.21.1 NeoForge** 시험 월드와 별도 Epic Fight **21.17.3.1** 실행 JAR 준비.
4. `mods` 폴더에 이전 `murimlife` JAR을 모두 제거하고 위 JAR 하나만 설치. Epic Fight는 외부 JAR로 유지.
5. 게임 테스트: `/murim npc spawn shaolin`, `~` 대화, `/murim talk testinvite`, `/murim arena playernpc`; 필요하면 `config/murimlife-npc.json`에서 `enablePersistentResidents`, `enableTournamentDemo`를 켜고 재시작.
6. 실패하면 같은 폴더의 `build_log_unified.txt`, `logs/latest.log`, `crash-reports` 파일을 검토. 실제 실행 없이도 어느 코드까지 검증했는지는 `QA_V1221_REPORT_KO.md`와 `scripts/qa_v122.py`로 추적 가능.

**Linux/macOS:** `bash scripts/test_v122.sh` 다음 `bash ./gradlew --no-daemon clean build`.

### CI 참고

`.github/workflows/build.yml`은 이제 v1.22 구조·독립 규칙·데이터 검사를 빌드 전에 수행합니다. QA Python 스크립트는 외부 Pillow 설치 없이 실행되도록 변경했습니다. 전체 JAR 빌드는 GitHub Actions가 Gradle/NeoForge/Epic Fight 의존성을 성공적으로 내려받을 때 진행됩니다.
