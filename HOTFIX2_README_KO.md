# MURIM LIFE v0.1.9 – Build Fix 2 (ModItems 초기화 오류 해결)

이 패치는 기존 Build Fix 1을 기반으로 하며, 추가로 `ModItems.java`의
`WEAPON_FAMILIES`, `WEAPON_GRADES`, `GRADE_DURABILITY` 배열을 아이템 등록용
static 블록보다 먼저 초기화하도록 수정했습니다.

## 원인
`ModItems.<clinit>`에서 `gradedWeapons()`가 먼저 호출되어
아직 `null`인 `WEAPON_GRADES`의 길이를 읽으면서 게임이 종료되었습니다.
이는 사용자가 전송한 2026-11-23 00:58:11 NeoForge 크래시 보고서와 일치합니다.

## 빌드
1. 이 ZIP을 압축 해제합니다.
2. `MurimLife_v1_9_MDK_Integrated` 폴더에서 `BUILD_WINDOWS.cmd`를 실행합니다.
3. `build/libs`의 새 JAR로 이전 버전을 **교체**합니다(두 JAR을 동시에 두지 않습니다).

## 빠른 테스트
- 기존 JAR 대신 `murimlife-0.1.9-mdk-dev-hotfix1.jar`를 `mods`에 넣어 실행합니다.
- 실제 게임 실행과 전체 기능 검증은 아직 미확인입니다.
- 다른 오류가 있으면 `latest.log` 또는 새 `crash-...txt`를 보내주세요.

새 빌드 소스는 ModItems 정적 초기화 순서를 수정한 것 외에
Build Fix 1의 기존 기능·설정을 유지합니다.
