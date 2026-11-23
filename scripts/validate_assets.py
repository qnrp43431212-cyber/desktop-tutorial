from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]
p=root/'design_data/murimlife_v15_rules.json'
d=json.loads(p.read_text(encoding='utf-8'))
assert len(d['outfits'])==38
assert len({o['id'] for o in d['outfits']})==38
assert len([o for o in d['outfits'] if o['category']=='unorthodox'])==10
assert len([o for o in d['outfits'] if o['category']=='wanderer'])==8
assert len([o for o in d['outfits'] if o['category']=='player_created'])==3
assert len([o for o in d['outfits'] if o['category']=='special_cult'])==2
assert set(d['cult_world']['valid_initial_states'])=={'both_exist','only_blood_exists','only_demon_exists'}
assert not set(d['cult_world']['invalid_initial_states'])&set(d['cult_world']['valid_initial_states'])
assert len({m['id'] for m in d['cult_arts']})==len(d['cult_arts'])
for m in d['cult_arts']:
 if m['kind']=='passive': assert m['optimal_weapon'] is None
 if m['source_class']=='original_game_design': assert m['notes']
for o in d['outfits']:
 for k in ('chestplate','leggings','boots'):
  id=o['id']+'_'+k
  assert (root/f'src/main/resources/assets/murimlife/textures/item/{id}.png').exists(),id
  assert (root/f'src/main/resources/assets/murimlife/models/item/{id}.json').exists(),id
 for layer in (1,2):
  assert (root/f'src/main/resources/assets/murimlife/textures/models/armor/robe_{o["id"]}_layer_{layer}.png').exists()
print('Asset/data validation PASS: 38 outfits, 114 costume items, 76 armour layers, cult and skill registry')
