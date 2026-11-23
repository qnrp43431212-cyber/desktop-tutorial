package kr.murimlife.npc;
import kr.murimlife.npc.combat.MotionBlend;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
/** Genuine chunk-persistent wuxia humanoid. Its fight goals/animations are patched by Epic Fight. */
public class MurimHumanoid extends PathfinderMob {
    private static final EntityDataAccessor<Integer> SKIN=SynchedEntityData.defineId(MurimHumanoid.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WEAPON_STYLE=SynchedEntityData.defineId(MurimHumanoid.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DUEL=SynchedEntityData.defineId(MurimHumanoid.class,EntityDataSerializers.BOOLEAN);
    private float filteredYaw;
    public MurimHumanoid(EntityType<? extends MurimHumanoid> type,Level level){super(type,level);xpReward=0;setPersistenceRequired();}
    public static AttributeSupplier.Builder attributes(){return createMobAttributes()
        .add(Attributes.MAX_HEALTH,36).add(Attributes.MOVEMENT_SPEED,.29)
        .add(Attributes.ATTACK_DAMAGE,5).add(Attributes.ARMOR,3).add(Attributes.FOLLOW_RANGE,24);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){
        super.defineSynchedData(builder);builder.define(SKIN,346);builder.define(DUEL,false);builder.define(WEAPON_STYLE,kr.murimlife.npc.combat.WeaponTactics.Style.FIST.ordinal());
    }
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(5,new WaterAvoidingRandomStrollGoal(this,.75));
        // No forced player tracking: residents keep strolling and looking around naturally.
        goalSelector.addGoal(7,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
            this,Player.class,true,p->p instanceof Player player && kr.murimlife.cult.CultRaidService.hostileTo(this,player)));
        // Epic Fight registers the AnimatedAttackGoal and TargetChasingGoal via entity patch.
    }
    public int getSkin(){return entityData.get(SKIN);}
    public void setSkin(int skin){entityData.set(SKIN,Math.clamp(skin,0,965));}
    public boolean isInDuel(){return entityData.get(DUEL);}
    public void setDuel(boolean duel){entityData.set(DUEL,duel);}
    public void duelWith(LivingEntity target){setDuel(true);setTarget(target);}
    public kr.murimlife.npc.combat.WeaponTactics.Style getWeaponStyle(){
        var values=kr.murimlife.npc.combat.WeaponTactics.Style.values();
        int val=entityData.get(WEAPON_STYLE);
        return val>=0&&val<values.length?values[val]:kr.murimlife.npc.combat.WeaponTactics.Style.SWORD;
    }
    public void setWeaponStyle(kr.murimlife.npc.combat.WeaponTactics.Style style){
        entityData.set(WEAPON_STYLE,style.ordinal());
        String itemName=switch(style){
            case SWORD->"weapon_jian";
            case SABER->"weapon_dao";
            case SPEAR->"weapon_spear";
            case STAFF->"weapon_staff";
            case DAGGER,DUAL->"weapon_dagger";
            case HEAVY->"weapon_great_dao";
            case FIST->"";
        };
        if(itemName.isEmpty()){setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
            setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);return;}
        var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("murimlife",itemName));
        setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(item));
        setItemSlot(EquipmentSlot.OFFHAND,style==kr.murimlife.npc.combat.WeaponTactics.Style.DUAL?
            new ItemStack(item):ItemStack.EMPTY);
    }
    public void equipTrainingWeapon(){setWeaponStyle(kr.murimlife.npc.combat.WeaponTactics.Style.SWORD);}
    public void equipMasterWeapon(){
        entityData.set(WEAPON_STYLE,kr.murimlife.npc.combat.WeaponTactics.Style.SWORD.ordinal());
        setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.NETHERITE_SWORD));
    }
    public void setLeaderStats(boolean sado){
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(sado?120:80);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(sado?14:10);
        getAttribute(Attributes.ARMOR).setBaseValue(sado?12:8);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(sado?.34:.32);
        getPersistentData().putBoolean("MurimLeader",true);
        equipMasterWeapon();
        setHealth(getMaxHealth());
    }
    @Override public void tick(){
        super.tick();
        // Cap body-turn rate. Does not change Epic Fight shared player animation/interpolation.
        if(tickCount==1)filteredYaw=yBodyRot;
        filteredYaw=MotionBlend.approachDegrees(filteredYaw,yBodyRot,getTarget()!=null?.40f:.24f,14f);
        yBodyRot=filteredYaw;
        // Keep gait-speed changes gradual; combat pursuit retains a slightly faster stride.
        if(!level().isClientSide && !isNoAi()){
            var attribute=getAttribute(Attributes.MOVEMENT_SPEED);
            if(attribute!=null){
                float wanted=getTarget()!=null?.33f:(getNavigation().isDone()?.235f:.29f);
                if(getPersistentData().getBoolean("MurimLeader"))wanted+=.025f;
                attribute.setBaseValue(MotionBlend.approach((float)attribute.getBaseValue(),wanted,.18f,.015f));
            }
        }
    }
    @Override public boolean removeWhenFarAway(double distance){return false;}
    @Override public void addAdditionalSaveData(CompoundTag tag){
        super.addAdditionalSaveData(tag);tag.putInt("MurimWuxiaSkin",getSkin());tag.putBoolean("MurimDuel",isInDuel());tag.putInt("MurimWeaponStyle",entityData.get(WEAPON_STYLE));
    }
    @Override public void readAdditionalSaveData(CompoundTag tag){
        super.readAdditionalSaveData(tag);if(tag.contains("MurimWuxiaSkin"))setSkin(tag.getInt("MurimWuxiaSkin"));
        setDuel(tag.getBoolean("MurimDuel"));
        if(tag.contains("MurimWeaponStyle"))entityData.set(WEAPON_STYLE,tag.getInt("MurimWeaponStyle"));
        else entityData.set(WEAPON_STYLE,kr.murimlife.npc.combat.WeaponTactics.fromItemId(
            net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(getMainHandItem().getItem()).toString()).ordinal());
        // A world shutdown during a dialogue must never permanently freeze a resident NPC.
        if(getPersistentData().getBoolean("MurimLifeDialoguePause")){
            setNoAi(getPersistentData().getBoolean("MurimLifeDialogueOldNoAi"));
            getPersistentData().remove("MurimLifeDialoguePause");
            getPersistentData().remove("MurimLifeDialogueOldNoAi");
            setTarget(null); // interrupted combat must be reevaluated by the ordinary AI
        }
    }
}
