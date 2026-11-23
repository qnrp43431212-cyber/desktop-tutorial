from pathlib import Path
from collections import Counter,defaultdict
import json,hashlib,sys,time
# Portable structure scanner, no external dependencies.
import nbt_codec as loader
# already imported nbt_codec
ROOT=loader.ROOT
seen={};result=[];fixes=[];fatal=[]
start=time.time()
for ix,p in enumerate(loader.PATHS):
 key=hashlib.sha256(p.read_bytes()).hexdigest()
 if key in seen:
  v=dict(seen[key]);v['path']=str(p.relative_to(ROOT));v['duplicateOf']=seen[key]['path'];result.append(v);continue
 try:
  d=loader.load(p);size=d.get('size',[]);palette=d.get('palette',[]);blocks=d.get('blocks',[])
  if len(size)!=3:raise ValueError('invalid size')
  names=[e.get('Name','') for e in palette]
  state={tuple(b['pos']):b['state'] for b in blocks}
  pos=[tuple(b['pos']) for b in blocks]
  outbounds=sum(1 for b in blocks if len(b['pos'])!=3 or any(x<0 or x>=size[i] for i,x in enumerate(b['pos'])) or b['state']<0 or b['state']>=len(palette))
  duplicates=len(pos)-len(state)
  bytype=Counter(names[i] for i in state.values())
  doors=0;brokenDoors=0;stairs=0;floaters=0;chair=0
  for (x,y,z),idx in state.items():
   n=names[idx]; props=palette[idx].get('Properties',{})
   if n.endswith('_door'):
    if props.get('half','lower')=='lower':
     doors+=1
     upper=state.get((x,y+1,z),-1)
     if upper<0 or names[upper]!=n or palette[upper].get('Properties',{}).get('half','upper')!='upper':brokenDoors+=1
    else:
     lower=state.get((x,y-1,z),-1)
     if lower<0 or names[lower]!=n or palette[lower].get('Properties',{}).get('half','lower')!='lower':brokenDoors+=1
   if n.endswith('_stairs'):
    stairs+=1
    below=state.get((x,y-1,z),-1)
    if y>0 and (below<0 or names[below] in ['minecraft:air','minecraft:cave_air','minecraft:water']):floaters+=1
   if any(w in n for w in ('_fence','_wall','_trapdoor')):chair+=1
  coverage=len(state)/(size[0]*size[1]*size[2]);foot=Counter((x,z) for x,y,z in state if y==0)
  stat={'path':str(p.relative_to(ROOT)),'unique':key,'size':size,'blocks':len(blocks),'palette':len(palette),
   'outOfBounds':outbounds,'duplicateCoordinates':duplicates,'doorPairs':doors,'unpairedDoors':brokenDoors,
   'stairs':stairs,'stairsWithoutSolidBelow':floaters,'fence_wall_trapdoor':chair,
   'blockCoverage':round(coverage,3),'materials':bytype.most_common(15)}
  if duplicates or outbounds or brokenDoors:fixes.append(stat)
  seen[key]=stat;result.append(stat)
 except Exception as e:fatal.append({'path':str(p.relative_to(ROOT)),'error':repr(e)})
 if ix%100==0:print('processed',ix,'unique',len(seen),'seconds',round(time.time()-start,1),flush=True)
report={'summary':{'all':len(loader.PATHS),'unique':len(seen),'parsed':len(result),'unreadable':len(fatal),
 'outOfBounds':sum(a['outOfBounds'] for a in seen.values()),
 'duplicateCoordinates':sum(a['duplicateCoordinates'] for a in seen.values()),
 'unpairedDoors':sum(a['unpairedDoors'] for a in seen.values()),
 'stairs':sum(a['stairs'] for a in seen.values()),
 'potentialUnsupportedStairs':sum(a['stairsWithoutSolidBelow'] for a in seen.values()),
 'elapsedSeconds':round(time.time()-start,2)},'problems':fixes,'errors':fatal,'allStructures':result}
(ROOT/'qa_v124_structures_detailed.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf8')
print(json.dumps(report['summary'],ensure_ascii=False),flush=True)
print('problemSamples',[(a['path'],a['unpairedDoors'],a['duplicateCoordinates']) for a in fixes[:15]])
print('examples',[(a['path'],a['size'],a['blocks']) for a in list(seen.values())[:7]])
