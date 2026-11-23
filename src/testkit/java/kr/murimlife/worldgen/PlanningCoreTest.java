package kr.murimlife.worldgen;
import java.util.List;
import static kr.murimlife.worldgen.PlanningCore.*;
public class PlanningCoreTest {
    static int count=0;
    static void ok(boolean x, String reason) { count++;if (!x)throw new AssertionError(reason); }
    public static void main(String[] args) {
        Area a=new Area(0,0,191,191);
        ok(a.width()==192 && a.depth()==192,"inclusive dimensions");
        ok(a.hits(new Area(191,191,210,220)),"edge collision");
        ok(!a.hits(new Area(192,192,240,240)),"adjacent free");
        ok(a.expand(10).hits(new Area(201,0,210,20)),"gap enforcement");
        ok(!a.expand(9).hits(new Area(201,0,210,20)),"gap exact boundary");
        Claim named = new Claim("shaolin@1,1","shaolin",a,64,true);
        Claim farNamed = new Claim("shaolin@2,2","shaolin",new Area(3000,3000,3100,3100),64,true);
        ok(check(farNamed,List.of(named))==Rejection.UNIQUE_EXISTS,"unique named site");
        Claim nearOther = new Claim("city@1,1","city",new Area(250,0,400,150),64,false);
        ok(check(nearOther,List.of(named))==Rejection.RESERVED_COLLISION,"reserve padding");
        Claim farOther = new Claim("city@2,2","city",new Area(400,0,500,150),64,false);
        ok(check(farOther,List.of(named))==Rejection.ACCEPT,"noncollision");
        ok(check(named,List.of(named))==Rejection.DUPLICATE_KEY,"same ID");
        ok(cellSeed(13,2,-5)==cellSeed(13,2,-5),"deterministic");
        ok(cellSeed(13,2,-5)!=cellSeed(14,2,-5),"seed changes");
        ok(cellSeed(13,2,-5)!=cellSeed(13,2,-4),"cell changes");
        ok(median(new int[]{8,2,9,3,7},5)==7,"median");
        ok(median(new int[]{2,3,4,5},4)==4,"median even");
        boolean caught=false;try{new Area(1,2,0,1);}catch(IllegalArgumentException ex){caught=true;}
        ok(caught,"inverted rejected");
        System.out.println("PLANNING_CORE_TESTS_OK="+count);
    }
}
