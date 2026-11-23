# MURIM LIFE v1.21 — Epic Fight 통합 전투 개발본

**단일 모드 ID `murimlife`, 단일 JAR 유지.** Epic Fight 21.17.3.1은 외부 필수 모드이며 MURIM LIFE JAR에 번들하지 않습니다.

## 이 개발본에 실제로 추가된 부분
- 신규 `murimlife:human` PathfinderMob 엔티티 (UUID/스킨ID/NBT/디스폰 방지)
- Epic Fight 21.17.3.1 API 직접 연결: entity patch, BIPED armature, patched renderer, 기본 보행/달리기/점프 및 검격 애니메이션, AnimatedAttackGoal
- 실전 공격을 시도하는 NPC vs NPC 및 플레이어 vs NPC 시험 대련; 기존 가상 35틱 피해량 연출 제거
- 무기 충돌 **합(合)**: 양쪽 공격 시점 5틱, 방향, 거리 조건을 서버에서 검사하여 피해 취소 + 불꽃 파티클. 데모 경기 중 낮은 체력 안전 종료
- 회전각 보간, 최대 각속도 제한, 전환 속도 계산을 하는 NPC 전용 MotionBlend. **Epic Fight 본체의 모든 플레이어 애니메이션을 강제로 수정하는 패치는 아님.**
- 고유 무협풍 인간형 64x64 스킨 442개: 구파일방·오대세가의 지도자/내부 인물 345명+사도련주 1명은 고유 인덱스 0~345, 일반 주민 96종 346~441.
- 사도련주 창작 인명: 독고천악 (獨孤天岳), 고유 ID `sado:leader`, 일반 NPC보다 높은 HP/방어력/공격력.
- 기존 v1.20 NPC 월드 저장 방식 유지. 구버전 주민(Villager) 엔티티는 기존 모습으로 남고 새로 생성한 NPC부터 신규 인간형을 사용합니다.

## 테스트 방법
1. 백업한 별도 세계에서 기존 MURIM LIFE JAR 하나만 제거하고 빌드한 v1.21 단일 JAR을 넣습니다. Epic Fight 21.17.3.1 별도 설치 필수.
2. `BUILD_WINDOWS.cmd`로 빌드. 외부 Gradle/NeoForge/Epic Fight 라이브러리가 다운로드되어야 합니다.
3. `config/murimlife-npc.json`의 `enablePersistentResidents`, `enableTournamentDemo`를 원하는 시험 기능에 맞춰 `true`로 설정 후 재시작.
4. `/murim npc spawn shaolin`, `/murim npc spawn sado`로 평평한 곳에 NPC 시험 생성; `/murim npc leader sado`로 네임드 확인.
5. 기존 비무대회장 중앙에 서서 `/murim arena npcvnpc`, `/murim arena playernpc`, `/murim arena stop` 명령으로 시험.

## 잔여 위험 및 미완료
- **이 환경에서는 NeoForge 전체 Gradle 컴파일과 Minecraft 실제 게임 실행을 하지 못했음**. 빌드 성공 및 Epic Fight 패치 작동 여부 확인 전까지 실제 생존 월드에 사용하지 마세요.
- NPC가 Epic Fight와 별도 등록된 스킨 렌더러에서 동시에 정상 렌더링되는지 실제 런타임 검증이 필요함.
- **합**은 피해 이벤트 기반 초기 시스템으로, Epic Fight의 모든 타격 판정·스턴·콤보와 통합된 완성 합 시스템은 아님. 향후 기본 공격기 애니메이션, 12슬롯 무공과 프레임 타이밍을 연동해야 함.
- 442개 PNG는 개별 64×64 스킨이지만 기본 UV 인간형 모델 기반 초기 수작업풍 픽셀아트. 도복/머리 장식/가문 문양의 세밀한 모델은 아직 없음.
- 주민 직업 30종 개별 생활 AI, 출혈·기력·합 파열 연출, 특수 보스 AI, NPC 사전 생성 월드젠, 서버 재기동 시 진행 중인 대회 복구는 추가 개발 항목.
- 대형 구조물 자동 생성은 기존대로 안전을 위해 기본 OFF.
