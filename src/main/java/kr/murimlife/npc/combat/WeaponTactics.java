package kr.murimlife.npc.combat;
import java.util.Locale;
/** Weapon families: maps game item IDs to actual Epic Fight preset attacks (no fake hit animations). */
public final class WeaponTactics {
    public enum Style {SWORD,SABER,SPEAR,STAFF,DAGGER,DUAL,HEAVY,FIST}
    private WeaponTactics(){}
    public static Style fromItemId(String id){
        String key=(id==null?"":id).toLowerCase(Locale.ROOT);
        if(key.contains("dagger")||key.contains("throwing_knife")||key.contains("needles"))return Style.DAGGER;
        if(key.contains("spear")||key.contains("glaive")||key.contains("trident"))return Style.SPEAR;
        if(key.contains("staff")||key.contains("stick")||key.contains("whip")||key.contains("chain"))return Style.STAFF;
        if(key.contains("great_jian")||key.contains("great_dao")||key.contains("hammer")||key.contains("mace")||key.contains("axe"))return Style.HEAVY;
        if(key.contains("dual")||key.contains("double"))return Style.DUAL;
        if(key.contains("weapon_dao")||key.contains("katana")||key.contains("tachi")||key.contains("saber"))return Style.SABER;
        if(key.contains("jian")||key.contains("sword"))return Style.SWORD;
        return Style.FIST;
    }
    public record Behavior(double reach,int minCooldown,int maxCooldown,boolean sidestep){}
    public static Behavior behavior(Style style){return switch(style){
        case SWORD->new Behavior(3.15,12,20,true);
        case SABER->new Behavior(3.25,14,23,true);
        case SPEAR->new Behavior(4.35,16,27,true);
        case STAFF->new Behavior(3.75,15,24,true);
        case DAGGER->new Behavior(2.35,7,13,true);
        case DUAL->new Behavior(2.75,9,16,true);
        case HEAVY->new Behavior(3.55,24,36,false);
        case FIST->new Behavior(2.10,10,17,true);
    };}
}
