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
    public static void duelAndRankedOpponentsIgnoreFriendshipAndRestoreItAfterward(GameTestHelper h){
        for(boolean ranked:List.of(false,true))try(var f=new Fixture(h)){
            var server=h.getLevel().getServer();
            TameDuelManager.startTeamDuel(server,f.a.getUUID(),Set.of(f.mine.getUUID()),f.b.getUUID(),Set.of(f.theirs.getUUID()),Set.of(),false,ranked);
            check(FriendlyOwnerProtection.blocks(f.b,f.a),"owners watching their pets do not become unprotected fighters");
            TameDuelManager.startTeamDuel(server,f.a.getUUID(),Set.of(f.mine.getUUID(),f.a.getUUID()),f.b.getUUID(),Set.of(f.theirs.getUUID(),f.b.getUUID()),Set.of(),false,ranked);
            check(TameDuelManager.areDuelOpponents(f.mine.getUUID(),f.theirs.getUUID()),"opposing fighters exist in casual/ranked match");
            check(!FriendlyOwnerProtection.blocks(f.theirs,f.mine)&&!FriendlyOwnerProtection.blocks(f.mine,f.theirs),"mutual friendship is bypassed for actual opposing fighters");
            check(!TameRegistry.isProtectedAttackTarget(f.mine,f.theirs),"registry target selection allows an opposing friend");
            check(!com.github.alexthe668.domesticationinnovation.server.entity.TameableUtils.shouldBlockOffensiveDiTarget(f.mine,f.theirs),"ability target selection allows an opposing friend");
            check(f.theirs.hurt(h.getLevel().damageSources().mobAttack(f.mine),3),"actual tame melee lands during casual/ranked battle");
            var arrow=new Arrow(h.getLevel(),f.a);var petArrow=new Arrow(h.getLevel(),f.mine);
            var cloud=new AreaEffectCloud(h.getLevel(),f.a.getX(),f.a.getY(),f.a.getZ());cloud.setOwner(f.a);
            var tnt=new PrimedTnt(h.getLevel(),f.a.getX(),f.a.getY(),f.a.getZ(),f.a);
            try{
                f.theirs.invulnerableTime=0;
                check(f.theirs.hurt(h.getLevel().damageSources().arrow(arrow,f.a),3),"actual player arrow lands on opposing tame");
                check(!FriendlyOwnerProtection.blocks(f.theirs,new DamageSource(h.getLevel().damageSources().arrow(petArrow,f.mine).typeHolder(),petArrow,null)),"direct-only pet arrow resolves the fighter");
                var impact=new ProjectileImpactEvent(arrow,new EntityHitResult(f.b));FriendlyOwnerProtection.impact(impact);check(!impact.isCanceled(),"opponent impact is not swallowed");
                check(!FriendlyOwnerProtection.blocks(f.theirs,cloud)&&FriendlyOwnerProtection.blocks(f.fox,cloud),"lingering effects bypass only participating opponents");
                check(f.theirs.addEffect(new MobEffectInstance(MobEffects.POISON,100),f.a),"opposing player can apply a harmful effect");
                var explosion=new Explosion(h.getLevel(),tnt,f.a.getX(),f.a.getY(),f.a.getZ(),4,false,Explosion.BlockInteraction.KEEP);
                var targets=new ArrayList<Entity>(List.of(f.b,f.theirs,f.fox));FriendlyOwnerProtection.explosion(new ExplosionEvent.Detonate(h.getLevel(),explosion,targets));
                check(targets.contains(f.b)&&targets.contains(f.theirs)&&!targets.contains(f.fox),"explosions retain opponents and exclude an unselected friendly pet");
                var attack=new LivingAttackEvent(f.b,h.getLevel().damageSources().playerAttack(f.a),4);attack.setCanceled(true);TameProtectionEvents.onAttackAllowDuelPlayerPvp(attack);check(!attack.isCanceled(),"duel PvP override allows opposing friendly players");
                var hurt=new LivingHurtEvent(f.b,h.getLevel().damageSources().playerAttack(f.a),4);hurt.setCanceled(true);TameProtectionEvents.onHurtAllowDuelPlayerPvp(hurt);check(!hurt.isCanceled(),"duel hurt override allows opposing friendly players");
                var damage=new LivingDamageEvent(f.theirs,h.getLevel().damageSources().mobAttack(f.mine),4);FriendlyOwnerProtection.damage(damage);check(!damage.isCanceled(),"final damage stage permits opponents");
                check(FriendlyOwnerProtection.blocks(f.fox,f.mine),"an unselected pet stays protected even when its owner is a fighter");
                TameDuelManager.endDuelsForOwner(server,f.a.getUUID());
                check(FriendlyOwnerProtection.blocks(f.theirs,petArrow)&&FriendlyOwnerProtection.blocks(f.b,arrow),"even previously fired projectiles are protected again after the match");
                check(TameRegistry.isProtectedAttackTarget(f.mine,f.theirs),"registry friendship protection returns after the match");
                check(PlayerDebugSettings.isDoNotAttackOwner(f.a.getUUID(),f.b.getUUID()),"saved friendship is never removed or edited");
            }finally{arrow.discard();petArrow.discard();cloud.discard();tnt.discard();}
        }h.succeed();
    }
    @GameTest(template="empty")
    public static void friendlyTeammatesAndDifferentBattlesStayProtected(GameTestHelper h){
        try(var f=new Fixture(h)){
            UUID enemyOwner=UUID.randomUUID();var enemy=f.pet(enemyOwner);var otherA=f.pet(UUID.randomUUID());var otherB=f.pet(UUID.randomUUID());
            try{
                var server=h.getLevel().getServer();
                TameDuelManager.startTeamDuelWithOwners(server,f.a.getUUID(),Set.of(f.mine.getUUID(),f.theirs.getUUID()),enemyOwner,Set.of(enemy.getUUID()),Set.of(f.a.getUUID(),f.b.getUUID()),Set.of(enemyOwner));
                check(FriendlyOwnerProtection.blocks(f.theirs,f.mine),"friendly owners on the same team stay protected");
                check(!f.theirs.hurt(h.getLevel().damageSources().mobAttack(f.mine),3),"actual friendly teammate damage remains blocked");
                TameDuelManager.endDuelsForOwner(server,f.a.getUUID());
                TameDuelManager.startTeamDuel(server,f.a.getUUID(),Set.of(f.mine.getUUID()),enemyOwner,Set.of(enemy.getUUID()));
                TameDuelManager.startTeamDuel(server,f.b.getUUID(),Set.of(f.theirs.getUUID()),UUID.randomUUID(),Set.of(otherB.getUUID()));
                check(FriendlyOwnerProtection.blocks(f.theirs,f.mine),"fighters in different battles do not bypass friendship");
                TameDuelManager.endDuelsForOwner(server,f.b.getUUID());
            }finally{for(var pet:List.of(enemy,otherA,otherB)){TameDuelManager.endDuelForEntity(h.getLevel().getServer(),pet.getUUID());TameRegistry.remove(pet.getUUID());pet.discard();}}
        }h.succeed();
    }
}
