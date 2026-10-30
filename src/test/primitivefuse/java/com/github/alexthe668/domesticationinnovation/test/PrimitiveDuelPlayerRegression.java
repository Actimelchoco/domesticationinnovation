package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.entity.ModifedToBeTameable;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.PrimitiveMobsCompat;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import java.util.*;

public final class PrimitiveDuelPlayerRegression {
    @SuppressWarnings("unchecked") static void verify(PrimitiveFuseRegression.Fixture h)throws Exception{
        var server=h.getLevel().getServer();
        java.lang.reflect.Field field;
        try{field=net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");}
        catch(NoSuchFieldException ignored){field=net.minecraft.server.players.PlayerList.class.getDeclaredField("f_11197_");}
        field.setAccessible(true);
        var players=(Map<UUID,ServerPlayer>)field.get(server.getPlayerList());
        for(boolean ranked:List.of(false,true))for(boolean ownerOpponent:List.of(true,false)){
            var player=new ServerPlayer(server,h.getLevel(),new GameProfile(UUID.randomUUID(),"CreeperOpponent"));
            player.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),player);
            player.setGameMode(GameType.SURVIVAL);player.setNoGravity(true);
            players.put(player.getUUID(),player);h.getLevel().addNewPlayer(player);
            var pet=PrimitiveFuseRegression.create(h,"festive_creeper",false);
            UUID petOwner=ownerOpponent?player.getUUID():UUID.randomUUID();
            ((ModifedToBeTameable)pet).setTameOwnerUUID(petOwner);PrimitiveMobsCompat.setCommand(pet,0);
            var data=new TameData(pet,petOwner,false);data.hungerSaturation=100000;TameRegistry.register(data);
            player.setPos(pet.getX()+2,pet.getY(),pet.getZ());
            try{
                h.assertTrue(!((ModifedToBeTameable)pet).isValidAttackTarget(player),"player protected outside duel");
                TameDuelManager.startTeamDuel(server,petOwner,Set.of(pet.getUUID()),UUID.randomUUID(),Set.of(player.getUUID()),Set.of(),false,ranked);
                h.assertTrue(TameDuelManager.areDuelOpponents(pet.getUUID(),player.getUUID()),"regular/ranked duel registers player opponent");
                h.assertTrue(((ModifedToBeTameable)pet).isValidAttackTarget(player),"Festive creeper must accept opposing player, including its owner; ranked="+ranked+" owner="+ownerOpponent);
                pet.setTarget(player);pet.setNoAi(false);pet.tick();
                h.assertTrue(pet.getTarget()==player,"native AI retains assigned player opponent");
                pet.setNoAi(true);player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);player.setHealth(1000);
                java.lang.reflect.Field spawnProtection;
                try{spawnProtection=ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");}
                catch(NoSuchFieldException ignored){spawnProtection=ServerPlayer.class.getDeclaredField("f_8921_");}
                spawnProtection.setAccessible(true);spawnProtection.setInt(player,0);
                h.assertTrue(!player.isInvulnerableTo(pet.damageSources().mobAttack(pet)),"test survival player has no spawn immunity");
                player.setPos(pet.getX()+2,pet.getY(),pet.getZ());float before=player.getHealth();
                var blasts=new PrimitiveFuseRegression.Blasts(pet);net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(blasts);
                try{
                    for(int i=0;i<40;i++){pet.setTarget(player);player.invulnerableTime=0;pet.tick();}
                    h.assertTrue(blasts.count>0,"native festive fuse explodes against player opponent");
                    h.assertTrue(player.getHealth()<before,"native festive blast damages survival player in regular/ranked duel");
                    h.assertTrue(pet.isAlive(),"pet survives its attack");
                }finally{net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(blasts);}
                TameDuelManager.endDuelsForOwner(server,petOwner);
                h.assertTrue(!((ModifedToBeTameable)pet).isValidAttackTarget(player),"outside-duel player protection resumes");
            }finally{
                TameDuelManager.endDuelsForOwner(server,petOwner);TameRegistry.remove(data.uuid);pet.discard();
                h.getLevel().removePlayerImmediately(player,net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);players.remove(player.getUUID());
                TameRegistry.removePlayerDuelStats(player.getUUID());
            }
        }
        org.apache.logging.log4j.LogManager.getLogger("PrimitiveDuelPlayerRegression").info("PRIMITIVE_DUEL_PLAYER_TESTS_PASS: own-owner and other survival players, regular/ranked target retention and native blast damage, post-duel protection");
    }
}
