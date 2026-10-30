package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexmodguy.alexscaves.server.entity.living.VallumraptorEntity;
import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.github.alexthe668.domesticationinnovation.server.entity.*;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.*;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

@GameTestHolder("domesticationinnovation") @PrefixGameTestTemplate(false)
public class VallumStealthRegression {
    static void check(boolean v,String message){if(!v)throw new GameTestAssertException(message);}
    static class Fixture implements AutoCloseable {
        final HeldItemTameWeaponsRegression.Fixture f;
        final Mob attacker;
        final VallumraptorEntity raptor;
        final TameData data;
        Fixture(GameTestHelper h)throws Exception{
            f=new HeldItemTameWeaponsRegression.Fixture(h);attacker=f.pet(false);data=TameRegistry.get(attacker.getUUID());data.hungerSaturation=100000;
            raptor=(VallumraptorEntity)ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("alexscaves","vallumraptor")).create(h.getLevel());
            raptor.setTame(true);raptor.setOwnerUUID(f.owner.getUUID());raptor.setNoAi(true);raptor.setPos(attacker.getX()+3,attacker.getY(),attacker.getZ());
            raptor.getAttribute(Attributes.MAX_HEALTH).setBaseValue(2000);raptor.setHealth(2000);raptor.getAttribute(Attributes.ARMOR).setBaseValue(0);
            h.getLevel().addFreshEntity(raptor);f.entities.add(raptor);TameRegistry.register(new TameData(raptor));
            if(f.owner.connection==null)f.owner.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(h.getLevel().getServer(),new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),f.owner);
        }
        void duel(Set<UUID> enemy){
            TameDuelManager.startTeamDuel(f.h.getLevel().getServer(),f.owner.getUUID(),Set.of(attacker.getUUID()),UUID.randomUUID(),enemy);
            check(TameDuelManager.areDuelOpponents(attacker.getUUID(),raptor.getUUID()),"duel active");attacker.setTarget(raptor);
        }
        void duel(){duel(Set.of(raptor.getUUID()));}
        public void close(){TameDuelManager.endDuelsForOwner(f.h.getLevel().getServer(),f.owner.getUUID());f.close();}
    }
    @GameTest(template="held_item_arena") public static void concealmentIsSpecificToEnemyDuelRaptors(GameTestHelper h)throws Exception{
        try(var f=new Fixture(h)){
            f.raptor.setHideFor(100);
            check(!TameCombatEvents.isHiddenDuelAbilityTarget(f.attacker,f.raptor),"outside duel unchanged");
            f.duel();f.raptor.setHideFor(100);
            check(TameCombatEvents.isHiddenDuelAbilityTarget(f.attacker,f.raptor),"native hide timer conceals enemy");
            check(TameableUtils.shouldBlockOffensiveDiTarget(f.attacker,f.raptor),"legacy ability selector respects cloak");
            check(!TameCombatEvents.isHiddenDuelAbilityTarget(f.raptor,f.raptor),"allies and self are not hidden from support");
            f.raptor.setHideFor(0);f.raptor.setInvisible(true);
            check(TameCombatEvents.isHiddenDuelAbilityTarget(f.attacker,f.raptor),"vanilla invisible flag also respected");
            f.raptor.setInvisible(false);
            check(!TameCombatEvents.isHiddenDuelAbilityTarget(f.attacker,f.raptor),"visible target restored");
            check(!TameableUtils.shouldBlockOffensiveDiTarget(f.attacker,f.raptor),"visible duel opponent can be targeted again");
        }h.succeed();
    }
    @GameTest(template="held_item_arena") public static void arrowsDoNotCastOrSpendCooldownOnHiddenRaptor(GameTestHelper h)throws Exception{
        try(var f=new Fixture(h)){
            f.duel();f.data.abilityLevels.put("arrow_shot",1);f.raptor.setHideFor(100);
            var method=TameAbilityEvents.class.getDeclaredMethod("handleArrowShot",net.minecraft.server.level.ServerLevel.class,LivingEntity.class,TameData.class,LivingEntity.class,long.class);method.setAccessible(true);
            method.invoke(null,h.getLevel(),f.attacker,f.data,f.raptor,h.getLevel().getGameTime());
            check(!f.data.cooldowns.containsKey("arrow"),"hidden target does not consume cast cooldown");
            f.raptor.setHideFor(0);
            method.invoke(null,h.getLevel(),f.attacker,f.data,f.raptor,h.getLevel().getGameTime());
            check(f.data.cooldowns.containsKey("arrow"),"visible target permits cast");
        }h.succeed();
    }
    @GameTest(template="held_item_arena") public static void hiddenRaptorIsSkippedForVisibleAlternate(GameTestHelper h)throws Exception{
        try(var f=new Fixture(h)){
            var visible=f.f.pet(false);visible.setPos(f.attacker.getX()+7,f.attacker.getY(),f.attacker.getZ());
            f.duel(Set.of(f.raptor.getUUID(),visible.getUUID()));f.raptor.setHideFor(100);visible.setInvisible(true);
            check(!TameCombatEvents.isHiddenDuelAbilityTarget(f.attacker,visible),"other invisible species keep existing targeting rules");
            var found=TameDuelManager.findNearestLoadedOpponent(h.getLevel().getServer(),f.attacker,candidate->!TameCombatEvents.isHiddenDuelAbilityTarget(f.attacker,candidate));
            check(found==visible,"ability acquires visible alternate rather than closest hidden opponent");
            var chain=TameAbilityEvents.class.getDeclaredMethod("findNearestChainTarget",net.minecraft.server.level.ServerLevel.class,LivingEntity.class,LivingEntity.class,Set.class,double.class);chain.setAccessible(true);
            check(chain.invoke(null,h.getLevel(),f.attacker,visible,Set.of(),12D)==null,"chain cannot jump to hidden raptor");
        }h.succeed();
    }
    @GameTest(template="held_item_arena") public static void shadowHandsDropCloakedTargetAndResume(GameTestHelper h)throws Exception{
        try(var f=new Fixture(h)){
            f.duel();f.data.abilityLevels.put("shadow_hands",1);
            for(int i=0;i<7;i++)DomesticationMod.PROXY.onLivingUpdate(new LivingEvent.LivingTickEvent(f.attacker));
            f.raptor.setHideFor(100);float before=f.raptor.getHealth();
            for(int i=0;i<30;i++)DomesticationMod.PROXY.onLivingUpdate(new LivingEvent.LivingTickEvent(f.attacker));
            check(TameableUtils.getPetAttackTargetID(f.attacker)==-1,"hands forget concealed target");
            check(f.raptor.getHealth()==before,"hands do not strike while hidden");
            f.raptor.setHideFor(0);
            for(int i=0;i<35;i++)DomesticationMod.PROXY.onLivingUpdate(new LivingEvent.LivingTickEvent(f.attacker));
            check(f.raptor.getHealth()<before,"hands resume when target reappears");
        }h.succeed();
    }
    @GameTest(template="held_item_arena") public static void existingHomingAbilitiesLoseHiddenTarget(GameTestHelper h)throws Exception{
        try(var f=new Fixture(h)){
            f.duel();var bullet=new TimedTameShulkerBullet(h.getLevel(),f.attacker,f.raptor,Direction.Axis.Y,1);h.getLevel().addFreshEntity(bullet);
            var chain=DIEntityRegistry.CHAIN_LIGHTNING.get().create(h.getLevel());chain.setCreatorEntityID(f.attacker.getId());chain.setToEntityID(f.raptor.getId());h.getLevel().addFreshEntity(chain);
            f.raptor.setHideFor(100);bullet.tick();chain.tick();
            check(bullet.isRemoved()&&chain.isRemoved(),"homing bullet and chain lose existing invisible target");
            float before=f.raptor.getHealth();f.raptor.invulnerableTime=0;
            check(f.raptor.hurt(f.raptor.damageSources().mobAttack(f.attacker),3F)&&f.raptor.getHealth()<before,"concealment is not damage immunity");
        }h.succeed();
    }
    @GameTest(template="held_item_arena") public static void magneticPullAndPsychicWallCannotTrackHiddenRaptor(GameTestHelper h)throws Exception{
        try(var f=new Fixture(h)){
            f.duel();f.data.abilityLevels.put("magnetic",1);f.data.abilityLevels.put("psychic_wall",1);
            f.raptor.setHideFor(100);TameableUtils.setMagneticTarget(f.attacker,f.raptor.getId());f.raptor.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            DomesticationMod.PROXY.onLivingUpdate(new LivingEvent.LivingTickEvent(f.attacker));
            check(f.raptor.getDeltaMovement().lengthSqr()==0,"magnetic ability cannot pull hidden opponent");
            check(TameableUtils.getMagneticTargetID(f.attacker)==-1,"magnetic target is forgotten");
            check(TameableUtils.getPsychicWallCooldown(f.attacker)==0,"psychic wall does not cast against unseen target");
        }h.succeed();
    }
    @GameTest(template="held_item_arena") public static void magneticVisualTargetSurvivesWithoutShadowHands(GameTestHelper h)throws Exception{
        try(var f=new Fixture(h)){
            f.duel();f.data.abilityLevels.put("magnetic",1);f.raptor.setHideFor(0);
            for(int i=0;i<4;i++)DomesticationMod.PROXY.onLivingUpdate(new LivingEvent.LivingTickEvent(f.attacker));
            check(TameableUtils.getMagneticTarget(f.attacker)==f.raptor,"magnetic target survives shadow-hand cleanup");
            var tag=com.github.alexthe666.citadel.server.entity.CitadelEntityData.getOrCreateCitadelTag(f.attacker);
            check(tag.getInt("PetMagneticTarget")==f.raptor.getId(),"visual target is synchronized even without a collar");
            check(TameableUtils.getPetAttackTargetID(f.attacker)==-1,"magnetic does not enable ghost shadow hands");
            check(f.raptor.getDeltaMovement().lengthSqr()>0,"magnetic pull still works on visible target");
            TameableUtils.setPetAttackTarget(f.attacker,f.raptor.getId());TameableUtils.clearShadowHandState(f.attacker);
            check(TameableUtils.getMagneticTarget(f.attacker)==f.raptor,"clearing shadow hands preserves magnet visual");
            f.data.abilityLevels.clear();DomesticationMod.PROXY.onLivingUpdate(new LivingEvent.LivingTickEvent(f.attacker));
            check(TameableUtils.getMagneticTargetID(f.attacker)==-1&&tag.getInt("PetMagneticTarget")==-1,"removing magnetic clears visual");
            tag.putInt("PetMagneticTarget",999999);
            DomesticationMod.PROXY.onLivingUpdate(new LivingEvent.LivingTickEvent(f.attacker));
            check(tag.getInt("PetMagneticTarget")==-1,"stale saved visual target is cleared without a runtime target");
        }h.succeed();
    }
}
