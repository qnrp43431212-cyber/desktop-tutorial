# MURIM LIFE v1.26.3 — GitHub Actions 오류 수정 통합본

## 수정 내용
- `DisputeService.java`의 `for` 반복문에서 변화하는 지역변수 `i`를 람다 안에서 참조하던 컴파일 오류 수정.
- 반복마다 `final int choice = i;`로 값을 복사하고 클릭 가능한 명령은 `+choice`를 사용하도록 수정.
- 빌드 버전을 `0.1.26.3-github-actions-hotfix-alpha`로 변경.
- `.github/workflows/build.yml`에 **수정된 소스가 GitHub에 실제로 업로드됐는지** 확인하는 사전 검사 추가.
- 기존 NPC, 문파, 기연, 무공, 전투, 경제, 대마두, 영약, 임무, 건축물 자료 유지.

## 기존 GitHub Desktop 프로젝트 업데이트하는 방법
1. 이 ZIP을 새 폴더에 압축 해제한다.
2. GitHub Desktop에서 `Repository → Show in Explorer`를 눌러 **기존 Git 저장소 폴더**를 연다.
3. 이 ZIP의 최상위 내용(`src`, `.github`, `gradle`, `build.gradle`, `gradlew`, 기타 파일)을 **모두** 선택해 복사한다.
4. 기존 Git 저장소의 **최상위 위치**에 붙여넣고 같은 이름의 파일은 **덮어쓴다**. 새 하위 폴더 하나에 통째로 넣으면 안 된다.
5. 기존 저장소의 `.git` 폴더는 삭제하거나 덮어쓰지 않는다. ZIP 자체를 한 파일로 저장소에 추가하지 않는다.
6. GitHub Desktop에서 변경 목록에 `DisputeService.java`, `gradle.properties`, `.github/workflows/build.yml`이 나타나는지 확인한다.
7. `Commit to main` → `Push origin`을 차례로 누른다.
8. GitHub 저장소 → `Actions`에서 새 빌드 실행 상태를 확인한다.

## GitHub Actions에서 오류 발생 시
- `Verify checkout is v1.26.3 with Java hotfix` 단계에서 실패하면 수정본을 저장소 최상위에 덮어쓰지 않은 것이다.
- `Compile and build NeoForge mod` 단계에서 실패하면 새 컴파일 오류 로그를 다운로드하여 제공한다.
- 성공하면 실행 상세 화면의 `Artifacts`에서 JAR이 포함된 파일을 다운로드한다.

## 자체 검사 및 제한
- 독립 규칙 및 데이터 검사에 통과할 수 있도록 기존 검사 스크립트를 보존했다.
- 전체 NeoForge Gradle 컴파일과 마인크래프트 실제 실행은 이 환경에서 확인하지 못했으므로 GitHub Actions 결과로 검증이 필요하다.
