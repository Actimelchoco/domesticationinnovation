package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.block.DIBlockRegistry;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.*;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public class GuardianActivationRegression {
    @SuppressWarnings("unchecked")
    static List<ServerPlayer> online(GameTestHelper h)throws Exception {
        var field=net.minecraft.server.players.PlayerList.class.getDeclaredField("players");field.setAccessible(true);
        return (List<ServerPlayer>)field.get(h.getLevel().getServer().getPlayerList());
    }
    static void reset(GameTestHelper h){GuardianActivityService.rebuild(h.getLevel().getServer());}
    @GameTest(template="empty") public static void teamPresenceExpandsPostRadiusTo100(GameTestHelper h)throws Exception {
        try(var f=new GuardianChestRegression.Fixture(h)){
            f.power(true);f.tick();
            var visitor=SharedGuardianRegression.observer(f,UUID.randomUUID(),"TeamVisitor",new ArrayList<>());
            var manager=(TeamManagerImpl)FTBTeamsAPI.api().getManager();
            var party=new PartyTeam(manager,UUID.randomUUID());
            var ownerTeam=new PlayerTeam(manager,f.owner.getUUID());ownerTeam.setEffectiveTeam(party);
            var visitorTeam=new PlayerTeam(manager,visitor.getUUID());visitorTeam.setEffectiveTeam(party);
            var knownField=TeamManagerImpl.class.getDeclaredField("knownPlayers");knownField.setAccessible(true);
            @SuppressWarnings("rawtypes") Map known=(Map)knownField.get(manager);
            known.put(f.owner.getUUID(),ownerTeam);known.put(visitor.getUUID(),visitorTeam);
            online(h).add(visitor);
            try {
                visitor.setPos(f.post.getX()+100.5,f.post.getY(),f.post.getZ()+.5);reset(h);
                h.assertTrue(GuardianActivityService.hasNearbyPlayer(h.getLevel().getServer(),f.data),"online teammate activates at inclusive 100-block boundary");
                visitor.setPos(f.post.getX()+100.51,f.post.getY(),f.post.getZ()+.5);reset(h);
                h.assertTrue(!GuardianActivityService.hasNearbyPlayer(h.getLevel().getServer(),f.data),"beyond 100 blocks stays inactive");
                visitor.setPos(f.post.getX()+80.5,f.post.getY(),f.post.getZ()+.5);
                visitorTeam.setEffectiveTeam(visitorTeam);reset(h);
                h.assertTrue(!GuardianActivityService.hasNearbyPlayer(h.getLevel().getServer(),f.data),"unrelated player does not expand radius");
                visitor.setPos(f.post.getX()+20.5,f.post.getY(),f.post.getZ()+.5);reset(h);
                h.assertTrue(GuardianActivityService.hasNearbyPlayer(h.getLevel().getServer(),f.data),"ordinary 20-block visitor activation retained");
            }finally {online(h).remove(visitor);known.remove(f.owner.getUUID());known.remove(visitor.getUUID());reset(h);}
        }h.succeed();
    }
    @GameTest(template="empty") public static void protectDrumControlsOnlyLinkedPoweredGuardians(GameTestHelper h)throws Exception {
        try(var f=new GuardianChestRegression.Fixture(h)){
            var visitor=SharedGuardianRegression.observer(f,UUID.randomUUID(),"ProtectVisitor",new ArrayList<>());
            visitor.setPos(f.post.getX()+1000,f.post.getY(),f.post.getZ());
            var drum=new ItemStack(DIBlockRegistry.DRUM.get().asItem());drum.setHoverName(Component.literal("protect:ProtectVisitor"));
            f.container.setItem(2,drum);f.power(true);f.tick();
            online(h).add(visitor);
            try {
                h.assertTrue(GuardianChestAutomation.keepsActive(f.data),"named online player keeps linked guardian active at any distance");
                f.data.mode=TameMode.PASSIVE.id();TameCommands.enforceGuardianCommandLock(f.wolf,f.data);
                h.assertTrue(f.data.mode==TameMode.MONSTER_HUNTER.id(),"guardian is forced into hunter mode");
                f.container.setItem(2,ItemStack.EMPTY);f.scan();
                h.assertTrue(!GuardianChestAutomation.keepsActive(f.data),"removing protect drum revokes override");
                f.container.setItem(2,drum);f.scan();
                h.assertTrue(GuardianChestAutomation.keepsActive(f.data),"reinserted protect drum works without redstone edge");
                f.container.setItem(1,ItemStack.EMPTY);f.scan();
                h.assertTrue(!GuardianChestAutomation.keepsActive(f.data),"removing paper revokes override");
                f.container.setItem(1,f.paper);f.scan();f.power(false);
                h.assertTrue(!GuardianChestAutomation.keepsActive(f.data),"unpowered chest cannot keep a guardian active");
            }finally {online(h).remove(visitor);}
        }h.succeed();
    }
}
