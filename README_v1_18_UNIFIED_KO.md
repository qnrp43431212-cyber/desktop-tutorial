# v1.18.1 사후 안전검수 수정 사항

자세한 수정사항은 `QA_V1181_KO.md` 참조. 이전 README v1.18은 같은 폴더에 그대로 있음.

# MURIM LIFE v1.18 — 단일 모드 통합 MDK (알파)

Minecraft Java **1.21.1 / NeoForge 21.1.x / Java 21**.

## 이번 작업에서 실제로 통합한 것

- **모드 ID는 `murimlife` 단 하나** (`@Mod` 1개, `neoforge.mods.toml` 1개).
- 이전 v1.12 본체: 캐릭터 진행·아이템·무공/상태창 프로토타입·복장 등의 소스/리소스 그대로 유지.
- 이전 v1.16 Architecture: 네임스페이스 `murimlife_arch`와 `murimlife_city`의 **344개 NBT**, 함수 및 Jigsaw 구조물 자연생성 데이터 12개 등 **JAR 내부 리소스로 흡수**.
- 이전 v1.17 Worldgen Engine: `PlanningCore`, `SiteCatalog`, `SiteLedger`, `WorldSiteSafety`, `WorldgenConfig`, `WorldgenEngine`을 `src/main/java/kr/murimlife/worldgen/`에 흡수.
- 단독 `@Mod("murimlife_worldgen")` 클래스는 제거. 본체 `MurimLife`에서 `WorldgenEngine::onTick`을 등록.
- 기존 구조물 명령어의 `murimlife_arch:...` 경로는 호환을 위해 유지. **네임스페이스와 설치 모드는 다름.**

## 빌드

1. JDK 21 설치, Windows에서 프로젝트 압축 해제.
2. `BUILD_WINDOWS.cmd` 실행. 최초 Gradle 다운로드 때문에 인터넷 필요.
3. 빌드 성공 시 `build/libs/murimlife-0.1.18-unified-alpha.jar` 한 개 생성.
4. `mods`에서 구버전 MURIM LIFE 본체 JAR, 모든 구버전 Architecture JAR, 별도 Worldgen Engine JAR을 제거.
5. 새 통합 JAR **한 개만** 넣기. 별도 v1.16 데이터팩 ZIP을 월드에 넣지 말 것.

다른 모드로 제공하는 Epic Fight·GeckoLib 등은 사용자의 기존 모드팩 기능에 필요할 수 있으나 이 파일에 무단 내장하지 않았음. '하나의 모드'는 **MURIM LIFE 프로젝트가 제공하는 JAR이 하나**라는 의미.

## 안전

- `config/murimlife-worldgen.json`의 **`enableLargeSiteGeneration=false` 기본값 유지**. 검증 전 자동 대형 문파 설치 비활성.
- 소규모 정착지 Jigsaw 자연생성 데이터는 포함되어 있으나 실제 게임에서 미검증.
- 대형 세계당 네임드 *최대* 한 곳 예약 로직만 구현. 모든 문파의 *정확히 한 곳 생성* 보장 불가.
- 지형과 타 모드 구조물 100% 충돌 방지·플레이어 건축물 보호·20~28청크 전체 본산 확장은 미완성.
- 월드 백업 없이 자동 건축 활성화 금지. 이전 월드에서 돌리기보다는 새 시험 월드 사용.

## 검증 상태

- 기존 v1.16 JAR의 구조물/월드생성 리소스 누락 없이 복사한 것을 ZIP 목록과 SHA-256으로 검사.
- Java 21의 독립 `PlanningCoreTest` 테스트와 구조물 카탈로그 참조 검사 수행.
- **현재 환경은 외부 Gradle/NeoForge 의존성을 받지 못해 통합 프로젝트의 전체 NeoForge 빌드 및 게임 실행을 검증하지 못했음.** 소스 ZIP만 제공하며 완성된 실행 JAR이라고 주장하지 않음.
- 빌드 실패: `build_log_unified.txt`, 게임 오류: `latest.log`와 크래시 로그를 제공.

## 앞으로

본체·자연생성 엔진·마을·도시·문파·아이템·무공·NPC 모두 단일 `murimlife` 모드 프로젝트에서 추가. 새 Architecture/Worldgen 모드는 **다시 분리하지 않는 구조**.
