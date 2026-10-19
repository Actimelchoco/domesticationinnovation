package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.*;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public class FriendlyOwnersRegression {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static final class Fixture implements AutoCloseable {
        final GameTestHelper h;final FakePlayer a,b;final Wolf mine,theirs;final LivingEntity fox;
        Fixture(GameTestHelper helper){
            h=helper;a=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"FriendlyA"));
            b=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"FriendlyB"));
            var pos=h.absolutePos(new net.minecraft.core.BlockPos(2,2,2));a.setPos(pos.getX(),pos.getY(),pos.getZ());b.setPos(pos.getX()+2,pos.getY(),pos.getZ());
            mine=pet(a.getUUID());theirs=pet(b.getUUID());fox=EntityType.FOX.create(h.getLevel());TameEntityAdapter.setOwner(fox,b.getUUID());
            fox.setPos(pos.getX()+3,pos.getY(),pos.getZ());h.getLevel().addFreshEntity(fox);
            TameRegistry.register(new TameData(fox,b.getUUID(),true));
            PlayerDebugSettings.setTamesFriendly(a.getUUID(),false);PlayerDebugSettings.setTamesFriendly(b.getUUID(),false);
            PlayerDebugSettings.setDoNotAttackOwner(a.getUUID(),b.getUUID(),true);
        }
        Wolf pet(UUID owner){var wolf=EntityType.WOLF.create(h.getLevel());TameEntityAdapter.setOwner(wolf,owner);wolf.setPos(a.getX(),a.getY(),a.getZ());h.getLevel().addFreshEntity(wolf);TameRegistry.register(new TameData(wolf));return wolf;}
        @Override public void close(){
            TameDuelManager.endDuelsForOwner(h.getLevel().getServer(),a.getUUID());
            PlayerDebugSettings.setDoNotAttackOwner(a.getUUID(),b.getUUID(),false);
            PlayerDebugSettings.setTamesFriendly(a.getUUID(),true);PlayerDebugSettings.setTamesFriendly(b.getUUID(),true);
            TameIllBloodManager.clearBetween(a.getUUID(),b.getUUID());
            for(var body:List.of(mine,theirs,fox)){TameRegistry.remove(body.getUUID());body.discard();}
        }
    }
    @GameTest(template="empty")
    public static void bowAndTameProjectilesCannotDamageFriendlyPlayersOrPets(GameTestHelper h){
        try(var f=new Fixture(h)){
            var arrow=new Arrow(h.getLevel(),f.a);var petArrow=new Arrow(h.getLevel(),f.mine);
            try{
                for(var victim:List.of(f.b,f.theirs,f.fox)){
                    float health=victim.getHealth();
                    check(!victim.hurt(h.getLevel().damageSources().arrow(arrow,f.a),6)&&victim.getHealth()==health,"actual player arrow damage is blocked");
                    check(!victim.hurt(h.getLevel().damageSources().arrow(petArrow,f.mine),6)&&victim.getHealth()==health,"actual tame arrow damage is blocked");
                    var directOnly=new DamageSource(h.getLevel().damageSources().arrow(arrow,f.a).typeHolder(),arrow,null);
                    check(FriendlyOwnerProtection.blocks(victim,directOnly),"direct-only projectile resolves real shooter");
                    var impact=new ProjectileImpactEvent(arrow,new EntityHitResult(victim));FriendlyOwnerProtection.impact(impact);
                    check(impact.isCanceled()&&!victim.isOnFire(),"impact blocked before Flame or other on-hit effects");
                    check(!f.mine.doHurtTarget(victim),"native tame melee blocked before pre-damage weapon effects");
                }
                check(FriendlyOwnerProtection.blocks(f.mine,h.getLevel().damageSources().playerAttack(f.b)),"existing mutual protection is preserved");
                PlayerDebugSettings.setDoNotAttackOwner(f.a.getUUID(),f.b.getUUID(),false);
                check(!FriendlyOwnerProtection.blocks(f.theirs,h.getLevel().damageSources().arrow(arrow,f.a)),"removing owner removes this protection");
            }finally{arrow.discard();petArrow.discard();}
        }h.succeed();
    }
    @GameTest(template="empty")
    public static void explosionsPotionsAndUnloadedProjectileOwnersStayProtected(GameTestHelper h){
        try(var f=new Fixture(h)){
            var tnt=new PrimedTnt(h.getLevel(),f.a.getX(),f.a.getY(),f.a.getZ(),f.a);
            var cloud=new AreaEffectCloud(h.getLevel(),f.a.getX(),f.a.getY(),f.a.getZ());cloud.setOwner(f.a);
            var arrow=new Arrow(h.getLevel(),f.mine);
            try{
                var explosion=new Explosion(h.getLevel(),tnt,f.a.getX(),f.a.getY(),f.a.getZ(),4,false,Explosion.BlockInteraction.KEEP);
                var targets=new ArrayList<Entity>(List.of(f.b,f.theirs,f.fox));
                var event=new ExplosionEvent.Detonate(h.getLevel(),explosion,targets);FriendlyOwnerProtection.explosion(event);
                check(targets.isEmpty(),"friends excluded from explosion damage and knockback");
                check(!f.theirs.addEffect(new MobEffectInstance(MobEffects.POISON,100),f.a)&&!f.theirs.hasEffect(MobEffects.POISON),"harmful splash effect from player blocked");
                check(!f.theirs.addEffect(new MobEffectInstance(MobEffects.WITHER,100),cloud),"lingering cloud owner resolved");
                check(f.theirs.addEffect(new MobEffectInstance(MobEffects.REGENERATION,100),f.a),"beneficial effects remain allowed");
                var bolt=EntityType.LIGHTNING_BOLT.create(h.getLevel());bolt.setCause(f.a);
                try {float health=f.theirs.getHealth();f.theirs.thunderHit(h.getLevel(),bolt);
                    check(!f.theirs.isOnFire()&&f.theirs.getHealth()==health,"owned lightning blocked before ignition and damage");}
                finally {bolt.discard();}
                f.mine.discard();
                check(f.a.getUUID().equals(FriendlyOwnerProtection.owner(arrow)),"projectile still resolves offline/unloaded tame owner from saved registry");
                check(FriendlyOwnerProtection.blocks(f.theirs,new DamageSource(h.getLevel().damageSources().genericKill().typeHolder(),f.a)),"bypass damage types do not bypass friendship");
                check(!FriendlyOwnerProtection.blocks(f.theirs,h.getLevel().damageSources().fall()),"unrelated environmental damage is not falsely attributed");
            }finally{tnt.discard();cloud.discard();arrow.discard();}
        }h.succeed();
    }
    @GameTest(template="empty")
    public static void friendlyOwnerCommandsAndLegacySavedListsMigrate(GameTestHelper h)throws Exception{
        try(var f=new Fixture(h)){
            var dispatcher=h.getLevel().getServer().getCommands().getDispatcher();var source=f.a.createCommandSourceStack().withPermission(0);
            String id=f.b.getUUID().toString();
            check(dispatcher.execute("tames friendlyOwners remove "+id,source)==1,"new root remove works");
            check(dispatcher.execute("tames settings combat friendlyOwners add "+id,source)==1,"categorized add works");
            check(dispatcher.execute("tames settings combat friendlyOwners",source)==1,"categorized settings list works");
            check(dispatcher.getRoot().getChild("tames").getChild("settings").getChild("combat").getChild("doNotAttackOwner")==null,"old setting name removed");
            var saved=PlayerDebugSettings.saveAll();
            try{
                var legacy=new CompoundTag();var list=new ListTag();var owner=new CompoundTag();owner.putUUID("owner",f.b.getUUID());list.add(owner);
                legacy.put("doNotAttackOwners",list);PlayerDebugSettings.loadAll(Map.of(f.a.getUUID(),legacy));
                check(PlayerDebugSettings.isDoNotAttackOwner(f.a.getUUID(),f.b.getUUID()),"legacy friend entry loads");
                var migrated=PlayerDebugSettings.saveAll();
                check(migrated.get(f.a.getUUID()).contains("friendlyOwners")&&!migrated.get(f.a.getUUID()).contains("doNotAttackOwners"),"saved list migrates to new key");
                PlayerDebugSettings.loadAll(migrated);check(PlayerDebugSettings.isDoNotAttackOwner(f.a.getUUID(),f.b.getUUID()),"new list survives reload");
            }finally{PlayerDebugSettings.loadAll(saved);}
        }h.succeed();
    }
    @GameTest(template="empty")
    public static void duelsAndConflictsCannotBypassExplicitFriendlyOwners(GameTestHelper h){
        try(var f=new Fixture(h)){
            TameDuelManager.startTeamDuel(h.getLevel().getServer(),f.a.getUUID(),Set.of(f.mine.getUUID()),f.b.getUUID(),Set.of(f.theirs.getUUID()));
            check(TameDuelManager.areDuelOpponents(f.mine.getUUID(),f.theirs.getUUID()),"fixture has opposing duel participants");
            var source=h.getLevel().damageSources().mobAttack(f.mine);
            check(!f.theirs.hurt(source,5),"duel does not override explicit friend protection");
            var event=new LivingAttackEvent(f.b,h.getLevel().damageSources().playerAttack(f.a),4);event.setCanceled(true);
            TameProtectionEvents.onAttackAllowDuelPlayerPvp(event);check(event.isCanceled(),"duel PvP override cannot uncancel friend damage");
            var hurt=new LivingHurtEvent(f.b,h.getLevel().damageSources().playerAttack(f.a),4);hurt.setCanceled(true);
            TameProtectionEvents.onHurtAllowDuelPlayerPvp(hurt);check(hurt.isCanceled(),"duel hurt override cannot uncancel friend damage");
            var damage=new LivingDamageEvent(f.theirs,source,4);FriendlyOwnerProtection.damage(damage);check(damage.isCanceled(),"final damage stage also protected");
            check(TameRegistry.isProtectedAttackTarget(f.a.getUUID(),f.theirs),"target selection recognizes friendly owner");
        }h.succeed();
    }
}
