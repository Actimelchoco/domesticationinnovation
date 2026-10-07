package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.Map;
import java.util.UUID;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public class GuardianRestRegression {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    @SuppressWarnings("unchecked")
    private static Map<UUID, ServerPlayer> online(GameTestHelper helper) throws Exception {
        var field = net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");
        field.setAccessible(true);
        return (Map<UUID, ServerPlayer>) field.get(helper.getLevel().getServer().getPlayerList());
    }
    private static Wolf fixture(GameTestHelper helper, UUID owner) {
        Wolf wolf = EntityType.WOLF.create(helper.getLevel());
        wolf.setTame(true); wolf.setOwnerUUID(owner);
        BlockPos post = helper.absolutePos(new BlockPos(2, 2, 2));
        wolf.moveTo(post.getX() + .5, post.getY(), post.getZ() + .5);
        helper.getLevel().addFreshEntity(wolf);
        TameData data = new TameData(wolf);
        data.name = "Rest Guardian"; data.movementOrder = 3; data.hasHome = true;
        data.homeDimension = helper.getLevel().dimension().location().toString();
        data.homeX = post.getX(); data.homeY = post.getY(); data.homeZ = post.getZ();
        data.guardianRestHome.putString("dimension", data.homeDimension);
        data.guardianRestHome.putDouble("x", post.getX() + 20.5);
        data.guardianRestHome.putDouble("y", post.getY());
        data.guardianRestHome.putDouble("z", post.getZ() + .5);
        TameRegistry.register(data); LoadedTameIndex.refresh(wolf);
        return wolf;
    }
    private static boolean atRestHome(Wolf wolf, TameData data) {
        return wolf.distanceToSqr(data.guardianRestHome.getDouble("x"), data.guardianRestHome.getDouble("y"), data.guardianRestHome.getDouble("z")) < 1;
    }

    @GameTest(template = "empty")
    public static void logoutRespawnLoginAndSavedPost(GameTestHelper helper) throws Exception {
        UUID ownerId = UUID.randomUUID(); Wolf wolf = fixture(helper, ownerId);
        TameData data = TameRegistry.get(wolf.getUUID());
        var players = online(helper);
        try {
            check(TameCommands.processGuardianRest(wolf, data) && atRestHome(wolf, data) && wolf.isOrderedToSit(),
                    "ordinary guardian must teleport home and sit with offline owner");
            check(data.hasHome && data.movementOrder == 3 && data.guardianResting, "guard post and intent must survive rest");
            var saved = TameData.fromTag(data.toTag());
            check(saved.guardianResting && saved.homeX == data.homeX && saved.guardianRestHome.equals(data.guardianRestHome),
                    "rest and original post must survive saving");
            wolf.teleportTo(data.homeX + .5, data.homeY, data.homeZ + .5);
            var respawn = TameCommands.class.getDeclaredMethod("applyBedRespawnMovement", net.minecraft.server.MinecraftServer.class, TameData.class);
            respawn.setAccessible(true); respawn.invoke(null, helper.getLevel().getServer(), data);
            check(atRestHome(wolf, data), "respawned guardian must remain home while owner is offline");
            var owner = FakePlayerFactory.get(helper.getLevel(), new GameProfile(ownerId, "rest_owner"));
            players.put(ownerId, owner);
            TameCommands.processGuardianRest(wolf, data);
            check(!data.guardianResting && !wolf.isOrderedToSit() && wolf.distanceToSqr(data.homeX + .5, data.homeY, data.homeZ + .5) < 1,
                    "owner reconnect must resume guarding at the original post");
        } finally { players.remove(ownerId); TameRegistry.remove(wolf.getUUID()); wolf.discard(); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hungryWalkDeadlineFoodAndCancellation(GameTestHelper helper) throws Exception {
        UUID ownerId = UUID.randomUUID(); Wolf wolf = fixture(helper, ownerId);
        TameData data = TameRegistry.get(wolf.getUUID());
        var players = online(helper);
        // Explicit offline deployments stay active without an online owner; hunger still suspends them.
        data.offlineGuardianDeployed = true;
        try {
            data.hungerSaturation = 0; data.hungerInventory.clear();
            var forced = helper.getLevel().getDataStorage().computeIfAbsent(
                    net.minecraft.world.level.ForcedChunksSavedData::load, net.minecraft.world.level.ForcedChunksSavedData::new, "chunks");
            var baseline = new java.util.HashSet<>(forced.getEntityForcedChunks().getTickingChunks().keySet());
            TameCommands.processGuardianRest(wolf, data);
            long deadline = data.guardianHungerHomeDeadline;
            check(data.guardianResting && !atRestHome(wolf, data) && !wolf.isOrderedToSit()
                    && deadline == helper.getLevel().getServer().overworld().getGameTime() + 1200L,
                    "hungry guardian must walk instead of immediately teleporting/sitting");
            check(forced.getEntityForcedChunks().getTickingChunks().keySet().size() > baseline.size(),
                    "homeward walk must receive a temporary ticking ticket");
            TameCommands.processGuardianRest(wolf, data);
            check(data.guardianHungerHomeDeadline == deadline && !atRestHome(wolf, data), "rechecks must not restart the minute");
            data.guardianHungerHomeDeadline = helper.getLevel().getServer().overworld().getGameTime();
            TameCommands.processGuardianRest(wolf, data);
            check(atRestHome(wolf, data) && wolf.isOrderedToSit(), "expired minute must teleport a blocked guardian home");
            check(forced.getEntityForcedChunks().getTickingChunks().keySet().equals(baseline),
                    "home arrival must release temporary walking tickets");
            data.hungerInventory.add(new ItemStack(Items.COOKED_BEEF));
            TameCommands.processGuardianRest(wolf, data);
            check(!data.guardianResting && !wolf.isOrderedToSit(), "stored food must resume deployed offline guardian duty");
            data.offlineGuardianDeployed = false;
            TameCommands.processGuardianRest(wolf, data);
            data.hungerSaturation = 0; data.hungerInventory.clear();
            var owner = FakePlayerFactory.get(helper.getLevel(), new GameProfile(ownerId, "rest_hungry"));
            players.put(ownerId, owner);
            TameCommands.processGuardianRest(wolf, data);
            check(data.guardianResting && wolf.isOrderedToSit(), "login must not resume guarding while still hungry");
            data.hungerSaturation = 100;
            TameCommands.processGuardianRest(wolf, data);
            check(!data.guardianResting, "restored saturation alone must resume guarding");
            players.remove(ownerId);
            TameCommands.processGuardianRest(wolf, data);
            var movement = TameCommands.class.getDeclaredMethod("applyLivingMovementOverride", net.minecraft.world.entity.LivingEntity.class,
                    TameData.class, Class.forName(TameCommands.class.getName() + "$MovementOrder"));
            movement.setAccessible(true);
            @SuppressWarnings({"rawtypes", "unchecked"})
            Object follow = Enum.valueOf((Class) movement.getParameterTypes()[2], "FOLLOW");
            movement.invoke(null, wolf, data, follow);
            check(!data.guardianResting && !data.hasHome, "manual movement change must cancel suspended guardian duty");
        } finally { players.remove(ownerId); TameRegistry.remove(wolf.getUUID()); wolf.discard(); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void equipmentNamesFilterAnimights(GameTestHelper helper) throws Exception {
        UUID ownerId = UUID.randomUUID(); Wolf wolf = fixture(helper, ownerId);
        TameData wolfData = TameRegistry.get(wolf.getUUID());
        var owner = FakePlayerFactory.get(helper.getLevel(), new GameProfile(ownerId, "equipment_names"));
        var animight = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(new net.minecraft.resources.ResourceLocation("animights", "canito"))
                .create(helper.getLevel());
        var pet = (net.minecraft.world.entity.TamableAnimal) animight;
        pet.setTame(true); pet.setOwnerUUID(ownerId);
        TameData data = new TameData(pet); data.name = "Gear Pet"; TameRegistry.register(data);
        try {
            var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
            var suggestions = dispatcher.getCompletionSuggestions(dispatcher.parse("tames inventory equipment ", owner.createCommandSourceStack())).join();
            String names = suggestions.getList().toString();
            check(names.contains("Gear Pet") && !names.contains(wolfData.name), "equipment completions must include Animights and exclude wolves");
            check(dispatcher.execute("tames inventory equipment " + wolfData.name, owner.createCommandSourceStack()) == 0,
                    "typed non-Animight name must be rejected too");
        } finally { TameRegistry.remove(wolf.getUUID()); TameRegistry.remove(pet.getUUID()); wolf.discard(); pet.discard(); }
        helper.succeed();
    }
}
