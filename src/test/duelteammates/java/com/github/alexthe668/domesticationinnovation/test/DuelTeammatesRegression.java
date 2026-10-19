package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import java.lang.reflect.*;
import java.util.*;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public class DuelTeammatesRegression {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static Object field(Object value,String name)throws Exception{var f=value.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(value);}
    @SuppressWarnings("unchecked") static Map<UUID,Object> store(String name)throws Exception{
        var f=TameCommands.class.getDeclaredField(name);f.setAccessible(true);return (Map<UUID,Object>)f.get(null);
    }
    static final class Fixture implements AutoCloseable {
        final GameTestHelper h;final ServerPlayer a,b,c,d;final List<ServerPlayer> players, online;final Map<UUID,ServerPlayer> index;
        final com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher;
        final List<LivingEntity> pets=new ArrayList<>();
        @SuppressWarnings("unchecked") Fixture(GameTestHelper helper)throws Exception{
            h=helper;var server=h.getLevel().getServer();dispatcher=server.getCommands().getDispatcher();
            String suffix=UUID.randomUUID().toString().substring(0,5);
            a=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"Captain"+suffix));
            b=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"Opponent"+suffix));
            c=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"Mate"+suffix));
            d=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"Other"+suffix));players=List.of(a,b,c,d);
            var f=net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");f.setAccessible(true);
            index=(Map<UUID,ServerPlayer>)f.get(server.getPlayerList());
            var pos=h.absolutePos(new net.minecraft.core.BlockPos(2,2,2));
            for(var player:players){index.put(player.getUUID(),player);player.setPos(pos.getX(),pos.getY(),pos.getZ());}
            var list=net.minecraft.server.players.PlayerList.class.getDeclaredField("players");list.setAccessible(true);
            online=(List<ServerPlayer>)list.get(server.getPlayerList());online.addAll(players);
        }
        int command(ServerPlayer player,String command)throws Exception{return dispatcher.execute("tames duel "+command,player.createCommandSourceStack().withPermission(0));}
        boolean rejected(ServerPlayer player,String command)throws Exception{try{return command(player,command)==0;}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){return true;}}
        UUID invite(ServerPlayer from,ServerPlayer to)throws Exception{
            check(command(from,"invite "+to.getGameProfile().getName())==1,"invite command succeeds");
            for(var entry:store("SIMPLE_DUEL_INVITES").entrySet())
                if(to.getUUID().equals(field(entry.getValue(),"opponent")))return entry.getKey();
            throw new AssertionError("invite was not stored");
        }
        void prepare()throws Exception{UUID id=invite(a,b);check(command(b,"accept "+id)==1,"opponent accepts challenge");}
        void join()throws Exception{UUID id=invite(a,c);check(command(c,"accept "+id)==1,"teammate accepts invitation");}
        LivingEntity pet(ServerPlayer owner,boolean fox,String name){
            LivingEntity body=fox?EntityType.FOX.create(h.getLevel()):EntityType.WOLF.create(h.getLevel());
            TameEntityAdapter.setOwner(body,owner.getUUID());body.setPos(owner.getX()+2,owner.getY(),owner.getZ());h.getLevel().addFreshEntity(body);
            var data=new TameData(body,owner.getUUID(),fox);data.name=name;TameRegistry.register(data);LoadedTameIndex.refresh(body);pets.add(body);return body;
        }
        public void close()throws Exception{
            for(var player:players){TameDuelManager.endDuelsForOwner(h.getLevel().getServer(),player.getUUID());store("SIMPLE_DUEL_PREPARATION_BY_PLAYER").remove(player.getUUID());index.remove(player.getUUID());}
            var invites=store("SIMPLE_DUEL_INVITES");
            for(var entry:new ArrayList<>(invites.entrySet())) {
                UUID challenger=(UUID)field(entry.getValue(),"challenger");
                if(players.stream().anyMatch(p->p.getUUID().equals(challenger)))invites.remove(entry.getKey());
            }
            online.removeAll(players);
            for(var pet:pets){TameRegistry.remove(pet.getUUID());pet.discard();}
        }
    }
    @GameTest(template="empty")
    public static void acceptedTeammateJoinsSameSideAndEveryoneMustReady(GameTestHelper h)throws Exception{
        try(var f=new Fixture(h)){
            f.prepare();UUID id=f.invite(f.a,f.c);
            check(!store("SIMPLE_DUEL_PREPARATION_BY_PLAYER").containsKey(f.c.getUUID()),"invites require acceptance");
            check(f.rejected(f.d,"accept "+id),"another player cannot accept someone else's invite");
            check(f.command(f.c,"accept "+id)==1,"intended player accepts");
            check(f.rejected(f.a,"invite "+f.c.getGameProfile().getName()),"already selecting teammate cannot join twice");
            check(f.command(f.a,"start")==1&&f.command(f.b,"start")==1,"original players ready");
            check(!TameDuelManager.isOwnerInDuel(f.a.getUUID()),"match waits for invited player");
            check(f.command(f.c,"start")==1,"teammate's empty selection defaults to self");
            for(var player:List.of(f.a,f.b,f.c))check(TameDuelManager.isOwnerInDuel(player.getUUID()),"all owners recognized in match");
            check(TameDuelManager.areDuelOpponents(f.c.getUUID(),f.b.getUUID()),"teammate opposes the other side");
            check(!TameDuelManager.areDuelOpponents(f.a.getUUID(),f.c.getUUID()),"teammates are not opponents");
            var teams=TameDuelManager.duelTeamStatus(h.getLevel().getServer(),f.c.getUUID());
            check(teams.size()==2&&teams.stream().anyMatch(t->t.members().size()==2&&t.label().contains(f.c.getGameProfile().getName())),"2v1 status includes teammate");
            check(f.command(f.c,"info")==1&&f.rejected(f.c,"invite "+f.d.getGameProfile().getName()),"battle commands available and team invites hidden");
            check(f.command(f.c,"ff")==1,"teammate can forfeit");
            for(var player:List.of(f.a,f.b,f.c))check(!TameDuelManager.isOwnerInDuel(player.getUUID()),"forfeit cleans every member");
        }h.succeed();
    }
    @GameTest(template="empty")
    public static void teammatesManageOnlyOwnTamesAndKeepIndependentReadiness(GameTestHelper h)throws Exception{
        try(var f=new Fixture(h)){
            f.prepare();f.join();var wolf=f.pet(f.a,false,"CaptainWolf");var fox=f.pet(f.c,true,"MateFox");
            f.command(f.a,"add CaptainWolf");f.command(f.c,"add MateFox");
            check(f.rejected(f.a,"add MateFox"),"captain cannot select teammate-owned tames");
            f.command(f.a,"start");f.command(f.b,"start");
            Object prep=store("SIMPLE_DUEL_PREPARATION_BY_PLAYER").get(f.a.getUUID());
            check((boolean)field(field(prep,"firstTeam"),"ready"),"captain is ready");
            f.command(f.c,"addMyself");
            check((boolean)field(field(prep,"firstTeam"),"ready"),"teammate edit does not reset captain readiness");
            f.command(f.c,"remove myself");f.command(f.c,"start");
            check(TameDuelManager.isOwnerInDuel(f.c.getUUID())&&!TameDuelManager.isEntityInDuel(f.c.getUUID()),"tame-only teammate remains a match owner");
            check(TameDuelManager.areDuelOwnersOpponents(f.c.getUUID(),f.b.getUUID())&&!TameDuelManager.areDuelOwnersOpponents(f.c.getUUID(),f.a.getUUID()),"owner alliance checks honor both sides");
            check(TameDuelManager.areDuelOpponents(fox.getUUID(),f.b.getUUID())&&!TameDuelManager.areDuelOpponents(fox.getUUID(),wolf.getUUID()),"mixed native/interface pets share correct side");
            check(f.command(f.c,"info")==1&&f.command(f.c,"ff")==1,"tame-only teammate can inspect and end match");
            check(f.c.getUUID().equals(TameEntityAdapter.ownerUuid(fox))&&f.a.getUUID().equals(TameEntityAdapter.ownerUuid(wolf)),"cleanup preserves actual pet ownership");
        }h.succeed();
    }
    @GameTest(template="empty")
    public static void cancelClearsAllMembersAndInvalidatesPendingTeamInvites(GameTestHelper h)throws Exception{
        try(var f=new Fixture(h)){
            f.prepare();f.join();UUID pending=f.invite(f.c,f.d);
            check(f.command(f.b,"cancel")==1,"opponent can cancel selection");
            for(var player:List.of(f.a,f.b,f.c))check(!store("SIMPLE_DUEL_PREPARATION_BY_PLAYER").containsKey(player.getUUID()),"cancel removes all drafts");
            check(f.rejected(f.d,"accept "+pending),"stale invitation cannot resurrect cancelled match");
            f.prepare();check(f.rejected(f.d,"accept "+pending),"stale invitation cannot join a replacement selection");
        }h.succeed();
    }
}
