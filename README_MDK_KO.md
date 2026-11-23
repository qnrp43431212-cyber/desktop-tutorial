# MURIM LIFE 1.21.1 — 전달받은 MDK와 기존 소스 통합본

이 폴더는 사용자가 보내준 **MDK-1.21.1-ModDevGradle-main.zip**을 기준으로, 앞서 작성한 MURIM LIFE v1.9 소스와 v1.8 건축물 템플릿을 합친 **빌드용 개발 프로젝트**입니다. **빌드/게임 실행이 검증된 완제품이 아닙니다.**

## 변경 사항
- 공식 MDK의 `gradlew`, `gradlew.bat`, Gradle Wrapper 9.2.1 및 `net.neoforged.moddev` 2.0.148 적용
- `src/main/templates/META-INF/neoforge.mods.toml`로 모드 메타데이터 위치 수정: MDK 빌드 시 값 치환
- 기존 Java + 독립 규칙 엔진 `rules_engine/src` 컴파일 경로 지정
- 기존 무공·스탯·별호·문파 규칙 자료, 무기·복장 텍스처, UI 코드 보존
- **v1.8 수정 구조물 10종**을 모드 안의 `data/murimlife/structure`에 반영
- GitHub Actions 빌드 및 빌드 로그 업로드 워크플로 추가
- 기본 NeoForge 버전 `21.1.235`, Java 21, Minecraft 1.21.1

## Windows에서 빌드하는 법
1. [Eclipse Temurin JDK 21](https://adoptium.net/temurin/releases/?version=21) 설치하고 `java -version`으로 21인지 확인.
2. ZIP을 압축 해제. 폴더 이름에 한글/공백이 없는 경로 추천 (예: `C:\MurimLife`).
3. 인터넷을 연결한 채 **BUILD_WINDOWS.cmd** 더블클릭 (또는 CMD에서 `gradlew.bat --no-daemon build`). 처음에는 Gradle·NeoForge·Minecraft 개발 라이브러리 다운로드가 발생합니다.
4. **성공 시** `build/libs/`에 `.jar` 생성. 새 시험 월드에서만 먼저 설치.
5. **실패 시** 생긴 **build_log.txt 전체**를 이 대화에 첨부. 이 로그를 토대로 Java API 오류를 수정합니다.

## GitHub 자동 빌드 방법
1. 프로젝트 파일을 GitHub 저장소 루트에 올리기(`gradlew`, `gradle/`, `build.gradle`이 루트에 있어야 함).
2. GitHub의 **Actions → Build MURIM LIFE NeoForge 1.21.1 → Run workflow** 선택.
3. 성공이면 실행 결과의 **Artifacts**에서 `murimlife-1.21.1-neoforge-dev` 다운로드.
4. 실패면 `murimlife-build-log`를 받아 대화에 첨부하거나 실패한 단계의 로그를 복사해 전송.

## 현재 검증 범위
- Java 21 독립 규칙 엔진과 일부 파일 검증: 통과
- 공식 MDK Wrapper 파일 및 ModDevGradle 설정: 포함
- Gradle 의존성 다운로드 및 Minecraft 실제 컴파일/실행: **이 환경에서는 검증되지 않음**
- 설계된 모든 무공의 독립 모션, 고급 NPC AI, 자동 도시 생성, 문파/기연/내면세계 등: **여전히 구현 필요**

## 구조물 수동 배치 테스트
모드 JAR을 빌드·실행한 뒤 치트가 가능한 새로운 월드에서:
`/place template murimlife:test_marker ~ ~1 ~`
`/place template murimlife:village_inn ~ ~1 ~`
`/place template murimlife:city_inn ~ ~1 ~`
`/place template murimlife:murim_alliance_headquarters ~ ~1 ~`

이들은 **자연 생성 구조물이 아니라 수동 배치 템플릿**입니다. 도시 건축물은 대형이므로 넓은 평지에서 시험하세요.
