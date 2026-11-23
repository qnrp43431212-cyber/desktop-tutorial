# MURIM LIFE v1.22.1 정밀 검사 결과

검사 대상: 사용자가 제공한 `MurimLife_v1_22_All_In_One_EpicFight_Dialogue_Weapon_AI_MDK_Source_1.21.1.zip`의 압축 해제본.

## 결과

- **치명적 Java 중복 선언 6개 검출 → 전부 제거** (`JavaDuplicateCheck.java` 검사 기준: TALK 필드, TalkRequest 중첩 타입, WEAPON_STYLE 필드, 무기 스타일 getter/setter, 경기 협상 종료 메서드).
- **그 외 중복 실행 코드 제거:** TALK 입력/키 등록, TalkRequest 서버 핸들러, 대화 서버 틱/피해 이벤트, 대화 명령, 무기 NBT 중복 처리, 대련 대화 검사.
- **대화 정책/안전:** 플레이어 자신의 피해를 무조건 차단하지 않고 두 당사자 사이의 상호 공격만 중단; 서버의 6블록·생존·관전자 검사; 클라이언트 조준 광선·지형 충돌; 만료 기록 정리.
- **NPC 성능:** 반복 로딩 인원 스캔을 한 번으로 축소하고 후보 리스트를 사이트별로 정리.
- **기존 구버전 QA 실패 2종 해결:** 직업 수 불일치(83→87), 구조물 수 불일치(10→291, 총 NBT 635).

## 수행한 명령 및 관찰

| 검사 | 관찰 |
|---|---|
| `JavaParseOnly` | Java 47개 파일 구문 PASS(타입 컴파일 아님) |
| `JavaDuplicateCheck` | Java 47개 파일 중복 선언 0건 PASS |
| `MurimV121PureTests` | 18/18 PASS |
| `MurimV122PureTests` | 38/38 PASS |
| `scripts/test_rules_v16.sh` | 규칙 엔진 자체 테스트 PASS |
| `validate_assets.py` | 의상/아이템/방어구 레이어 데이터 PASS |
| `verify_v16_assets.py` | 무공 261, 별호 275, 무기 112, 구스킨 96, NBT 검사 PASS |
| `npc_static_qa.py` | 직업 87, 프로필 96, 배치 후보 864 PASS |
| `npc_v120_qa.py`, `qa_v121.py` | 기존 정적 데이터 검사 PASS(출력 버전은 역사적 스냅샷) |
| `qa_v122.py` | 원본 리소스 SHA-256 1,647개, NBT 635, 스킨 442, 대사 324, 이벤트 중복 검증 PASS |
| `./gradlew --offline --no-daemon build` | **불가**: wrapper가 `gradle-9.2.1-bin.zip` 다운로드 시 `java.net.UnknownHostException: services.gradle.org` |
| 실제 Minecraft 1.21.1 실행 | 미실행 |

**판정:** 소스 중복 선언 오류와 여러 로직 결함은 수정했고 정적/독립 테스트는 통과했지만, **완성된 실행 가능 JAR로 검증된 것은 아님**. 빌드/게임 실행 검증 후 추가 API 문제 발견 시 빌드 로그 기반 후속 수정 필요.

추가 CI 예방 수정: `verify_v16_assets.py`에서 Pillow 의존성을 제거해 GitHub Actions 기본 Python 환경에서도 PNG IHDR 크기를 표준 라이브러리로 확인할 수 있도록 변경함.
