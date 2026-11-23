#!/usr/bin/env python3
"""Self-contained v1.22 audit: verifies resources and wiring, not a Minecraft runtime."""
from pathlib import Path
import collections
import hashlib
import json
import re
import struct

root=Path(__file__).resolve().parent.parent
java=root/'src/main/java'
res=root/'src/main/resources'
ref=json.loads((root/'MANIFEST_SHA256_v121_historical.json').read_text(encoding='utf-8'))
legacy={entry['path']:entry['sha256'] for entry in ref['files']}
# Every old structure NBT & skin PNG must remain byte-identical, not merely present.
preserved=[]
for path,digest in legacy.items():
    if not path.startswith('src/main/resources/'):
        continue
    if not (path.endswith('.nbt') or path.endswith('.png')):
        continue
    data=(root/path).read_bytes()
    assert hashlib.sha256(data).hexdigest()==digest, ('Historical resource modified',path)
    preserved.append(path)
nbt=sorted(res.rglob('*.nbt'))
assert len(nbt)==635,len(nbt)
skins=sorted((res/'assets/murimlife/textures/entity/wuxia').glob('skin_*.png'))
legacy=[res/'assets/murimlife/textures/entity/wuxia'/f'skin_{i:03d}.png' for i in range(442)]
assert len(skins)>=442 and all(p.exists() for p in legacy)
assert len({hashlib.sha256(x.read_bytes()).digest() for x in legacy})==442
for file in skins:
    data=file.read_bytes()
    assert data[:8]==b'\x89PNG\r\n\x1a\n' and struct.unpack('>II',data[16:24])==(64,64),file
keys={'draw','delay','provoke','mercy','info','end'}
all_lines=[]
for kind,count in [('daily',24),('combat',30)]:
    data=json.loads((res/f'data/murimlife/dialogue/{kind}_lines.json').read_text(encoding='utf-8'))
    assert set(data)==keys
    for key,entries in data.items():
        assert len(entries)==count and all(isinstance(x,str) and x.strip() for x in entries),(kind,key)
        all_lines.extend(entries)
assert len(all_lines)==324 and len(set(all_lines))==324

def source(name): return (java/name).read_text(encoding='utf-8')
client=source('kr/murimlife/ClientEvents.java')
network=source('kr/murimlife/MurimNetwork.java')
main=source('kr/murimlife/MurimLife.java')
admin=source('kr/murimlife/npc/NpcAdminCommands.java')
entity=source('kr/murimlife/npc/MurimHumanoid.java')
arena=source('kr/murimlife/npc/NpcArenaService.java')
dialogue=source('kr/murimlife/npc/dialogue/MurimDialogueService.java')
assert client.count('event.register(TALK)')==1
assert client.count('while (TALK.consumeClick())')==1
assert client.count('private static int aimedNpcId(')==1
assert network.count('record TalkRequest(')==1
assert network.count('registrar.playToServer(TalkRequest.TYPE')==1
assert main.count('NeoForge.EVENT_BUS.addListener(MurimDialogueService::onTick)')==1
assert main.count('NeoForge.EVENT_BUS.addListener(MurimDialogueService::onDamage)')==1
assert admin.count('MurimDialogueService.registerCommands(dispatcher)')==1
assert entity.count('defineId(MurimHumanoid.class,EntityDataSerializers.INT)')==2 # skin+weapon
assert entity.count('builder.define(WEAPON_STYLE,')==1
assert entity.count('public void setWeaponStyle(')==1
assert entity.count('public kr.murimlife.npc.combat.WeaponTactics.Style getWeaponStyle(')==1
assert entity.count('tag.putInt("MurimWeaponStyle"')==1
assert arena.count('public static void finishByNegotiation(')==1
assert 'MurimCommands.register(event)' in source('kr/murimlife/MurimProgressEvents.java')
assert 'DialogueRules.pairedDamage(' in dialogue
assert '@Mod(MurimLife.MOD_ID)' in main
assert len(re.findall(r'@Mod\s*\(', '\n'.join(x.read_text(encoding='utf8') for x in java.rglob('*.java'))))==1
report={'version':'v1.22.1-audit-fix','checks':'PASS','java_sources':len(list(java.rglob('*.java'))),
        'preserved_historical_resources_sha256':len(preserved),'structure_nbt':len(nbt),
        'unique_wuxia_skins':len(skins),'unique_dialogue_lines':len(all_lines),
        'duplicate_wiring_checks':'PASS','full_neoforge_build':'NOT_RUN','minecraft_runtime':'NOT_RUN'}
(root/'qa/v122_integrity_report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps(report,ensure_ascii=False,indent=2))
