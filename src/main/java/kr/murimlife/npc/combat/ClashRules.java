package kr.murimlife.npc.combat;
import java.util.*;
/** Pure timing and geometry for weapon-to-weapon 合. No client random rolls or packet trust. */
public final class ClashRules {
    private ClashRules(){}
    public static final int WINDOW_TICKS=5, COOLDOWN_TICKS=12,GUARD_TICKS=4;
    public record Strike(long tick,double x,double z,double lookX,double lookZ){}
    public static boolean facing(Strike s,double targetX,double targetZ){
        double dx=targetX-s.x(),dz=targetZ-s.z();
        double distance=Math.hypot(dx,dz),front=Math.hypot(s.lookX(),s.lookZ());
        return distance<.01 || front>.01 && (dx*s.lookX()+dz*s.lookZ())/(distance*front)>=.25;
    }
    public static boolean canClash(Strike a,Strike b,long tick,double distance,boolean validGear){
        return validGear && a!=null&&b!=null && tick>=a.tick()&&tick>=b.tick()
            && tick-a.tick()<=WINDOW_TICKS && tick-b.tick()<=WINDOW_TICKS
            && Math.abs(a.tick()-b.tick())<=WINDOW_TICKS && distance<=3.8
            && facing(a,b.x(),b.z()) && facing(b,a.x(),a.z());
    }
    public static final class Journal {
        private final HashMap<UUID,Strike> swings=new HashMap<>();
        private final HashMap<String,Long> guards=new HashMap<>();
        public void record(UUID id,Strike strike){swings.put(id,strike);}
        private String pair(UUID a,UUID b){return a.compareTo(b)<0?a+":"+b:b+":"+a;}
        public boolean evaluate(UUID a,UUID b,long tick,double distance,boolean gear){
            String key=pair(a,b);Long guarded=guards.get(key);
            if(guarded!=null && tick<=guarded)return true;
            boolean clash=canClash(swings.get(a),swings.get(b),tick,distance,gear);
            if(clash)guards.put(key,tick+GUARD_TICKS);
            if(swings.size()>512)swings.entrySet().removeIf(e->tick-e.getValue().tick()>40);
            if(guards.size()>512)guards.entrySet().removeIf(e->tick>e.getValue());
            return clash;
        }
        public int storedStrikes(){return swings.size();}
    }
}
