# MURIM LIFE v1.26.1 — Java compilation hotfix (KO)

기준: 사용자가 제공한 v1.26.0 `build_log_unified.txt` (Gradle 9.2.1, NeoForge 21.1.235)

## 로그 확인
- Gradle이 Minecraft 아티팩트 준비를 완료하였고 `:compileJava`에서 `8 errors / 2 warnings`로 중단됨.
- 오류는 서로 중복 보고된 것을 포함하여 5가지 근본 원인, 4개 Java 소스에 해당함.

## 수정 사항
1. `src/main/java/kr/murimlife/MurimLife.java`: `FMLCommonSetupEvent` import를 `net.neoforged.fml.event.lifecycle` 패키지로 변경.
2. `src/main/java/kr/murimlife/npc/MurimNpcTypes.java`: NeoForge 1.21.1에 없는 `DeferredRegister.Entities/createEntities/registerEntityType` 대신 `DeferredRegister.create(Registries.ENTITY_TYPE, ...)` + `EntityType.Builder.of(...).build(...)` 사용.
3. `src/main/java/kr/murimlife/npc/MurimHumanoid.java`: `NearestAttackableTargetGoal` 생성자를 1.21.1의 4인수 형태로 변경하고 `LivingEntity`를 `Player`로 검사하여 공격 가능 여부 판정. 저장 메서드 두 개를 `public`으로 상향.
4. `src/main/java/kr/murimlife/opportunity/OpportunitySites.java`: 존재하지 않는 `Blocks.MOSSY_DEEPSLATE_BRICKS`를 `Blocks.COBBLED_DEEPSLATE`로 대체.
5. `gradle.properties`: 이 수정본을 식별하기 위해 버전 번호를 `0.1.26.1-mentorship-raids-medicine-buildfix-alpha`로 변경.
6. `BUILD_WINDOWS.cmd`: 성공 메시지의 오래된 `v1.23` 문구를 `v1.26.1`로 정정.

## 자체 검사 (오프라인)
- Java 구문 파싱 + 중복 선언 검사: 83개 파일 통과.
- 독립 v1.26 수련/사제/영약 규칙: 2,803회 통과.
- `qa_v121.py`부터 `qa_v126.py`까지 정적/데이터 검사 통과.
- 보존 대상 레거시 PNG/NBT 1,647개 SHA-256 일치.

## 중요한 제한
이 환경은 `services.gradle.org`의 DNS 해석이 불가능하여 NeoForge 실제 Java 타입 컴파일 및 게임 런타임 검증을 완료하지 못했습니다.
이번 패치는 업로드된 로그에 나타난 컴파일 문제를 고치도록 작성되었으나, 다른 컴파일 오류가 추가로 드러날 수 있습니다.
`ClientEvents.java`의 EventBusSubscriber `bus()` / `Bus`는 경고 2건으로 현재 빌드 실패의 직접 원인은 아닙니다. 기능 보존을 위해 별도 변경하지 않았습니다.

## 사용자 실행 순서
1. 이 ZIP을 **새 폴더**에 압축 해제. 과거 `build` 폴더와 섞지 말 것.
2. Java 21 사용.
3. `BUILD_WINDOWS.cmd` 실행 (`gradlew.bat --no-daemon clean build --stacktrace`).
4. `BUILD SUCCESSFUL`이면 `build/libs/`에서 JAR을 확인. Minecraft 실행은 별도 검증.
5. 실패하면 새로 생성된 **`build_log_unified.txt`**를 ChatGPT에 첨부.
