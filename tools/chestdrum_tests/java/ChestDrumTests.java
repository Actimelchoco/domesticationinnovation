package test;

import com.github.alexthe668.domesticationinnovation.server.block.DIBlockRegistry;
import com.github.alexthe668.domesticationinnovation.server.block.DrumBlockEntity;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.LoadedChestDrums;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod("chestdrum_tests")
@Mod.EventBusSubscriber(modid = "chestdrum_tests")
public class ChestDrumTests {
    private static int ready = -1;
    private static int checks;
    private static final BlockPos CHEST = new BlockPos(0, 80, 0);
    private static final List<Wolf> pets = new ArrayList<>();
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        checks++;
    }
    private static Wolf pet(ServerLevel level, int x, int y, int z) {
        Wolf wolf = new Wolf(EntityType.WOLF, level);
        wolf.setTame(true);
        wolf.setOwnerUUID(UUID.randomUUID());
        wolf.setNoAi(true); wolf.setNoGravity(true);
        wolf.moveTo(x + 0.5, y, z + 0.5);
        level.addFreshEntity(wolf);
        TameData data = new TameData(wolf, wolf.getOwnerUUID(), false);
        data.hungerInventory.clear();
        TameRegistry.register(data);
        pets.add(wolf);
        return wolf;
    }
    private static int count(Wolf pet, net.minecraft.world.item.Item item) {
        return TameRegistry.get(pet.getUUID()).hungerInventory.stream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum();
    }
    private static void feed(MinecraftServer server) throws Exception {
        var method = TameCommands.class.getDeclaredMethod("feedLoadedChestDrums", MinecraftServer.class);
        method.setAccessible(true); method.invoke(null, server);
    }
    @SubscribeEvent public static void start(ServerStartedEvent event) {
        ServerLevel level = event.getServer().overworld();
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            level.setChunkForced(x, z, true);
            level.getChunk(x, z);
        }
        level.setBlockAndUpdate(CHEST.below(), DIBlockRegistry.DRUM.get().defaultBlockState());
        level.setBlockAndUpdate(CHEST, Blocks.CHEST.defaultBlockState());
        pet(level, 0, 80, 0);
        pet(level, 25, 85, 25);
        pet(level, -25, 75, -25);
        pet(level, 26, 80, 0);
        pet(level, 0, 86, 0);
        pet(level, 0, 74, 0);
        ready = event.getServer().getTickCount() + 40;
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (AsyncFoodDistributionTests.active()) {
            AsyncFoodDistributionTests.tick(event.getServer());
            return;
        }
        if (ready < 0 || event.getServer().getTickCount() < ready) return;
        ready = -1;
        MinecraftServer server = event.getServer();
        try {
            ServerLevel level = server.overworld();
            if (Boolean.getBoolean("di.guardianDeployTests")) {
                GuardianDeployTests.run(level);
                return;
            }
            check(LoadedChestDrums.chests(level).size() == 1, "unregistered drum discovered automatically");
            ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(CHEST);
            chest.setItem(0, new ItemStack(Items.COOKED_BEEF, 64));
            chest.setItem(1, new ItemStack(Items.COOKED_BEEF, 64));
            chest.setItem(2, new ItemStack(Items.STONE, 64));
            feed(server);
            for (int i = 0; i < 3; i++) check(count(pets.get(i), Items.COOKED_BEEF) == 32, "cross-owner / inclusive boundary " + i);
            for (int i = 3; i < 6; i++) check(count(pets.get(i), Items.COOKED_BEEF) == 0, "outside range " + i);
            check(chest.getItem(2).getCount() == 64, "incompatible food left alone");
            check(chest.getItem(0).getCount() + chest.getItem(1).getCount() == 32, "batch transfers conserve items");
            feed(server);
            check(chest.getItem(0).getCount() + chest.getItem(1).getCount() == 32, "green tames do not consume more");
            // Give the first wolf a full inventory and verify nothing disappears.
            TameData first = TameRegistry.get(pets.get(0).getUUID());
            first.hungerInventory.clear();
            for (int i = 0; i < 18; i++) first.hungerInventory.add(new ItemStack(Items.STONE, 64));
            feed(server);
            check(chest.getItem(0).getCount() + chest.getItem(1).getCount() == 32, "full inventory cannot lose or duplicate chest food");
            first.hungerInventory.clear();
            chest.clearContent();
            chest.setItem(0, new ItemStack(Items.BREAD, 64));
            chest.setItem(1, new ItemStack(Items.COOKED_BEEF, 1));
            feed(server);
            check(count(pets.get(0), Items.COOKED_BEEF) == 1 && count(pets.get(0), Items.BREAD) == 64,
                    "preferred slot consumed before earlier fallback slot, then edible fallback");
            // Shared chests: a gluttonous pig must not take the wolf's preferred beef as fallback.
            first.hungerInventory.clear();
            first.hungerInventory.add(new ItemStack(Items.BREAD, 1));
            chest.clearContent();
            chest.setItem(0, new ItemStack(Items.BREAD, 64));
            BlockPos secondPos = CHEST.offset(-40, 0, 0);
            pets.get(0).moveTo(-19.5, 80, 0.5);
            level.setBlockAndUpdate(secondPos.below(), DIBlockRegistry.DRUM.get().defaultBlockState());
            level.setBlockAndUpdate(secondPos, Blocks.CHEST.defaultBlockState());
            ((DrumBlockEntity) level.getBlockEntity(secondPos.below())).onLoad();
            ((ChestBlockEntity) level.getBlockEntity(secondPos)).setItem(0, new ItemStack(Items.COOKED_BEEF, 1));
            var pig = new net.minecraft.world.entity.animal.Pig(EntityType.PIG, level);
            pig.setNoAi(true); pig.setNoGravity(true); pig.moveTo(-20, 80, 0);
            level.addFreshEntity(pig);
            var pigData = new TameData(pig, UUID.randomUUID(), false);
            pigData.attributeLevels.put("gluttonous", 1);
            TameRegistry.register(pigData);
            feed(server);
            check(count(pets.get(0), Items.COOKED_BEEF) == 1, "preferred pass across overlapping chests precedes fallback");
            check(pigData.hungerInventory.stream().noneMatch(stack -> stack.is(Items.COOKED_BEEF)), "fallback eater cannot steal preferred food");
            check(pigData.hungerInventory.stream().anyMatch(stack -> stack.is(Items.BREAD)), "gluttonous tame gets compatible fallback");
            pig.discard();
            level.removeBlock(secondPos, false); level.removeBlock(secondPos.below(), false);
            pets.get(0).moveTo(0.5, 80, 0.5);
            // A chest position inside another active chest's range must be deactivated.
            BlockPos blockedPos = CHEST.offset(25, 5, 25);
            level.setBlockAndUpdate(blockedPos.below(), DIBlockRegistry.DRUM.get().defaultBlockState());
            level.setBlockAndUpdate(blockedPos, Blocks.CHEST.defaultBlockState());
            ((DrumBlockEntity) level.getBlockEntity(blockedPos.below())).onLoad();
            var blockedChest = (ChestBlockEntity) level.getBlockEntity(blockedPos);
            blockedChest.setItem(0, new ItemStack(Items.COOKED_BEEF, 64));
            var statuses = LoadedChestDrums.statuses(level);
            check(statuses.stream().anyMatch(status -> status.chest().pos().equals(blockedPos)
                    && CHEST.equals(status.blocker())), "inclusive overlap boundary deactivates second drum");
            chest.clearContent(); first.hungerInventory.clear();
            feed(server);
            check(blockedChest.getItem(0).getCount() == 64, "deactivated chest never distributes food");
            level.removeBlock(CHEST, false);
            check(LoadedChestDrums.activeChests(level).stream().anyMatch(c -> c.pos().equals(blockedPos)),
                    "removing blocker reactivates deactivated drum");
            level.setBlockAndUpdate(CHEST, Blocks.CHEST.defaultBlockState());
            chest = (ChestBlockEntity) level.getBlockEntity(CHEST);
            level.removeBlock(blockedPos, false); level.removeBlock(blockedPos.below(), false);
            // Exercise the actual interval gate and hunger-sit recovery together.
            first.hungerInventory.clear();
            first.hungerSaturation = 0; first.hungerForcedSit = true; first.movementOrder = 1;
            first.hasHome = true; first.homeDimension = "minecraft:overworld";
            first.homeX = 12; first.homeY = 80; first.homeZ = 12;
            chest.clearContent(); chest.setItem(0, new ItemStack(Items.COOKED_BEEF, 64));
            var hungerTick = TameCommands.class.getDeclaredMethod("processTameHunger", MinecraftServer.class);
            hungerTick.setAccessible(true);
            server.getWorldData().overworldData().setGameTime(4999); hungerTick.invoke(null, server);
            check(first.hungerInventory.isEmpty(), "no chest feeding between second boundaries");
            server.getWorldData().overworldData().setGameTime(5000); hungerTick.invoke(null, server);
            check(count(pets.get(0), Items.COOKED_BEEF) == 32, "one-second boundary feeds tame to green");
            check(!first.hungerForcedSit && first.movementOrder == 3 && first.homeX == 12, "refill restores guardian anchor");
            var foodTotal = TameCommands.class.getDeclaredMethod("totalHungerFoodPoints", TameData.class);
            foodTotal.setAccessible(true);
            List<Wolf> crowd = new ArrayList<>();
            for (int i = 0; i < 40; i++) crowd.add(pet(level, 2, 80, 2));
            chest.clearContent();
            for (int slot = 0; slot < 27; slot++) chest.setItem(slot, new ItemStack(Items.COOKED_BEEF, 64));
            server.getWorldData().overworldData().setGameTime(5020); hungerTick.invoke(null, server);
            for (Wolf wolf : crowd) {
                check((int) foodTotal.invoke(null, TameRegistry.get(wolf.getUUID())) >= 500,
                        "all 40 tames stay green after feeding and eating in one cycle");
                wolf.discard();
            }
            var foodManager = com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameFoodManager.class;
            foodManager.getMethod("addSuperfood", MinecraftServer.class, String.class, int.class)
                    .invoke(null, server, "minecraft:stick", Integer.MAX_VALUE);
            first.hungerInventory.clear();
            first.hungerInventory.add(new ItemStack(Items.STICK, 64));
            check((int) foodTotal.invoke(null, first) > 1_000_000, "high-value food totals do not overflow");
            var consume = TameCommands.class.getDeclaredMethod("consumeOneHungerFood", TameData.class, net.minecraft.world.entity.LivingEntity.class);
            consume.setAccessible(true);
            first.hungerSaturation = 100;
            check((boolean) consume.invoke(null, first, pets.get(0)) && first.hungerSaturation == Integer.MAX_VALUE,
                    "high-value food consumption does not overflow");
            foodManager.getMethod("removeSuperfood", MinecraftServer.class, String.class).invoke(null, server, "minecraft:stick");
            check(LoadedChestDrums.chests(level, new BlockPos(250, 80, 0), 250).size() == 1, "listing includes 250 boundary");
            check(LoadedChestDrums.chests(level, new BlockPos(251, 80, 0), 250).isEmpty(), "listing excludes farther than 250");
            var command = server.getCommands().getDispatcher().getRoot().getChild("tames").getChild("chestxdrum");
            check(command.getChildren().size() == 2 && command.getChild("system") != null && command.getChild("deactivated") != null, "system and deactivated subcommands");
            DrumBlockEntity drum = (DrumBlockEntity) level.getBlockEntity(CHEST.below());
            drum.onChunkUnloaded();
            check(LoadedChestDrums.chests(level).isEmpty(), "unload removes indexed drum");
            drum.onLoad();
            check(LoadedChestDrums.chests(level).size() == 1, "reload restores index without registration");
            level.removeBlock(CHEST, false);
            check(LoadedChestDrums.chests(level).isEmpty(), "removed container cannot feed");
            level.setBlockAndUpdate(CHEST, Blocks.CHEST.defaultBlockState());
            check(LoadedChestDrums.chests(level).size() == 1, "container placed after drum discovered");
            level.removeBlock(CHEST.below(), false);
            check(LoadedChestDrums.chests(level).isEmpty(), "removed drum cannot feed");
            AsyncFoodDistributionTests.start(server, level);
            System.out.println("CHEST_DRUM_TESTS_PASS checks=" + checks);
        } catch (Throwable failure) {
            System.out.println("CHEST_DRUM_TESTS_FAIL"); failure.printStackTrace();
        } finally { if (!AsyncFoodDistributionTests.active()) server.halt(false); }
    }
}
