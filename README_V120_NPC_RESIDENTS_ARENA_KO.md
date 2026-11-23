# MURIM LIFE v1.20 — 영구 NPC 주민 / 한국·중국식 고유 인명 / 비무대회 관중 시스템 (Alpha)

## 통합 모드 유지

- Minecraft **1.21.1**, NeoForge **21.1.235 기준**, Java **21** 개발 소스.
- 모드 ID는 `murimlife` 단 하나. 기존 MURIM LIFE 본체(전투/장비/UI)+v1.16 건축물+v1.17 월드 생성 코드+v1.19 NPC 준비 자료를 그대로 포함한다.
- Architecture나 Worldgen, NPC를 별도의 MURIM LIFE 모드 JAR로 설치하는 방식이 아니다. `murimlife_arch:`는 구조물 **데이터 네임스페이스**이지 별도 설치 모드가 아니다.

## 요구사항 반영

1. **주민 영속성**: 월드에 살아 있는 주민은 시험 구현에서 **바닐라 주민(Villager)** 엔티티를 사용하고, `setPersistenceRequired`, 엔티티 NBT, 월드 공용 `SavedData`에 고유 UUID·소속·이름·역할을 저장한다. 주민이 청크 밖으로 나가도 삭제하지 않는다. 청크 언로드 시 저장되고 재로딩 시 동일 주민으로 돌아오며, 언로드 중에는 마인크래프트 기본 규칙대로 틱/AI가 돌지 않는다. 플레이어가 떠난다고 `discard`하거나 신규 엔티티로 갈아 끼우지 않는다. **사망은 별도 상태이며 죽은 주민은 자동 부활하지 않는다.**
2. **30개 민간 직업**: 기존 26종 + 재봉사/제지공/필사공/뱃사공 4종. 다른 무림 역할을 포함하면 총 87개 역할 분류. 직업 30개가 서로 다른 전용 AI 및 텍스처로 구현된 것은 **아님**.
3. **구파일방 10 / 오대세가 5**: 창작 가주·장문인·방주 등 15명. 15곳 각각 22명 = 추가 330명 고유 인명. 기존 96명과 이름 중복 없음. `한국 이름`과 `중국식 이름`을 함께 제공. 중국식 이름에는 한국 한자음과 한자·중국어 병음이 함께 매핑된다. 같은 인물의 표기이지 인물 복제가 아님.
4. **실제 대회장 기반 좌석**: 기존 `murimlife:martial_tournament_arena` NBT(55×55×17)의 계단형 관중석을 읽어 444개 후보 검사(4방위 111개씩). 기본 최대 48명, 설정 최대 80명. 강제 청크 로딩 없이 이미 로딩된 좌석만 사용한다.
5. **시험용 대련**: 경기 시작 명령으로 NPC 대 NPC 또는 플레이어 대 NPC와 관중을 볼 수 있는 임시 시험 시스템. 충돌 시 파티클·피해를 발생시키지만 아직 무공/애니메이션/경지에 따른 진짜 전투 AI는 **미구현**. 기존 정기대회 `/murim tournament enter`는 아직 문자 시뮬레이션이며 새 시험 경기가 공식 대회와 자동 연결된 것은 아니다.
6. **관중 정리**: 관중은 상주 주민과 다른 *임시 엔티티*. 경기 종료·호스트 퇴장·시간 만료 시 제거, 게임 종료 후 로딩된 이전 임시 엔티티도 세션 ID를 확인하여 제거. 상주 주민에게는 이 정리가 적용되지 않는다.

## 빌드 및 설치

1. 압축 해제 후 `BUILD_WINDOWS.cmd` 실행 (`Java 21`, 인터넷 연결, Gradle 래퍼 필요).
2. 빌드 성공 시 `build/libs/murimlife-0.1.20-residents-arena-alpha.jar`가 만들어져야 한다.
3. **기존 MURIM LIFE 본체/구조물/월드생성 JAR은 모두 제거하고 새 JAR 하나만** `mods/`에 설치. Epic Fight 등 외부 라이브러리는 사용하는 기능에 따라 별도로 필요할 수 있다.
4. 모든 시험은 백업된 새 월드에서 진행. 전체 NeoForge 컴파일 및 실제 게임 동작은 아직 검증되지 않았다.

## NPC 시험 설정

첫 서버 기동 후 만들어지는 `config/murimlife-npc.json`을 다음처럼 변경하고 재시작:

```json
{
  "enablePersistentResidents": true,
  "enableTournamentDemo": true,
  "displayNameStyle": "MIXED",
  "residentGlobalCap": 160,
  "residentSiteCap": 24,
  "residentSpawnIntervalTicks": 100,
  "tournamentSpectatorCap": 48,
  "tournamentMaxDurationTicks": 2400
}
```

`enablePersistentResidents`와 `enableTournamentDemo`는 **기본 false**. 실제 빌드·게임 검증 전에는 안전을 위해 false로 유지하는 것이 권장된다. `residentGlobalCap`은 **현재 로딩된 개체 수 제한**이며 월드에 저장 가능한 총 주민 수 제한이 아니다. `displayNameStyle`은 `KOREAN`, `CHINESE`, `BOTH`, `MIXED`(기본). 모든 모드 구성은 하나의 JAR이다.

## 명령어 (시험용)

- `/murim npc status`: 월드에 저장된 영구 주민 수, 로딩된 영구 주민 수.
- `/murim npc leader namgung`: 남궁세가 가주 이름을 한국어·한자·중국어 병음으로 조회.
- `/murim npc leader shaolin`: 소림사 방장의 이중 표기 조회.
- `/murim npc spawn namgung`: 관리자(권한 레벨 2) 전용 시험 주민 한 명 생성. 적절한 평지·머리 공간 필요.
- `/murim arena npcvnpc`: **55×55 무림대회장 중앙 광택 섬록암** 위에 서서 실행하면 NPC끼리 시험 대련하며 관객을 생성.
- `/murim arena playernpc`: 같은 장소에서 플레이어 대 NPC 시험 대련, 관중 생성. 테스트용이며 플레이어 체력에 영향을 줄 수 있다.
- `/murim arena stop`: 경기·임시 관중 정리.

기존 대회장 수동 배치 예: `/place template murimlife:martial_tournament_arena ~ ~ ~` (명령이 놓는 곳의 y에 따라 바닥 높이 변동). 대회장 중앙으로 이동해서 `polished_diorite` 바닥 위에 선다.

## 중요한 기술적 제약

- **현재 마인크래프트 월드 테스트/NeoForge Gradle 전체 컴파일을 하지 못했다.** API 타입·실제 등록·서버 성능·클라이언트 렌더링 확인 필요.
- 96개 준비된 커스텀 NPC 스킨을 사용하는 렌더러는 아직 미구현. 실제 주민은 **바닐라 주민 외형을 쓰는 영속성 시험 구현**이며 30개 직업마다 옷/상호작용이 실제로 달라지지 않는다.
- 명명 데이터는 NPC에게 배정되어 월드에 저장되므로 게임 재시작 후 이름이 변경되지 않는다. 데이터 기반 이름과 직업은 플레이어·설치 모드팩의 번역 기능과는 무관하다.
- 이름이 있는 NPC의 죽음·타 모드 변신은 현재 자동 복구하지 않는다. '청크가 멀어졌을 때 보존'과 '전투 사망에도 영원히 생존'을 구분한다.
- 55×55 경기장 NPC 대련은 연출용 알파. 정규 무림대회 참가 등록/대진표/승부 결과/상금/합·패링 등의 전투 시스템과 별도로 아직 테스트용 명령을 사용한다.
- 거대 문파 자연 생성 v1.17은 기본 비활성 상태. 완공된 SiteLedger 위치에만 주민을 자동 생성하며, 수동 `/murim npc spawn` 명령은 별도 시험 경로이다.

## 재검사

- `python scripts/npc_v120_qa.py` : 이름·직업·좌석·데이터/참조 검증.
- `QA_NPC_V120_WINDOWS.cmd` : Java 문법 파싱 + Java 21 독립 규칙 테스트 + Python 데이터 검증.
- `npc_qa_v120/` : 한국어/중국어 이름 및 30직업의 CSV, 검증 결과 JSON.
