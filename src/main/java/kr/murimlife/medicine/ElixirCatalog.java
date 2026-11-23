package kr.murimlife.medicine;
import java.util.*;
/** Unique medicine ids, descriptions, server rules, and recipes. All prices in copper mun. */
public final class ElixirCatalog {
    public record Formula(String id,String title,String school,boolean pill,int strain,int inner,int fatigue,
                          int insight,int price,String ingredientA,int countA,String ingredientB,int countB){}
    private static final LinkedHashMap<String,Formula> ALL=new LinkedHashMap<>();
    static {
        add("ginseng_root","인삼","공통",false,5,7,-3,0,75,"minecraft:carrot",3,"minecraft:glow_berries",2);
        add("snow_lotus","설련화","아미",false,6,11,-4,1,180,"minecraft:white_tulip",3,"minecraft:packed_ice",2);
        add("blood_spirit_herb","혈령초","혈교",false,15,21,2,1,320,"minecraft:nether_wart",3,"minecraft:red_mushroom",2);
        add("yin_moss","음태이끼","무당",false,8,8,-8,1,115,"minecraft:moss_block",2,"minecraft:glow_berries",2);
        add("fire_ganoderma","화지영지","화산",false,12,19,3,2,260,"minecraft:crimson_fungus",2,"minecraft:blaze_powder",1);
        add("ice_petal","한빙화","곤륜",false,10,14,-5,1,190,"minecraft:blue_orchid",2,"minecraft:packed_ice",2);
        add("jade_bamboo","청옥죽수","청성",false,4,8,-7,0,100,"minecraft:bamboo",4,"minecraft:glow_berries",2);
        add("poison_vine","독령등","사천당문",false,19,23,8,2,270,"minecraft:spider_eye",2,"minecraft:vine",3);
        add("clear_mind_pill","청심단","소림",true,6,5,-19,2,250,"minecraft:honey_bottle",1,"minecraft:glow_berries",3);
        add("minor_inner_pill","소환단","공통",true,9,19,-5,1,350,"minecraft:golden_carrot",2,"minecraft:glow_berries",3);
        add("great_inner_pill","대환단","소림",true,27,48,4,5,1800,"minecraft:golden_apple",1,"minecraft:glowstone_dust",3);
        add("bone_pill","속골단","개방",true,9,11,-15,1,420,"minecraft:bone_meal",4,"minecraft:honey_bottle",1);
        add("jade_marow_pill","옥수단","남궁세가",true,16,24,-9,3,620,"minecraft:amethyst_shard",3,"minecraft:golden_carrot",1);
        add("nine_turn_pill","구전영단","무당",true,34,47,-4,7,2300,"minecraft:ghast_tear",1,"minecraft:glowstone_dust",4);
        add("soul_stilling_pill","안혼단","아미",true,8,7,-25,3,550,"minecraft:phantom_membrane",1,"minecraft:honey_bottle",2);
        add("dragon_blood_pill","용혈단","혈교",true,38,59,12,7,2600,"minecraft:fermented_spider_eye",2,"minecraft:blaze_powder",2);
        add("demon_heart_pill","마심단","마교",true,40,56,15,6,2500,"minecraft:magma_cream",3,"minecraft:nether_wart",4);
        add("purifying_pill","정기단","종남",true,4,8,-13,1,390,"minecraft:milk_bucket",1,"minecraft:honey_bottle",1);
        add("tendon_pill","세맥단","점창",true,18,25,-1,4,790,"minecraft:rabbit_foot",1,"minecraft:glow_berries",4);
        add("heavenly_pill","천원단","공동",true,32,47,4,6,2100,"minecraft:enchanted_golden_apple",1,"minecraft:glowstone_dust",4);
        add("red_mist_pill","홍무단","사도련",true,24,36,7,3,900,"minecraft:redstone",6,"minecraft:fermented_spider_eye",1);
        add("seven_stars_pill","칠성단","제갈세가",true,14,21,-12,3,690,"minecraft:lapis_lazuli",4,"minecraft:golden_carrot",2);
        add("marrow_rebirth_pill","탈태환골단","모용세가",true,35,63,8,9,3200,"minecraft:nether_star",1,"minecraft:golden_apple",2);
        add("unknown_elixir","미확인 영단","행상",true,20,25,0,2,420,"minecraft:fermented_spider_eye",1,"minecraft:glow_berries",3);
    }
    private static void add(String id,String name,String school,boolean pill,int strain,int inner,int fatigue,int insight,int price,String a,int ca,String b,int cb){
        if(ALL.putIfAbsent(id,new Formula(id,name,school,pill,strain,inner,fatigue,insight,price,a,ca,b,cb))!=null)
            throw new IllegalStateException("Duplicate elixir id: "+id);
    }
    public static Formula get(String id){return ALL.get(id);}
    public static Collection<Formula> all(){return Collections.unmodifiableCollection(ALL.values());}
    public static List<Formula> recipes(){return ALL.values().stream().filter(e->!e.id().equals("unknown_elixir")).toList();}
}
