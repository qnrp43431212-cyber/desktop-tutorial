# MURIM LIFE v1.26.5 — Minecraft 무협 건축 참고 / 비차단 독립 HUD

## 이전 사용자 피드백
- 현실 건축 사진이 아닌 *Minecraft 안에서 실제로 구현된* 중국풍/무협풍 건축물, 튜토리얼, 월드 설계를 기준으로 사용할 것.
- 구조물의 어색한 반복 벽무늬·끊어지는 벽체·침대만 놓인 침실·서로 뜨는 처마/울타리를 개선할 것.
- 기본 마인크래프트 `Button`을 사용해 상태창 텍스트가 흐려 보이거나 모자이크처럼 보이지 않도록 바꿀 것.
- `~` NPC 대화는 시야 중앙을 가리지 않는 측면 HUD에 놓고, 배경 흐림 없이 표시할 것.
- **Alt를 누르는 동안 마우스 커서가 나타나고 시점 회전을 멈춘 상태로 메뉴 선택**할 것.

## 이 버전에서 실제 변경한 것
1. `MurimHudOverlay.java` 신규 작성: 기본 `Screen`과 `Button`을 쓰지 않는 HUD 직접 그리기(불투명 패널+선명한 글자), M 강호록 5탭·무공 배치·닫기 버튼.
2. `ClientEvents.java`에서 M 키를 새 HUD로 연결. `RenderGuiEvent.Post`로 렌더링하고 Left/Right Alt 커서 해제·재포획, 마우스 클릭/휠 이벤트 경로 추가.
3. `MurimNetwork`에 서버→클라이언트 대화창/알림/닫기 패킷 추가. 이 패킷은 **표시 전용**이며, 선택 시 기존 `/murim ...` 명령을 호출하므로 서버의 세션·거리·금전·권한 검사를 유지한다.
4. NPC 무림인·주민/점소이 대화, 상점, 분쟁, 임무 선택지의 채팅 버튼 대신 측면 HUD 메뉴 표시. 일부 자세한 정보와 경제 결과 알림은 기존 서버 채팅 메시지가 남아 있을 수 있음(후속 통합 대상).
5. 단순히 현실 건축물을 묘사하는 것이 아니라 Minecraft 지붕 계단과 벽 프레임 표현을 분석해 Minecraft NBT 구조물에 적용: v1.26.4 대비 **237개 구조물 NBT**의 블록 상태가 추가로 변경됨. 건축물 635개 보존. 실내 침실 구획과 가구 배치는 v1.26.4에서 한 수정분을 유지.
6. 기존 v1.21 역사적 SHA-256 데이터와 v1.26.4 승인 변경 해시를 그대로 두고, 별도 `ARCHITECTURE_V1265_MANIFEST.json`에 v1.26.4→v1.26.5 변경을 기록. 자동검사는 변경 승인 체인만 허용.

## Minecraft에서 직접 참고한 프로젝트·강좌 (원본 구조물/스킨을 복제하지 않음)
- Minecraft 中国风 건축 시공 튜토리얼 — 골조/보/기와 계단 연구: https://www.bilibili.com/video/BV1HP4y1i7BC/
- Minecraft Java 1.21.1 중국풍 객잔 제작 — 객실·기둥 배치 연구: https://www.bilibili.com/video/BV1xyRKYzEJ7/
- Minecraft 중국식 사합원 대형 건축 강좌 — 규모·외벽/마당 균형 연구: https://www.bilibili.com/video/BV1qL4y187bt/
- Minecraft Courtyard House 튜토리얼 — 안마당과 출입 동선: https://www.youtube.com/watch?v=fpPqKS_crhQ

**기술·품질 한계:** NBT가 정상이라고 건물의 미관이 검증되는 것은 아닙니다. 현재 수정한 계단·창틀은 재질/방향 등 블록 상태를 검증했지만 마인크래프트에서 일일이 육안 점검하지는 못했습니다. 독립 UI의 Alt 커서와 입력 차단도 실제 NeoForge 클라이언트에서 확인이 필요합니다. 변경 소스는 CI 컴파일 전이며 정상 JAR이라고 확정할 수 없습니다.

## 설치/검증
1. 이 ZIP은 개발 소스입니다. **mods 폴더에 그대로 넣지 마세요.**
2. GitHub 저장소의 동일 프로젝트 최상위 폴더에 모든 파일을 교체하여 커밋/Push합니다. `build.gradle`, `gradlew`, `.github/workflows/build.yml`이 최상위에 있어야 합니다.
3. GitHub Actions가 자동 `./gradlew build` 실행. 성공하면 `murimlife-1.21.1-neoforge-dev` artifact의 `.jar`를 다운로드합니다.
4. 기존 MURIM LIFE JAR은 제거한 뒤 새 JAR 하나만 설치합니다. Minecraft 1.21.1 NeoForge + 의존성 Epic Fight를 사용합니다.
5. **새 월드에서** 건축물을 확인하세요. 기존에 이미 생성된 건물은 변경되지 않습니다.
6. M, ~, Alt를 다음 순서로 테스트: `M` → 강호록 뜸 → Alt를 누르고 버튼을 누르기 → Alt 해제 후 시점 조작 회복 → NPC를 조준해 `~` → 오른쪽 대화창 → Alt+클릭으로 선택 → 상점·임무 이동.
7. 오류 발생 시 `logs/latest.log`, `crash-reports`, GitHub Actions build log를 첨부해 주세요.

## 실행되는 검증
```bash
python3 scripts/check_hotfix_v1263.py
python3 scripts/verify_hud_ui_v1265.py
python3 scripts/asset_integrity_v1264.py
python3 scripts/qa_v1264_architecture.py
```
전체 Gradle 빌드와 인게임 검증은 위 정적 검사에 포함되지 않습니다.
