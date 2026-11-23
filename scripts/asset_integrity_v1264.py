"""Validate historical resources against the explicit v1.26.4/v1.26.5 NBT change chains.
Never rewrite the v1.21 historical manifest, even when architecture gets updated.
"""
from pathlib import Path
import hashlib,json
ROOT=Path(__file__).resolve().parents[1]

def verify_legacy_binary_assets(root=ROOT):
    history=json.loads((root/'MANIFEST_SHA256_v121_historical.json').read_text(encoding='utf-8'))
    previous=json.loads((root/'ARCHITECTURE_V1264_MANIFEST.json').read_text(encoding='utf-8'))
    newer_file=root/'ARCHITECTURE_V1265_MANIFEST.json'
    newer=json.loads(newer_file.read_text(encoding='utf-8')) if newer_file.is_file() else {'modified_nbt':[]}
    v1264={d['path']:d for d in previous['modified_nbt']}
    v1265={d['path']:d for d in newer['modified_nbt']}
    historical={entry['path']:entry['sha256'] for entry in history['files']}
    for path,d in v1264.items():
        assert path.endswith('.nbt') and path.startswith('src/main/resources/data/'),path
        if path in historical:assert historical[path]==d['before_sha256'],('historical SHA changed',path)
    for path,d in v1265.items():
        assert path.endswith('.nbt') and path.startswith('src/main/resources/data/'),path
        expected_before=v1264[path]['after_sha256'] if path in v1264 else historical.get(path)
        if expected_before:assert expected_before==d['before_sha256'],('unexpected base NBT hash',path)
    intact=0,0
    unchanged,approved=0,0
    for entry in history['files']:
        path=entry['path']
        if not path.startswith('src/main/resources/') or not path.endswith(('.nbt','.png')):continue
        f=root/path;assert f.is_file(),path
        expected=(v1265[path]['after_sha256'] if path in v1265 else
                  v1264[path]['after_sha256'] if path in v1264 else entry['sha256'])
        assert hashlib.sha256(f.read_bytes()).hexdigest()==expected,('unapproved binary change',path)
        if path in v1264 or path in v1265:approved+=1
        else:unchanged+=1
    for path,d in dict(v1264,**v1265).items():
        final=v1265[path]['after_sha256'] if path in v1265 else v1264[path]['after_sha256']
        f=root/path
        assert f.exists() and hashlib.sha256(f.read_bytes()).hexdigest()==final,('altered NBT',path)
    return unchanged,approved
if __name__=='__main__':print('PASS approved architecture modifications; unchanged originals',verify_legacy_binary_assets())
