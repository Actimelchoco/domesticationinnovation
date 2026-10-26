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
    public static void interfaceTameShadowHandsDamageTarget(GameTestHelper helper) {
        verify(helper, EntityType.FOX);
    }

    @GameTest(template="empty")
    public static void vanillaTameShadowHandsDamageTarget(GameTestHelper helper) {
        verify(helper, EntityType.WOLF);
    }

    private static void verify(GameTestHelper helper, EntityType<? extends Mob> type) {
        Mob tame=type.create(helper.getLevel());
        var target=EntityType.ZOMBIE.create(helper.getLevel());
        try {
            UUID owner=UUID.randomUUID();
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
            for(int i=0;i<35;i++)DomesticationMod.PROXY.onLivingUpdate(new LivingEvent.LivingTickEvent(tame));
            if(target.getHealth()>=before)throw new AssertionError("Shadow Hands must damage target for "+type);
            if(TameableUtils.getShadowPunchTimes(tame).length!=3)throw new AssertionError("Shadow Hands must initialize all hands");
            data.abilityLevels.clear();
            DomesticationMod.PROXY.onLivingUpdate(new LivingEvent.LivingTickEvent(tame));
            if(TameableUtils.getShadowPunchTimes(tame).length!=0)throw new AssertionError("Removing ability must clear hands");
        } finally {
            TameableUtils.clearShadowHandState(tame);TameRegistry.remove(tame.getUUID());tame.discard();target.discard();
        }
        helper.succeed();
    }
}
