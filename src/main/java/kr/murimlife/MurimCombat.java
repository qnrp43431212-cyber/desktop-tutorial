package kr.murimlife;

import java.util.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import com.google.gson.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

/** Working generic game-side combat moves. Full per-art choreography is not implemented. */
public final class MurimCombat {
    private MurimCombat(){}
    public record Art(String name,String type,String weapon,int trainingRequired,int innerCost,int cooldownTicks,double power){}
    public static final Map<String,Art> ARTS;
    /** Source/tier metadata accompanies generic combat archetypes (no per-art unique motion yet). */
    public static final Map<String,String> ART_TIERS=new LinkedHashMap<>();
    public static final Map<String,String> ART_ORIGINS=new LinkedHashMap<>();
    static {
        var m=new LinkedHashMap<String,Art>();
        for(var a:new Art[]{
            new Art("기본검격","slash","jian",0,2,12,1.0),
            new Art("연환검","combo","jian",12,8,30,1.4),
            new Art("파공장","palm","gauntlet",16,12,30,1.7),
            new Art("태극흘리기","parry","unarmed",14,8,45,0),
            new Art("경공보","dash","unarmed",8,6,28,0),
            new Art("장풍","blast","gauntlet",21,18,50,2.0),
            new Art("호신강기","ward","unarmed",26,23,70,0),
            new Art("운기조식","breathe","unarmed",7,0,80,0),
            new Art("매화연검","combo","jian",30,16,44,2.1),
            new Art("타구봉격","slash","staff",22,10,32,1.8),
            new Art("혈수격","palm","claw",40,21,52,2.5),
            new Art("패천검격","blast","jian",70,36,90,3.0),
            new Art("호흡조절","passive","unarmed",0,0,0,0)
        })m.put(a.name(),a);
        // The research catalog is packaged with the mod. Adding entries is data-driven rather than
        // writing a separate Java method for each original martial name. Individual animations and
        // narrative acquisition routes are NOT yet implemented for every entry.
        try(InputStream input=MurimCombat.class.getResourceAsStream("/assets/murimlife/rules/martial_catalog.json")) {
            if(input!=null){
                JsonObject root=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
                for(JsonElement element:root.getAsJsonArray("martial_arts")){
                    JsonObject o=element.getAsJsonObject();
                    String name=o.get("name").getAsString();
                    String tier=o.get("tier").getAsString();
                    String kind=o.get("kind").getAsString();
                    String weapon=o.has("weapon_family")&&!o.get("weapon_family").isJsonNull()?o.get("weapon_family").getAsString():"unarmed";
                    weapon=switch(weapon){case "sword"->"jian";case "saber"->"dao";default->weapon;};
                    JsonObject balance=o.getAsJsonObject("balanced_effect");
                    String animation=balance.get("animation_archetype").getAsString();
                    String type="passive".equals(kind)?"passive":switch(animation){
                        case "blade_combo"->"combo";
                        case "hand_strike"->"palm";
                        case "weapon_sweep"->"slash";
                        case "meditation"->"ward";
                        default->"blast";
                    };
                    if(name.contains("보") && "martial_generic".equals(animation) && "active".equals(kind))type="dash";
                    int req=switch(tier){case "기초무공"->2;case "정통무공"->7;case "상승무공"->18;case "절기"->30;case "절학"->44;case "신공"->75;case "전설급"->110;default->22;};
                    int cost=balance.get("inner_cost").getAsInt();
                    int cooldown=Math.max(8,balance.get("cooldown_ticks").getAsInt());
                    double power=balance.get("damage_coefficient").getAsDouble();
                    m.putIfAbsent(name,new Art(name,type,weapon,req,cost,cooldown,power));
                    ART_TIERS.putIfAbsent(name,tier);
                    ART_ORIGINS.putIfAbsent(name,o.get("origin_class").getAsString());
                }
            }
        }catch(Exception ex){throw new IllegalStateException("Failed to load martial catalog",ex);}
        for(var candidate:kr.murimlife.cult.CultDesign.all()){
            Art secret=new Art(candidate.art(),candidate.type(),candidate.weapon(),candidate.training(),
                candidate.innerCost(),candidate.cooldown(),candidate.power());
            if(m.putIfAbsent(candidate.art(),secret)!=null)
                throw new IllegalStateException("Cult martial art overlaps an existing technique: "+candidate.art());
            ART_TIERS.put(candidate.art(),"사공");ART_ORIGINS.put(candidate.art(),candidate.cult());
        }
        ARTS=Collections.unmodifiableMap(m);
    }
    public static boolean learn(MurimProgressData.Profile p,String skill) {
        Art a=ARTS.get(skill);
        if(a==null || p.training<a.trainingRequired() || p.arts.contains(skill))return false;
        var demonic=kr.murimlife.cult.CultDesign.art(skill);
        if(demonic!=null && (p.realm<demonic.realm() || p.counters.getOrDefault("cultManual:"+skill,0)<1))
            return false; // Founding either cult never grants its arts. A verified defeat gives one proof.

        // Higher arts need a real discovery/transfer flag, not just repeatedly typing /murim learn.
        String tier=ART_TIERS.getOrDefault(skill,"기초무공");
        if((tier.equals("신공")||tier.equals("전설급")) &&
           p.counters.getOrDefault("manual:"+skill,0)<1)return false;
        return p.arts.add(skill);
    }
    public static boolean assign(MurimProgressData.Profile p,int slot,String skill){
        if(slot<0 || slot>=12||!p.arts.contains(skill))return false;
        Art art=ARTS.get(skill);
        if(art==null||"passive".equals(art.type()))return false;
        p.actionSlots[slot]=skill;return true;
    }
    private static String itemFamily(ItemStack item) {
        if(item.isEmpty()) return "unarmed";
        String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.getItem()).getPath();
        if(id.startsWith("weapon_")) {
            String n=id.substring(7);
            for(String w:ModItems.WEAPON_FAMILIES)if(n.equals(w)||n.startsWith(w+"_"))return w;
        }
        if(item.getItem() instanceof SwordItem)return "jian";
        return "other";
    }
    private static double compatibility(String needed,String equipped){
        if("unarmed".equals(needed))return 1;
        if(needed.equals(equipped))return 1;
        if((needed.equals("jian")&&equipped.equals("dao")) || (needed.equals("dao")&&equipped.equals("jian")))return .67;
        if(needed.equals("gauntlet")&&equipped.equals("unarmed"))return 1;
        if(needed.equals("staff")&&equipped.equals("spear"))return .67;
        if(needed.equals("claw")&&equipped.equals("gauntlet"))return .67;
        return 0;
    }
    public static void use(ServerPlayer player,int slot) {
        var state=MurimProgressData.get(player.server);
        var profile=state.player(player.getUUID(),player.getName().getString());
        if(slot<0||slot>=12)return;
        String id=profile.actionSlots[slot];
        if(id==null||!profile.arts.contains(id)){tell(player,"이 칸에는 배운 무공이 없습니다.");return;}
        var art=ARTS.get(id);
        if(art==null ||"passive".equals(art.type()))return;
        long now=player.server.overworld().getGameTime();
        double compat=compatibility(art.weapon(),itemFamily(player.getMainHandItem()));
        if(compat<=0){tell(player,"현재 무기로는 "+id+"을 사용할 수 없습니다.");return;}
        if(now-profile.lastCastTick<art.cooldownTicks()){tell(player,"초식을 다시 준비하고 있습니다.");return;}
        profile.restoreInner(now);
        if(profile.inner<art.innerCost()){tell(player,"내력이 부족합니다.");return;}
        if("breathe".equals(art.type())){
            kr.murimlife.qi.QiDeviationService.onBreathing(player);
            if(profile.qiSeverity>0){state.setDirty();return;}
        }
        profile.inner-=art.innerCost();profile.lastCastTick=now;profile.fatigue=Math.min(100,profile.fatigue+1.1+art.innerCost()*.08);
        profile.artMastery.merge(id,1,(old,inc)->Math.min(100,old+inc));
        ServerLevel world=player.serverLevel();Vec3 look=player.getLookAngle();
        String equippedId=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).getPath();
        double gradeMultiplier=1.0;
        String[] gradeIds=ModItems.WEAPON_GRADES;
        double[] gradeScale={1,1.15,1.35,1.6,1.95,2.3,2.8};
        for(int g=0;g<gradeIds.length;g++)if(equippedId.endsWith("_"+gradeIds[g]))gradeMultiplier=gradeScale[g];
        double damage=Math.min(120,Math.max(1,profile.power()*.017*art.power()*compat*gradeMultiplier));
        if(art.type().equals("parry")) {
            profile.parryUntil=now+10;world.sendParticles(ParticleTypes.ENCHANT,player.getX(),player.getY()+1,player.getZ(),18,.5,.5,.5,.05);
        } else if(art.type().equals("dash")) {
            player.push(look.x*1.3,.16,look.z*1.3);world.sendParticles(ParticleTypes.CLOUD,player.getX(),player.getY()+.1,player.getZ(),20,.5,.1,.5,.08);
        } else if(art.type().equals("ward")) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,120,0));
            world.sendParticles(ParticleTypes.ENCHANT,player.getX(),player.getY()+1,player.getZ(),28,.6,.8,.6,.06);
        } else if(art.type().equals("breathe")) {
            profile.inner=Math.min(100,profile.inner+18);profile.fatigue=Math.max(0,profile.fatigue-3);
            world.sendParticles(ParticleTypes.HAPPY_VILLAGER,player.getX(),player.getY()+1,player.getZ(),10,.4,.5,.4,.03);
        } else {
            final double reach = art.type().equals("blast")?8:art.type().equals("palm")?3.7:4.2;
            int hit=0;Vec3 eye=player.getEyePosition();
            AABB area=player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.6);
            for(LivingEntity target:world.getEntitiesOfClass(LivingEntity.class,area,e->e!=player && e.isAlive())) {
                Vec3 displacement=target.getBoundingBox().getCenter().subtract(eye);
                double projected=displacement.dot(look);
                if(projected<=0 || projected>reach || displacement.normalize().dot(look)<.72)continue;
                if(target instanceof ServerPlayer ally && player.getTeam()!=null && player.getTeam()==ally.getTeam())continue;
                if(target.hurt(player.damageSources().playerAttack(player),(float)damage)){
                    target.push(look.x*.2, .08,look.z*.2);
                    world.sendParticles(ParticleTypes.CRIT,target.getX(),target.getY()+target.getBbHeight()*.6,target.getZ(),18,.35,.5,.35,.1);
                    hit++;if(hit>= (art.type().equals("combo")?3:art.type().equals("blast")?5:1))break;
                }
            }
            if(hit==0)world.sendParticles(ParticleTypes.SWEEP_ATTACK,player.getX()+look.x*2,player.getY()+1.1,player.getZ()+look.z*2,3,.2,.2,.2,.01);
        }
        state.setDirty();tell(player,"시전: "+id);
    }
    private static void tell(ServerPlayer player,String msg) {player.displayClientMessage(Component.literal("[무림] "+msg),true);}
}
