package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.gametest.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public class DuelSelectionRegression {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static Object field(Object instance, String name) throws Exception {
        Field f = instance.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(instance);
    }
    @SuppressWarnings("unchecked")
    private static Map<UUID, Object> preparations() throws Exception {
        Field f = TameCommands.class.getDeclaredField("SIMPLE_DUEL_PREPARATION_BY_PLAYER"); f.setAccessible(true);
        return (Map<UUID, Object>) f.get(null);
    }
    private static Wolf wolf(GameTestHelper helper, ServerPlayer owner, String name, String group) {
        Wolf wolf = EntityType.WOLF.create(helper.getLevel());
        wolf.setTame(true); wolf.setOwnerUUID(owner.getUUID()); wolf.setPos(owner.getX(), owner.getY(), owner.getZ());
        helper.getLevel().addFreshEntity(wolf);
        TameData data = new TameData(wolf); data.name = name; data.group = group; TameRegistry.register(data); LoadedTameIndex.refresh(wolf);
        return wolf;
    }
    private static Set<String> visibleCommands(com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher,
                                                net.minecraft.commands.CommandSourceStack source) {
        return dispatcher.getRoot().getChild("tames").getChild("duel").getChildren().stream()
                .filter(node->node instanceof com.mojang.brigadier.tree.LiteralCommandNode<?> && node.canUse(source))
                .map(node->node.getName()).collect(Collectors.toSet());
    }

    @GameTest(template="empty")
    @SuppressWarnings("unchecked")
    public static void phaseCommandsNearbyRangesAndBattleInfo(GameTestHelper h)throws Exception{
        List<String> messages=new ArrayList<>();
        var first=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"PhaseA")){
            @Override public void sendSystemMessage(Component message){messages.add(message.getString());}
        };
        var second=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"PhaseB"));
        var pos=h.absolutePos(new net.minecraft.core.BlockPos(2,2,2));
        first.setPos(pos.getX(),pos.getY(),pos.getZ());second.setPos(pos.getX()+2,pos.getY(),pos.getZ());
        Field index=net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");index.setAccessible(true);
        Map<UUID,ServerPlayer> players=(Map<UUID,ServerPlayer>)index.get(h.getLevel().getServer().getPlayerList());
        players.put(first.getUUID(),first);players.put(second.getUUID(),second);
        var dispatcher=h.getLevel().getServer().getCommands().getDispatcher();var source=first.createCommandSourceStack().withPermission(0);
        Class<?> cls=Class.forName(TameCommands.class.getName()+"$SimpleDuelPreparation");
        Constructor<?> ctor=cls.getDeclaredConstructor(UUID.class,UUID.class,long.class);ctor.setAccessible(true);
        Object prep=ctor.newInstance(first.getUUID(),second.getUUID(),System.currentTimeMillis()+300000);
        Set<UUID> ids=(Set<UUID>)field(field(prep,"firstTeam"),"tameIds");
        List<net.minecraft.world.entity.LivingEntity> pets=new ArrayList<>();
        try{
            check(visibleCommands(dispatcher,source).equals(Set.of("invite","selection","arena","leaderboard")),"idle command set");
            preparations().put(first.getUUID(),prep);preparations().put(second.getUUID(),prep);
            check(visibleCommands(dispatcher,source).equals(Set.of("selection","info","start","cancel","add","addMyself","addNearby","remove")),"selection phase exposes only selection and start/cancel commands");
            var near=wolf(h,first,"Near","Ranges");near.setPos(first.getX()+9,first.getY(),first.getZ());
            var diagonal=wolf(h,first,"Diagonal","Ranges");diagonal.setPos(first.getX()+9,first.getY(),first.getZ()+9);
            var edge=wolf(h,first,"Edge","Ranges");edge.setPos(first.getX()+64,first.getY(),first.getZ());
            var outside=wolf(h,first,"OutsideRange","Ranges");outside.setPos(first.getX()+64.25,first.getY(),first.getZ());
            var close=wolf(h,first,"Close","Ranges");close.setPos(first.getX()+1,first.getY(),first.getZ());
            var fox=EntityType.FOX.create(h.getLevel());var modified=(com.github.alexthe668.domesticationinnovation.server.entity.ModifedToBeTameable)fox;
            modified.setTame(true);modified.setTameOwnerUUID(first.getUUID());fox.setPos(first.getX()+2,first.getY(),first.getZ());h.getLevel().addFreshEntity(fox);
            var foxData=new TameData(fox,first.getUUID(),true);foxData.name="InterfaceFox";TameRegistry.register(foxData);LoadedTameIndex.refresh(fox);
            pets.addAll(List.of(near,diagonal,edge,outside,close,fox));
            check(dispatcher.execute("tames duel addNearby",source)==1,"default addNearby succeeds");
            check(ids.equals(Set.of(near.getUUID(),close.getUUID(),fox.getUUID())),"default radius is 10, spherical, and includes interface tames");
            dispatcher.execute("tames duel remove all",source);
            check(dispatcher.execute("tames duel addNearby 1",source)==1&&ids.equals(Set.of(close.getUUID())),"minimum radius 1");
            dispatcher.execute("tames duel remove all",source);
            check(dispatcher.execute("tames duel addNearby 64",source)==1&&!ids.contains(outside.getUUID())&&ids.contains(edge.getUUID())&&ids.contains(diagonal.getUUID()),"maximum radius 64 includes boundary and excludes beyond");
            for(int radius:List.of(0,65)){
                Set<UUID> before=Set.copyOf(ids);boolean rejected=false;
                try{rejected=dispatcher.execute("tames duel addNearby "+radius,source)==0;}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){rejected=true;}
                check(rejected&&ids.equals(before),"out-of-range radius rejected without changing roster");
            }
            check(dispatcher.execute("tames duel info",source)==1,"info works in selection");
            dispatcher.execute("tames duel addMyself",source);
            dispatcher.execute("tames duel start",source);dispatcher.execute("tames duel start",second.createCommandSourceStack().withPermission(0));
            check(visibleCommands(dispatcher,source).equals(Set.of("ff","info")),"battle exposes exactly ff and info");
            TameDuelManager.endDuelForEntity(h.getLevel().getServer(),near.getUUID());
            check(near.isAlive(),"eliminated body is still alive, so status must come from duel membership");
            var nearData=TameRegistry.get(near.getUUID());
            TameRegistry.rebindEntityUuid(nearData,UUID.randomUUID());
            messages.clear();
            try{check(dispatcher.execute("tames duel info",source)==1,"battle info succeeds after a recovery changes entity identity");}
            finally{TameRegistry.rebindEntityUuid(nearData,near.getUUID());}
            check(messages.size()==2&&messages.stream().anyMatch(line->line.contains("Near (dead)"))&&messages.stream().anyMatch(line->line.contains("player (alive)")),"battle info preserves original selection and marks alive/dead");
            boolean blocked=false;try{dispatcher.execute("tames duel addNearby",source);}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){blocked=true;}
            check(blocked,"selection changes cannot execute during battle");
            check(dispatcher.execute("tames duel ff",source)==1,"forfeit remains available");
            check(visibleCommands(dispatcher,source).equals(Set.of("invite","selection","arena","leaderboard")),"idle commands return after battle");
            h.succeed();
        }finally{
            TameDuelManager.endDuelsForOwner(h.getLevel().getServer(),first.getUUID());preparations().remove(first.getUUID());preparations().remove(second.getUUID());
            players.remove(first.getUUID());players.remove(second.getUUID());
            for(var pet:pets){var loaded=h.getLevel().getEntity(pet.getUUID());if(loaded!=null)loaded.discard();TameRegistry.remove(pet.getUUID());pet.discard();}
        }
    }
    @GameTest(template="empty")
    @SuppressWarnings("unchecked")
    public static void editableRosterAndExplicitReady(GameTestHelper helper) throws Exception {
        List<String> firstMessages=new ArrayList<>(),secondMessages=new ArrayList<>();
        var first = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "DuelDraftA")) {
            @Override public void sendSystemMessage(Component message){firstMessages.add(message.getString());}
        };
        var second = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "DuelDraftB")) {
            @Override public void sendSystemMessage(Component message){secondMessages.add(message.getString());}
        };
        var pos = helper.absolutePos(new net.minecraft.core.BlockPos(2, 2, 2));
        first.setPos(pos.getX(), pos.getY(), pos.getZ()); second.setPos(pos.getX()+2, pos.getY(), pos.getZ());
        Field index = net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID"); index.setAccessible(true);
        Map<UUID,ServerPlayer> players = (Map<UUID,ServerPlayer>)index.get(helper.getLevel().getServer().getPlayerList());
        players.put(first.getUUID(),first); players.put(second.getUUID(),second);
        Class<?> cls = Class.forName(TameCommands.class.getName()+"$SimpleDuelPreparation");
        Constructor<?> ctor = cls.getDeclaredConstructor(UUID.class,UUID.class,long.class); ctor.setAccessible(true);
        Object prep = ctor.newInstance(first.getUUID(),second.getUUID(),System.currentTimeMillis()+300000);
        preparations().put(first.getUUID(),prep); preparations().put(second.getUUID(),prep);
        Object draft = field(prep,"firstTeam");
        Object otherDraft = field(prep,"secondTeam");
        Set<UUID> ids = (Set<UUID>)field(draft,"tameIds");
        List<Wolf> wolves = new ArrayList<>();
        var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
        var source = first.createCommandSourceStack().withPermission(0);
        try {
            Wolf alpha=wolf(helper,first,"Alpha","Pack"), beta=wolf(helper,first,"Beta","Pack"), outsider=wolf(helper,first,"Outside","Other");
            wolves.addAll(List.of(alpha,beta,outsider));
            firstMessages.clear();secondMessages.clear();
            check(dispatcher.execute("tames duel selection",source)==1,"empty rosters can be viewed");
            check(firstMessages.equals(List.of("DuelDraftA: player","DuelDraftB: player")),"empty rosters display both players without instructions: "+firstMessages);
            check(dispatcher.execute("tames duel start",source)==1 && (boolean)field(draft,"ready") && (boolean)field(draft,"includeSelf"),"empty start acts as addMyself and readies");
            check(dispatcher.execute("tames duel remove myself",source)==1,"clear explicit self before picking tames");
            firstMessages.clear();secondMessages.clear();
            check(dispatcher.execute("tames duel add group Pack",source)==1 && ids.size()==2,"group add snapshots participants");
            check(firstMessages.size()==2 && firstMessages.get(0).startsWith("DuelDraftA: ")
                    && firstMessages.get(0).contains("Alpha") && firstMessages.get(0).contains("Beta")
                    && firstMessages.get(1).equals("DuelDraftB: player") && firstMessages.equals(secondMessages),
                    "edit sends only both current rosters, once, to both players");
            check(dispatcher.execute("tames duel add name Alpha",source)==1 && ids.size()==2,"repeated add deduplicates");
            var suggestions=dispatcher.getCompletionSuggestions(dispatcher.parse("tames duel remove ",source)).join().getList().stream().map(s->s.getText()).collect(Collectors.toSet());
            check(suggestions.containsAll(Set.of("Alpha","Beta","type","group"))&&!suggestions.contains("Outside"),"remove names must be selected only");
            suggestions=dispatcher.getCompletionSuggestions(dispatcher.parse("tames duel remove group ",source)).join().getList().stream().map(s->s.getText()).collect(Collectors.toSet());
            check(suggestions.equals(Set.of("Pack")),"only selected groups suggested");
            check(dispatcher.execute("tames duel remove name Alpha",source)==1 && ids.equals(Set.of(beta.getUUID())),"remove one member from group snapshot");
            TameRegistry.get(beta.getUUID()).name="Renamed Beta";
            check(dispatcher.execute("tames duel selection",source)==1,"selection view supports renamed tame");
            check(dispatcher.execute("tames duel start",source)==1 && (boolean)field(draft,"ready"),"first player explicitly readies");
            check(!TameDuelManager.isOwnerInDuel(first.getUUID()),"first confirmation must wait for opponent");
            var otherSource=second.createCommandSourceStack().withPermission(0);
            check(dispatcher.execute("tames duel addMyself",otherSource)==1 && (boolean)field(draft,"ready"),"opponent edit preserves first player's confirmation");
            check(dispatcher.execute("tames duel addMyself",source)==1 && (boolean)field(draft,"includeSelf") && !(boolean)field(draft,"ready"),"edit clears confirmation");
            check(dispatcher.execute("tames duel start",otherSource)==1 && (boolean)field(otherDraft,"ready"),"opponent readies while editor remains unready");
            check(dispatcher.execute("tames duel remove type minecraft:wolf",source)==1 && ids.isEmpty(),"type removes selected tames only");
            check(dispatcher.execute("tames duel remove myself",source)==1 && !(boolean)field(draft,"includeSelf"),"self can be removed");
            check(dispatcher.execute("tames duel add group Pack",source)==1,"re-add group");
            check(dispatcher.execute("tames duel remove group Pack",source)==1 && ids.isEmpty(),"group remove");
            check((boolean)field(otherDraft,"ready"),"multiple own edits preserve opponent confirmation");
            check(firstMessages.get(firstMessages.size()-2).equals("DuelDraftA: player"),"removing last tame displays the player");
            check(dispatcher.execute("tames duel addMyself",source)==1,"self-only team allowed");
            check(!TameDuelManager.isOwnerInDuel(first.getUUID()),"editing both teams never auto starts");
            check(dispatcher.execute("tames duel start",source)==1,"confirm first");
            check(TameDuelManager.isOwnerInDuel(first.getUUID()) && TameDuelManager.isOwnerInDuel(second.getUUID()),"editor's reconfirmation starts duel without opponent pressing start again");
            helper.succeed();
        } finally {
            TameDuelManager.endDuelsForOwner(helper.getLevel().getServer(),first.getUUID());
            preparations().remove(first.getUUID()); preparations().remove(second.getUUID());
            players.remove(first.getUUID()); players.remove(second.getUUID());
            for(Wolf wolf:wolves) { TameRegistry.remove(wolf.getUUID()); wolf.discard(); }
        }
    }
}
