package kr.murimlife.npc.epic;
import kr.murimlife.npc.MurimHumanoid;
import kr.murimlife.npc.combat.WeaponTactics;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.entitypatch.Factions;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;
import yesman.epicfight.world.entity.ai.goal.AnimatedAttackGoal;
import yesman.epicfight.world.entity.ai.goal.CombatBehaviors;
import yesman.epicfight.world.entity.ai.goal.TargetChasingGoal;
/** Epic Fight preset animation-based melee AI for eight weapon families.
 * Style conditions are checked on each behavior, allowing later weapon changes.
 * Staff/fist currently use compatible presets; custom staff/fist motion files remain future work.
 */
public final class MurimEpicPatch extends HumanoidMobPatch<MurimHumanoid> {
    public MurimEpicPatch(MurimHumanoid npc){super(npc,Factions.VILLAGER);}
    @Override public void updateMotion(boolean consider){commonMobUpdateMotion(consider);}
    private CombatBehaviors.BehaviorSeries.Builder<MurimEpicPatch> series(
        WeaponTactics.Style style,int cooldown,float weight,double distance,
        AnimationManager.AnimationAccessor<? extends StaticAnimation> animation){
        return new CombatBehaviors.BehaviorSeries.Builder<MurimEpicPatch>()
            .weight(weight).cooldown(cooldown)
            .nextBehavior(new CombatBehaviors.Behavior.Builder<MurimEpicPatch>()
                .custom(p->p.original.getWeaponStyle()==style && !p.original.isNoAi())
                .withinDistance(0.0,distance).animationBehavior(animation));
    }
    @Override protected void initAI(){
        super.initAI();
        var combos=new CombatBehaviors.Builder<MurimEpicPatch>()
            .newBehaviorSeries(series(WeaponTactics.Style.SWORD,14,4,3.15,Animations.BIPED_MOB_ONEHAND1))
            .newBehaviorSeries(series(WeaponTactics.Style.SWORD,21,3,3.15,Animations.BIPED_MOB_LONGSWORD2))
            .newBehaviorSeries(series(WeaponTactics.Style.SABER,16,4,3.25,Animations.BIPED_MOB_TACHI))
            .newBehaviorSeries(series(WeaponTactics.Style.SABER,23,2,3.25,Animations.BIPED_MOB_UCHIGATANA1))
            .newBehaviorSeries(series(WeaponTactics.Style.SPEAR,18,4,4.35,Animations.BIPED_MOB_SPEAR_TWOHAND1))
            .newBehaviorSeries(series(WeaponTactics.Style.SPEAR,24,3,4.35,Animations.BIPED_MOB_SPEAR_TWOHAND2))
            .newBehaviorSeries(series(WeaponTactics.Style.STAFF,20,4,3.75,Animations.BIPED_MOB_SPEAR_ONEHAND))
            .newBehaviorSeries(series(WeaponTactics.Style.STAFF,24,2,3.75,Animations.BIPED_MOB_SPEAR_TWOHAND3))
            .newBehaviorSeries(series(WeaponTactics.Style.DAGGER,9,5,2.35,Animations.BIPED_MOB_DAGGER_ONEHAND1))
            .newBehaviorSeries(series(WeaponTactics.Style.DAGGER,13,3,2.35,Animations.BIPED_MOB_DAGGER_ONEHAND2))
            .newBehaviorSeries(series(WeaponTactics.Style.DUAL,12,5,2.75,Animations.BIPED_MOB_SWORD_DUAL1))
            .newBehaviorSeries(series(WeaponTactics.Style.DUAL,16,3,2.75,Animations.BIPED_MOB_SWORD_DUAL2))
            .newBehaviorSeries(series(WeaponTactics.Style.HEAVY,28,5,3.55,Animations.BIPED_MOB_GREATSWORD))
            .newBehaviorSeries(series(WeaponTactics.Style.FIST,12,5,2.10,Animations.BIPED_MOB_ONEHAND2))
            .build(this);
        original.goalSelector.addGoal(1,new AnimatedAttackGoal<>(this,combos));
        original.goalSelector.addGoal(2,new TargetChasingGoal(this,original,1.1,true));
    }
    @Override public void initAnimator(Animator animator){
        super.initAnimator(animator);
        animator.addLivingAnimation(LivingMotions.IDLE,Animations.BIPED_IDLE);
        animator.addLivingAnimation(LivingMotions.WALK,Animations.BIPED_WALK);
        animator.addLivingAnimation(LivingMotions.RUN,Animations.BIPED_RUN);
        animator.addLivingAnimation(LivingMotions.FALL,Animations.BIPED_FALL);
        animator.addLivingAnimation(LivingMotions.JUMP,Animations.BIPED_JUMP);
        animator.addLivingAnimation(LivingMotions.DEATH,Animations.BIPED_DEATH);
    }
}
