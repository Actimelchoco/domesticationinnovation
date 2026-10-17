package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraftforge.common.util.FakePlayerFactory;
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
    @GameTest(template="empty")
    @SuppressWarnings("unchecked")
    public static void editableRosterAndExplicitReady(GameTestHelper helper) throws Exception {
        var first = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "DuelDraftA"));
        var second = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "DuelDraftB"));
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
        Set<UUID> ids = (Set<UUID>)field(draft,"tameIds");
        List<Wolf> wolves = new ArrayList<>();
        var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
        var source = first.createCommandSourceStack().withPermission(0);
        try {
            Wolf alpha=wolf(helper,first,"Alpha","Pack"), beta=wolf(helper,first,"Beta","Pack"), outsider=wolf(helper,first,"Outside","Other");
            wolves.addAll(List.of(alpha,beta,outsider));
            check(dispatcher.execute("tames duel start",source)==0,"empty teams must not ready");
            check(dispatcher.execute("tames duel add group Pack",source)==1 && ids.size()==2,"group add snapshots participants");
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
            check(dispatcher.execute("tames duel addMyself",source)==1 && (boolean)field(draft,"includeSelf") && !(boolean)field(draft,"ready"),"edit clears confirmation");
            check(dispatcher.execute("tames duel remove type minecraft:wolf",source)==1 && ids.isEmpty(),"type removes selected tames only");
            check(dispatcher.execute("tames duel remove myself",source)==1 && !(boolean)field(draft,"includeSelf"),"self can be removed");
            check(dispatcher.execute("tames duel add group Pack",source)==1,"re-add group");
            check(dispatcher.execute("tames duel remove group Pack",source)==1 && ids.isEmpty(),"group remove");
            check(dispatcher.execute("tames duel addMyself",source)==1,"self-only team allowed");
            var otherSource=second.createCommandSourceStack().withPermission(0);
            check(dispatcher.execute("tames duel addMyself",otherSource)==1,"opponent builds team");
            check(!TameDuelManager.isOwnerInDuel(first.getUUID()),"editing both teams never auto starts");
            check(dispatcher.execute("tames duel start",source)==1,"confirm first");
            check(dispatcher.execute("tames duel start",otherSource)==1,"confirm second");
            check(TameDuelManager.isOwnerInDuel(first.getUUID()) && TameDuelManager.isOwnerInDuel(second.getUUID()),"both confirmations start duel");
            helper.succeed();
        } finally {
            TameDuelManager.endDuelsForOwner(helper.getLevel().getServer(),first.getUUID());
            preparations().remove(first.getUUID()); preparations().remove(second.getUUID());
            players.remove(first.getUUID()); players.remove(second.getUUID());
            for(Wolf wolf:wolves) { TameRegistry.remove(wolf.getUUID()); wolf.discard(); }
        }
    }
}
