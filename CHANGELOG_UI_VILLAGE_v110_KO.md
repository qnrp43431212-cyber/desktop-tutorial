# MURIM LIFE 0.1.10 UI / 마을 객잔 수정

- UI: M 상태창의 renderBackground() 블러 호출 제거. 배경은 암전만 적용.
- UI: 불투명 본문 패널, 글자 그림자, 대비 높은 흰색/황금색 글자, 탭별 가독성 개선.
- 구조물: 마을 객잔(village_inn 및 jianghu_inn)의 계단·2층 착지공간·방문·벽·의자 방향·기둥 받침 재설계.
- 일반 객잔 외 다른 8개 구조물은 이번 수정 대상이 아니며 기존 데이터를 보존함.

빌드: BUILD_WINDOWS.cmd 실행 → build/libs 의 murimlife-0.1.10-mdk-dev.jar 사용.
기존 murimlife-0.1.9-mdk-dev*.jar 은 mods 폴더에서 제거해 중복 설치하지 마세요.

사용 시: /place template murimlife:village_inn ~ ~1 ~
건축물 전용 모드를 사용할 때는: /place template murimlife_arch:village_inn ~ ~1 ~

상태: 구조물 NBT 정적 검사 통과. NeoForge 전체 컴파일/게임 실행 미검증.
