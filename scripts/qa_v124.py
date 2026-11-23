#!/usr/bin/env python3
"""Static integration checks: do not represent an actual NeoForge run."""
from pathlib import Path
import json,collections,sys,re
ROOT=Path(__file__).resolve().parents[1]
CAT=ROOT/'src/main/resources/data/murimlife/quests/catalog.json'
quests=json.loads(CAT.read_text(encoding='utf-8'))
counts=collections.Counter((q['faction'],q['grade']) for q in quests)
assert all(counts['alliance',grade]==30 and counts['sado',grade]==27 for grade in range(5))
assert sum(counts['civilian',grade] for grade in range(5))==19
assert len(quests)==304 and len(set(q['id'] for q in quests))==304
assert set(q['objective'] for q in quests)=={'TRAIN','WIN','DELIVER','GUARD','EXPLORE'}
for q in quests:
 assert 0<=q['grade']<=4 and q['count']>0 and q['rewardMun']>0 and q['reputation']>0
 assert q['title'] and len(q['title'])>=5
 assert q['objective']!='DELIVER' or q['item'].startswith('minecraft:')
assert len({q['title'] for q in quests})==len(quests)
base=ROOT/'src/main/java/kr/murimlife'
checks={'MurimProgressData.java':['WageClaims','Reputation','Loyalty','SectAlignment'],
 'economy/SectPayroll.java':['checkOverdue','withdrawUpTo','NpcSectRoster.get'] ,
 'economy/NpcSectRoster.java':['recruit(ServerPlayer','payday(MinecraftServer','checkOverdue','reimburse','desert'],
 'economy/SectDiscipline.java':['CORRECTION_DAYS','guardianKill','wageClaims'],
 'quest/QuestService.java':['registerCommands','boardCycle','PERIOD_TICKS','OpportunitySites'],
 'quest/ReputationService.java':['disguised','getEntitiesOfClass','recognize'],
 'npc/dialogue/MurimDialogueService.java':['무림 임무 문의','문파 NPC 영입 제안','문파 NPC 승급','integer(1,21)'],
 'npc/dialogue/CivicDialogueService.java':['시민 임무 문의','integer(1,10)'],
 'opportunity/OpportunitySites.java':['double edge=4.65','if(radial>edge)continue']}
for file,parts in checks.items():
 s=(base/file).read_text(encoding='utf-8')
 for part in parts:assert part in s,(file,part)
print(f'PASS: {len(quests)} unique quests, per-grade 30 Alliance / 27 Sado, 19 civilian')
print('PASS: profile NBT persistence and service registrations/interaction routes (static source inspection)')
print('NOTE: this validation does not prove Minecraft/NeoForge compatibility')
