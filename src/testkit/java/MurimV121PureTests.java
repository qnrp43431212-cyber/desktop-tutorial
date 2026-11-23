import kr.murimlife.npc.combat.ClashRules;
import kr.murimlife.npc.combat.MotionBlend;
import java.util.UUID;
public final class MurimV121PureTests{
 static int n=0;
 static void ok(boolean b,String message){n++;if(!b)throw new AssertionError(message);}
 public static void main(String...args){
  ok(MotionBlend.wrap(190)==-170,"wrap +");
  ok(MotionBlend.wrap(-190)==170,"wrap -");
  ok(MotionBlend.wrap(360)==0,"wrap full circle");
  ok(MotionBlend.approach(0,1,.2f,.1f)==.1f,"cap delta");
  ok(MotionBlend.approach(1,0,.2f,.1f)==.9f,"cap reverse");
  ok(MotionBlend.approachDegrees(179,-179,.5f,9f)==-180,"seam");
  ok(MotionBlend.movementSpeed(0,1,false)>0,"walk ease");
  ok(MotionBlend.movementSpeed(0,1,true)>MotionBlend.movementSpeed(0,1,false),"combat ease");
  ok(MotionBlend.approach(Float.NaN,0,.2f,.3f)==0,"finite guard");
  var a=new ClashRules.Strike(100,0,0,1,0);var b=new ClashRules.Strike(102,2,0,-1,0);
  ok(ClashRules.canClash(a,b,103,2,true),"valid weapon clash");
  ok(!ClashRules.canClash(a,b,106,2,true),"expire time");
  ok(!ClashRules.canClash(a,b,103,4,false),"no weapon");
  ok(!ClashRules.canClash(a,b,103,4,true),"out of range");
  ok(!ClashRules.canClash(a,new ClashRules.Strike(102,2,0,1,0),103,2,true),"facing");
  var idA=UUID.fromString("00000000-0000-0000-0000-000000000001");var idB=UUID.fromString("00000000-0000-0000-0000-000000000002");
  var j=new ClashRules.Journal();j.record(idA,a);j.record(idB,b);
  ok(j.evaluate(idA,idB,103,2,true),"start clash");
  ok(j.evaluate(idB,idA,105,2,true),"guard reverse hit");
  ok(!j.evaluate(idA,idB,112,2,true),"guard expires");
  ok(j.storedStrikes()==2,"strike journal");
  System.out.println("PASS "+n+" pure Java motion and clash assertions");
 }
}
