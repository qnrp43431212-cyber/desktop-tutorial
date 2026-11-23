import gzip, struct
from pathlib import Path
class NbtReader:
 def __init__(self,b):self.b=memoryview(b);self.p=0
 def read(self,n):
  b=self.b[self.p:self.p+n];self.p+=n;return b
 def num(self,fmt): return struct.unpack('>'+fmt,self.read(struct.calcsize('>'+fmt)))[0]
 def string(self):return bytes(self.read(self.num('H'))).decode('utf8')
 def payload(self,t):
  if t==1:return self.num('b')
  if t==2:return self.num('h')
  if t==3:return self.num('i')
  if t==4:return self.num('q')
  if t==5:return self.num('f')
  if t==6:return self.num('d')
  if t==7:return bytes(self.read(self.num('i')))
  if t==8:return self.string()
  if t==9:
   child=self.num('B');n=self.num('i');return [self.payload(child) for _ in range(n)]
  if t==10:
   d={}
   while True:
    child=self.num('B')
    if child==0:break
    key=self.string();d[key]=self.payload(child)
   return d
  if t==11:return [self.num('i') for _ in range(self.num('i'))]
  if t==12:return [self.num('q') for _ in range(self.num('i'))]
  raise ValueError(t)
 def parse(self):
  t=self.num('B');self.string();return self.payload(t)
def load(path):
 b=Path(path).read_bytes()
 if b[:2]==b'\x1f\x8b':b=gzip.decompress(b)
 return NbtReader(b).parse()
