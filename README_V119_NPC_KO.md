# MURIM LIFE v1.19 NPC 생성 준비 통합 개발본

## 구현 범위(실제 파일 수정)

원본: **v1.18.1 단일 모드 MDK 프로젝트**. 모드 ID는 계속 `murimlife` 하나. 별도의 NPC 모드/JAR 없음.

- 기존 NPC PNG **96개** 및 `models_source/murim_npc.geo.json` 원본 보존.
- 기존 이름/문파/성향/무기/성장 계수 데이터 **96개**를 `src/main/resources/murimnpc/legacy_profiles_96.json`으로 복사.
- 사용 목적을 구분한 **83개 NPC 역할** 생성 정책: `npc_role_catalog.json`.
- 기존 월드 생성 목록 **38개 거점·377개 건물 조각**의 실제 NBT에서 바닥/공중 2블록 확인 후 **864개 후보 생성 위치** 추출: `npc_site_spawn_candidates.json`.
- 기존 작은 자연 생성 Jigsaw **17개 템플릿**의 별도 로컬 후보 및 회전 검증 필요 규칙: `npc_natural_jigsaw_candidates.json`.
- 문파/세가별 소속·역할 후보 표: `npc_faction_rosters.json`.
- 인구 상한/중복 UUID 방지/접근 거리/하루 일정/활성화 규칙: `npc_spawn_policy.json`.
- 기존 `MurimProgressData`의 96개 가상 무림인에게 실제 준비된 이름·문파 프로필을 적용하고, 과거 `무림인-숫자` 임시 이름은 **기존 진행/성장 데이터는 건드리지 않고 이름과 기본 소속만 1회 동기화**. 커스텀 이름을 사용 중인 NPC는 덮어쓰지 않음.
- Java `NpcSpawnRules` (순수 생성 조건/좌표/UUID 판단) + `NpcSeedProfiles` (Gson JSON 프로필 읽기) 추가.

## 검증

- Java 21 독립 테스트 **16개 통과** (`NpcSpawnRulesTest`).
- Python 정적 검사: 역할 83개/프로필 96개/누락 스킨 0/거점 38개/후보지 864개/작은 자연 생성 템플릿 17개 검사 통과.
- 검사는 실제 NBT 블록을 읽고 각 후보의 **발밑 바닥 + 머리 위 빈 공간 2칸**을 점검함.
- NeoForge 전체 컴파일, 실제 엔티티 렌더링/충돌, 길찾기, 자연 월드에서 소환은 **아직 미검증**.

## 이것은 아직 완성된 NPC 생성 모드가 아님

현재 NPC를 **실제로 소환하는 `EntityType`, Renderer, Spawn Hook, 서버 NPC 인공지능, 실체/가상 상태 전환, 대화 UI** 등은 아직 구현되지 않았음. 기존 프로필 96개가 가상 세계에서 수련·성장하는 로직은 유지되지만, 이들 96개를 마인크래프트 엔티티로 전환하지 않는다.

`npc_spawn_policy.json`의 `enabled=false`를 유지한다. 단순히 JSON을 `true`로 수정해도 소환 기능이 활성화되는 것은 아님(소환 후크 미구현).

## 생성 시스템 구현 순서

1. NeoForge 1.21.1 `EntityType`/NPC 인적 모델·텍스처 렌더러를 본체 `murimlife`에 등록.
2. 중립 NPC의 서버 이동/작업/대화·경계 상태를 개발. NPC끼리는 논쟁만으로 바로 싸우지 않고 갈등 수치와 주변 경비 개입이 필요.
3. `SiteLedger.Stage.COMPLETE` 및 로드된 청크와 NPC 생성 좌표를 연결, 실시간 블록/엔티티 충돌 및 지면/길찾기 재확인.
4. 거점 NPC UUID를 `SavedData`에 영구 저장해 재접속/청크 언로드/장문인 중복 생성 방지.
5. 정파·오대세가 복장/무기·역할별 일정과 직업·거래를 연결.
6. NPC 계급/경지 성장, 무공/비무/입문/결혼·동료/대회/사건 연계.
7. NPC 성능 제한(근거리 실체화, 원거리 가상화), Jigsaw 회전 적용 및 큰 본산 월드 생성 통합 시험.

## 아직 결정되지 않은 사실

- v1.18.1 `sites.json`에는 **38개 건축 계획**이 있으나 천마신교·혈교·사도련의 **전체 본산 생성 계획**은 아직 없음. 구파일방 10곳+오대세가 5곳의 고유 본산은 포함되어 있음.
- 일반 문파 20곳의 정파/사파 개별 분류를 임의로 확정하지 않고 `GENERIC_SECT`로 표시함. 실제 설정 확인 후 소속 정리 필요.
- 현재 96개 NPC는 기존 생성 프로필 예시일 뿐, 각 문파 장문인/가주/교주의 확정 이름표나 월드에서 각각 하나만 등장하는 고유 보스 목록이 아님.
- Jigsaw 17개 템플릿은 회전 가능한 구조이므로 **로컬 후보 좌표를 월드 좌표로 옮길 때 실제 회전 및 블록 충돌을 재검사**해야 함.

## 사용자 Windows 개발/시험

- `QA_NPC_WINDOWS.cmd`: Java 21 및 Python 3으로 NPC 준비 자료의 정적 검사 수행.
- `BUILD_WINDOWS.cmd`: 단일 `murimlife` 모드 전체 빌드(인터넷/Gradle/NeoForge 의존성 필요).
- 결과 예상: `build/libs/murimlife-0.1.19-npc-prep-alpha.jar` (빌드 성공 시에만 생성).
- **이 개발본만으로 NPC가 월드에 소환되지는 않음**. 기존 v1.18.1 생성 엔진의 미검증 위험도 그대로이므로 자동 거대 문파 생성은 비활성 유지.
