# MURIM LIFE v1.9 - 1차 컴파일 오류 수정

## 근거: 사용자가 보내준 build_log.txt

- Gradle과 Minecraft/NeoForge 개발 아티팩트 내려받기 및 준비는 성공함.
- `:compileJava`에서 오류 5개, 경고 2개 발생함.
- `CoreEvents.java` : `Player` 타입에서 존재하지 않는 `server` 필드 3회 및 `serverLevel()` 메서드 1회 사용을 수정함.
  - 서버에 실제로 존재하는 `ServerPlayer`만 처리하도록 변경.
  - MinecraftServer는 `target.getServer()`로 가져오고 null 확인.
  - `serverLevel()`은 `ServerPlayer`로 좁힌 타입에서 호출.
- `MurimCombat.java` : 1.21.1 API의 효과 필드 이름 `MobEffects.DAMAGE_RESISTANCE`로 교체.
- `ClientEvents.java` 경고 2개는 제거 예정인 API 사용에 대한 경고로, 현재 실패 원인이 아님. 이번 패치에서 건드리지 않음.

## Windows에서 재빌드

1. 기존 프로젝트를 백업한다.
2. 이 ZIP 전체를 압축 해제한다. 또는 'Only_2_Java_Files.zip'의 두 소스만 기존 폴더의 동일 상대경로에 덮어쓴다.
3. 프로젝트 최상위에서 `BUILD_WINDOWS.cmd` 실행. (또는 `gradlew.bat --no-daemon clean build`)
4. 성공하면 `build/libs`의 결과물을 **시험용으로만** 확인하고, 실제 1.21.1 NeoForge 게임 실행을 추가 검증한다.
5. 여전히 실패하면 새 `build_log.txt`를 보낸다. 이전 로그가 아니라 새 파일이어야 한다.

**검증 범위**: 사용자 로그에 명시된 컴파일 오류 5개를 코드에서 수정하고 자바 문법·기본 구조와 ZIP 무결성을 정적으로 검사. 이 작업 환경에는 전체 NeoForge 컴파일 의존성이 없어서 재컴파일/게임 실행 성공을 주장하지 않음. 추가 컴파일 오류가 다음 시도에서 나타날 수 있음.
