from pathlib import Path
import json,gzip
root=Path(__file__).resolve().parents[1]
asset=root/'src/main/resources/assets/murimlife'
rules=root/'design_data'
for f in root.rglob('*.json'):
    try:json.loads(f.read_text(encoding='utf8'))
    except Exception as e:raise RuntimeError(f'Invalid JSON {f}: {e}') from e
arts=json.loads((rules/'v16_martial_catalog_balancing.json').read_text())['martial_arts']
aliases=json.loads((rules/'v16_epithet_catalog.json').read_text())['catalog']
weapons=json.loads((rules/'v16_weapon_grade_registry.json').read_text())['weapons']
npcs=json.loads((rules/'v16_npc_templates.json').read_text())['profiles']
assert len({a['id'] for a in arts})==len(arts)
assert len({a['name'] for a in aliases})==len(aliases)
assert len(weapons)==112
assert len(npcs)==96
for w in weapons:
   iid=w['id'].replace('weapon_','')
   assert (asset/'models/item'/f'{w["id"]}.json').is_file()
   assert (asset/'textures/item'/f'{w["id"]}.png').is_file()
# PNG IHDR carries width/height in bytes 16..23. No third-party Pillow needed.
import struct
for n in npcs:
    path=asset/'textures/entity/npc'/n['skin'];assert path.is_file()
    data=path.read_bytes()
    assert data[:8]==b'\x89PNG\r\n\x1a\n' and struct.unpack('>II',data[16:24])==(64,64), path
assert len(set((asset/'textures/entity/npc'/n['skin']).read_bytes() for n in npcs))==96
bpath=root/'src/main/resources/data/murimlife/structure'
for f in bpath.glob('*.nbt'):
    raw=gzip.decompress(f.read_bytes());assert raw.startswith(b'\x0a\x00\x00')
assert len(list(bpath.glob('*.nbt')))>=10  # v1.22 also contains hundreds of later structures
print(f'PASS data: {len(arts)} research arts, {len(aliases)} unique aliases, {len(weapons)} weapons, {len(npcs)} unique skins, {len(list(bpath.glob("*.nbt")))} compressed NBT structures, JSON syntax')
