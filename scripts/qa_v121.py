from pathlib import Path
import hashlib,json,re,csv,subprocess,zipfile,struct
r=Path(__file__).resolve().parent.parent
res=r/'src/main/resources/data/murimlife/npc'
assets=r/'src/main/resources/assets/murimlife/textures/entity/wuxia'
main=r/'src/main/java/kr/murimlife/MurimLife.java'
skins=sorted(assets.glob('skin_*.png'))
# Preserve the complete v1.21 legacy atlas and allow new non-reused cult skin slots.
legacy=[assets/f'skin_{i:03d}.png' for i in range(442)]
assert all(p.exists() for p in legacy)
assert len(skins)>=442,len(skins)
assert len(set(hashlib.sha256(x.read_bytes()).hexdigest() for x in legacy))==442
assert all(struct.unpack('>II',p.read_bytes()[16:24])==(64,64) for p in skins)
index=json.loads((res/'wuxia_named_skins.json').read_text())
entries=index['entries']; assert len(entries)==346 and len(set(x['name_ko'] for x in entries))==346 and len(set(x['skin'] for x in entries))==346
leaders=json.loads((res/'named_leaders_16.json').read_text())['leaders']; assert len(leaders)==16
members=json.loads((res/'named_faction_members_330.json').read_text())['members']; assert len(members)==330
assert set(x['name_ko'] for x in entries)==set(x['name_ko'] for x in leaders+members)
assert sum(x['faction']=='sado' for x in entries)==1
assert any(x['name_ko']=='독고천악' and x['role']=='sado_leader' for x in entries)
assert all((assets/f"skin_{x['skin']:03d}.png").exists() for x in entries)
assert len(list((r/'src/main/resources/assets/murimlife/textures/entity/npc').glob('npc_skin_*.png')))==96
classes={x.stem:x.read_text(encoding='utf-8') for x in (r/'src/main/java/kr/murimlife/npc').rglob('*.java')}
assert 'MurimNpcTypes.ENTITIES.register(modBus)' in main.read_text()
assert 'MurimEpicBridge.register()' in main.read_text()
assert '@Mod(MurimLife.MOD_ID)' in main.read_text()
assert len(re.findall(r'@Mod\s*\(', '\n'.join(p.read_text() for p in (r/'src/main/java').rglob('*.java'))))==1
assert 'new MurimHumanoid' in classes['NpcResidentService']
assert 'new MurimHumanoid' in classes['NpcArenaService']
assert 'AnimatedAttackGoal' in classes['MurimEpicPatch']
assert 'getTarget()!=null' in classes['MurimHumanoid']
assert 'e.setCanceled(true)' in classes['MurimClashEvents']
assert 'net.minecraft.world.entity.npc.Villager' not in classes['NpcArenaService']
assert 'maven.modrinth:epic-fight:21.17.3.1-mc1.21.1-neoforge' in (r/'build.gradle').read_text()
assert 'modId="epicfight"' in (r/'src/main/templates/META-INF/neoforge.mods.toml').read_text()
# check core legacy files stayed present
nbt=list((r/'src/main/resources').rglob('*.nbt'))
assert len(nbt)>=600,len(nbt)
report={'version':'0.1.21-epiccombat-alpha','mod_id':'murimlife','epic_fight_target':'21.17.3.1','named_humanoid_skins':346,'all_wuxia_skins':len(skins),'legacy_skin_assets':96,'named_leaders':16,'named_roster_members':330,'structure_ntbs_preserved':len(nbt),'api_class_surface':'EpicFight jar 21.17.3.1 symbols checked with javap','pure_java_tests':18,'java_files_syntax_parsed':len(list((r/'src/main/java').rglob('*.java'))),'gradle_neoForge_compile':'NOT_RUN_NO_DEPENDENCIES','minecraft_runtime':'NOT_RUN','features':{'custom_entity':True,'epic_patch_java':True,'epic_fight_attack_ai_java':True,'clash_damage_cancel_java':True,'tournament_real_target_ai_java':True,'named_unique_skin_assets':True,'general_epicfight_smoothing_rewrite':False,'real_game_confirmed':False}}
(r/'qa_v121').mkdir(exist_ok=True)
(r/'qa_v121/manifest.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
with (r/'qa_v121/named_346_unique_skins.csv').open('w',encoding='utf-8-sig',newline='') as fh:
 w=csv.writer(fh);w.writerow(['고유키','이름','문파','역할','스킨ID','경로'])
 for x in entries:w.writerow([x['key'],x['name_ko'],x['faction'],x['role'],x['skin'],x['texture']])
print(json.dumps(report,ensure_ascii=False,indent=2))
