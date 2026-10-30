package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.github.alexthe668.domesticationinnovation.server.entity.TameableUtils;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.gametest.*;
import java.util.UUID;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public class ShadowHandsRegression {
    @GameTest(template="empty")
    public static void interfaceTameShadowHandsDamageTarget(GameTestHelper helper) throws Exception {
        verify(helper, EntityType.FOX);
    }

    @GameTest(template="empty")
    public static void vanillaTameShadowHandsDamageTarget(GameTestHelper helper) throws Exception {
        verify(helper, EntityType.WOLF);
    }

    private static void verify(GameTestHelper helper, EntityType<? extends Mob> type) throws Exception {
        Mob tame=type.create(helper.getLevel());
        var target=EntityType.ZOMBIE.create(helper.getLevel());
        var fakeOwner=new net.minecraftforge.common.util.FakePlayer(helper.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ShadowOwner"));
        var field=net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");
        field.setAccessible(true);
        @SuppressWarnings("unchecked") var players=(java.util.Map<UUID,net.minecraft.server.level.ServerPlayer>)field.get(helper.getLevel().getServer().getPlayerList());
        players.put(fakeOwner.getUUID(),fakeOwner);
        helper.getLevel().addNewPlayer(fakeOwner);
        try {
            UUID owner=fakeOwner.getUUID();
            TameEntityAdapter.setOwner(tame,owner);
            TameData data=new TameData(tame,owner,true);
            data.abilityLevels.put("shadow_hands",3);
            TameRegistry.register(data);
            var pos=helper.absolutePos(new net.minecraft.core.BlockPos(2,2,2));
            tame.setPos(pos.getX(),pos.getY(),pos.getZ());
            target.setPos(pos.getX()+2,pos.getY(),pos.getZ());
            tame.setNoAi(true);target.setNoAi(true);
            helper.getLevel().addFreshEntity(tame);helper.getLevel().addFreshEntity(target);
            tame.setTarget(target);
            float before=target.getHealth();
            boolean fullyExtended=false, fullyRetracted=false, damaged=false;
            int previous=0;
            for(int i=0;i<60;i++) {
                DomesticationMod.PROXY.onLivingUpdate(new LivingEvent.LivingTickEvent(tame));
                var tag=com.github.alexthe666.citadel.server.entity.CitadelEntityData.getOrCreateCitadelTag(tame);
                if(!tag.contains("PetAttackTarget") || tag.getInt("PetAttackTarget")!=target.getId())throw new AssertionError("Client visual target must be synchronized");
                int[] progress=TameableUtils.getShadowPunchTimes(tame);
                if(progress.length>0) {
                    int current=progress[0];
                    if(current==10)fullyExtended=true;
                    if(fullyExtended && current==0)fullyRetracted=true;
                    if(Math.abs(current-previous)>1)throw new AssertionError("Punch progress must change smoothly");
                    previous=current;
                }
                damaged |= target.getHealth() < before;
                // Keep the target alive while checking several complete animation cycles.
                target.setHealth(target.getMaxHealth());target.invulnerableTime=0;
            }
            if(!fullyExtended || !fullyRetracted)throw new AssertionError("Hands must complete reach AND retraction, rather than flicker at full extension");
            if(!damaged)throw new AssertionError("Shadow Hands must damage target for "+type);
            if(TameableUtils.getShadowPunchTimes(tame).length!=3)throw new AssertionError("Shadow Hands must initialize all hands");
            data.abilityLevels.clear();
            DomesticationMod.PROXY.onLivingUpdate(new LivingEvent.LivingTickEvent(tame));
            if(TameableUtils.getShadowPunchTimes(tame).length!=0)throw new AssertionError("Removing ability must clear hands");
            if(com.github.alexthe666.citadel.server.entity.CitadelEntityData.getOrCreateCitadelTag(tame).getInt("PetAttackTarget")!=-1)throw new AssertionError("Removing ability clears synchronized target");
        } finally {
            helper.getLevel().removePlayerImmediately(fakeOwner,Entity.RemovalReason.DISCARDED);players.remove(fakeOwner.getUUID());
            TameableUtils.clearShadowHandState(tame);TameRegistry.remove(tame.getUUID());tame.discard();target.discard();
        }
        helper.succeed();
    }
}
