# MURIM LIFE v1.21 Epic Fight / NPC / 合 교차 검수

**실제 NeoForge 전체 Gradle 컴파일: 미실행. Minecraft 런타임/에픽파이트 애니메이션: 미검증.**

## 실제 확인한 사항

- 기존 v1.20 통합 MDK를 기반으로 본체 기능·월드젠·문파·건축물·기존 주민 관련 코드 보존. 모드 ID 하나 `murimlife`.
- Epic Fight 실제 사용자 제공 JAR `21.17.3.1` 내부 클래스 및 메서드 심볼을 `javap`로 확인: `EntityPatchRegistryEvent.registerEntityPatch`, `EpicFightEventHooks.Registry.ENTITY_PATCH`, `EpicFightClientEventHooks.Registry.ADD_PATCHED_ENTITY`, `HumanoidMobPatch`, `AnimatedAttackGoal`, `Armatures.BIPED`, `Meshes.BIPED`.
- `MurimHumanoid` 엔티티 등록, 속성 등록, 서버 저장(NBT), 클라이언트 렌더러, 스킨 인덱스 동기화 코드.
- `MurimEpicPatch`의 Epic Fight 기본 Idle/Walk/Run/Fall/Jump/Death 및 2가지 한손검 공격 애니메이션 AI 등록 코드.
- 비무장의 가짜 35틱 타격 제거, 실제 NPC 목표·Epic Fight 공격 AI 경로로 전환. 관중 48명 기본/80명 상한 데이터 유지.
- 합(合): 무기 소지, 거리 3.8블록 이내, 공격 시작 5틱 이내, 양쪽 바라보는 방향을 확인해 서버 피해 취소. 이펙트 CRIT/ENCHANTED_HIT.
- 머리/몸통 회전, 이동 속도를 점진적으로 변경하는 `MotionBlend`(MURIM NPC만 변경) 통합.
- 16명 지도자(구파일방 10 + 오대세가 5 + 사도련주 1) 및 문파 내부 330명에게 각 1개씩 독립 스킨 ID 부여, 일반 NPC 96개 추가. 442개 64x64 PNG, SHA256 중복 0.
- 사도련주 독고천악(獨孤天岳) 강자 능력치·유니크 스킨 ID 15 부여. 다른 사용자가 이미 사도련주를 생성했다면 중복 등록 안 함.
- 순수 Java `MotionBlend` 및 `ClashRules` 테스트 **18개** 통과, Java 소스 **43개** 파서 문법 검사 통과, 635개 기존 구조물 NBT 리소스 보존 정적 검사 통과.

## 남은 개발/불확실성

1. `Gradle build` + `runClient` + `runServer` 성공 여부 직접 테스트 필요. 컴파일 API 차이가 있으면 `build_log_unified.txt` 제공 필요.
2. 진짜 3D 도포·머리 모양, 동작별 뿌리 이동(root motion), 261개 무공별 고유 에픽파이트 모션은 아직 미구현.
3. NPC 인공지능은 기본 목표 추적과 Epic Fight 일반 검격/피격만 연결한 시험판. 전략적 포위, 파벌, 내공 대결, 합 파열, 단계별 스턴, 장문인 개별 콤보 등은 후속 작업.
4. 일반 NPC 96개 외형과 네임드 스킨은 텍스처를 실제로 포함했으나, **실제 Minecraft 렌더링·클리핑은 미검증**.
5. `enablePersistentResidents` / `enableTournamentDemo`는 기존과 같이 기본 비활성. 대형 구조물 자동 생성 기능도 기본 비활성. 별도 백업 시험 월드에서만 활성.
6. 구버전에서 이미 주민 형태로 저장된 NPC는 자동 교체하지 않고 보존. 새로 생성되는 NPC에 새 스킨/모델 적용.

**주의: 소스 ZIP은 mods 폴더에 넣지 않고 `BUILD_WINDOWS.cmd`로 직접 빌드해야 합니다. Epic Fight는 기존 설치한 외부 모드로 그대로 필요합니다.**
