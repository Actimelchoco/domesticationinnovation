package test;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.common.util.FakePlayerFactory;
import java.lang.reflect.*;
import java.util.*;

final class AsyncFoodDistributionTests {
    private static final BlockPos POS = new BlockPos(2, 80, 2);
    private static final List<TameData> data = new ArrayList<>();
    private static final List<Wolf> pets = new ArrayList<>();
    private static ServerLevel level;
    private static ServerPlayer player;
    private static ChestBlockEntity chest;
    private static Method distribute;
    private static Constructor<?> wrapper;
    private static Field jobs;
    private static int stage = -1;
    private static int deadline;
    private static int checks;
    private static int[] previous;
    private static long snapshotNanos;
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        checks++;
    }
    static boolean active() { return stage >= 0; }
    private static boolean pending() throws Exception { return !((List<?>) jobs.get(null)).isEmpty(); }
    private static int food(TameData target) {
        return target.hungerInventory.stream().filter(s -> s.is(Items.COOKED_BEEF)).mapToInt(ItemStack::getCount).sum();
    }
    private static int total() { return data.stream().mapToInt(AsyncFoodDistributionTests::food).sum(); }
    private static void submit() throws Exception {
        deadline = level.getServer().getTickCount() + 400;
        check((int) distribute.invoke(null, player, data, wrapper.newInstance(chest), false) == 1, "distribution queued");
        previous = data.stream().mapToInt(AsyncFoodDistributionTests::food).toArray();
    }
    static void start(MinecraftServer server, ServerLevel serverLevel) throws Exception {
        level = serverLevel;
        player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "AsyncFoodTest"));
        level.setBlockAndUpdate(POS, Blocks.CHEST.defaultBlockState());
        chest = (ChestBlockEntity) level.getBlockEntity(POS);
        for (int i = 0; i < 40; i++) {
            Wolf wolf = new Wolf(EntityType.WOLF, level);
            wolf.setTame(true); wolf.setOwnerUUID(player.getUUID()); wolf.setNoAi(true); wolf.setNoGravity(true);
            wolf.moveTo((i % 8) * 2 + 0.5, 80, (i / 8) * 2 + 0.5); level.addFreshEntity(wolf);
            var target = new TameData(wolf, player.getUUID(), false);
            target.hungerSaturation = 1000; target.movementOrder = 1;
            TameRegistry.register(target); data.add(target); pets.add(wolf);
        }
        Class<?> access = Class.forName(TameCommands.class.getName() + "$InventoryAccess");
        wrapper = Class.forName(TameCommands.class.getName() + "$ContainerInventoryAccess").getDeclaredConstructor(Container.class);
        wrapper.setAccessible(true);
        distribute = TameCommands.class.getDeclaredMethod("distributeHungerFoodFromInventory", ServerPlayer.class, List.class, access, boolean.class);
        distribute.setAccessible(true);
        jobs = TameCommands.class.getDeclaredField("FOOD_DISTRIBUTIONS"); jobs.setAccessible(true);
        for (int i = 0; i < 27; i++) chest.setItem(i, new ItemStack(Items.COOKED_BEEF, 64));
        long started = System.nanoTime(); submit(); snapshotNanos = System.nanoTime() - started;
        check(total() == 0 && chest.getItem(0).getCount() == 64, "snapshot removes no food");
        check((int) distribute.invoke(null, player, data, wrapper.newInstance(chest), false) == 0, "duplicate job blocked");
        stage = 0;
    }
    static void tick(MinecraftServer server) {
        try {
            check(server.getTickCount() <= deadline, "worker and transfers finish without blocking tick thread");
            int changed = 0;
            for (int i = 0; i < data.size(); i++) {
                int now = food(data.get(i));
                if (now != previous[i]) changed++;
                previous[i] = now;
            }
            check(changed <= 1, "at most one tame receives food per tick");
            if (pending()) return;
            if (stage == 0) {
                int min = data.stream().mapToInt(AsyncFoodDistributionTests::food).min().orElseThrow();
                int max = data.stream().mapToInt(AsyncFoodDistributionTests::food).max().orElseThrow();
                check(total() == 1728 && chest.isEmpty() && max - min <= 1, "1728 items balanced across 40 tames without loss");
                data.forEach(target -> target.hungerInventory.clear());
                for (int i = 0; i < 27; i++) chest.setItem(i, new ItemStack(Items.COOKED_BEEF, 64));
                submit();
                // Mutations occur after snapshot, before any subsequent main-thread transfer.
                data.get(0).dead = true;
                for (int i = 0; i < 18; i++) data.get(1).hungerInventory.add(new ItemStack(Items.STONE, 64));
                data.get(2).ownerUUID = UUID.randomUUID();
                pets.get(3).remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
                chest.clearContent(); chest.setItem(0, new ItemStack(Items.COOKED_BEEF, 7));
                chest.setItem(1, new ItemStack(Items.STONE, 64));
                stage = 1;
            } else if (stage == 1) {
                check(total() == 7 && chest.getItem(0).isEmpty(), "remaining food still distributed when snapshot supply shrinks");
                check(chest.getItem(1).is(Items.STONE) && chest.getItem(1).getCount() == 64, "changed item not extracted");
                for (int i = 0; i < 4; i++) check(food(data.get(i)) == 0, "dead/full/transferred/unloaded tame skipped " + i);
                data.get(0).dead = false; data.get(2).ownerUUID = player.getUUID();
                data.forEach(target -> target.hungerInventory.clear());
                chest.clearContent(); chest.setItem(0, new ItemStack(Items.COOKED_BEEF, 64));
                submit();
                level.removeBlock(POS, false);
                level.setBlockAndUpdate(POS, Blocks.CHEST.defaultBlockState());
                chest = (ChestBlockEntity) level.getBlockEntity(POS);
                chest.setItem(0, new ItemStack(Items.COOKED_BEEF, 64));
                stage = 2;
            } else if (stage == 2) {
                check(total() == 0 && chest.getItem(0).getCount() == 64, "replacement chest not touched by old plan");
                submit();
                var stop = TameCommands.class.getDeclaredMethod("stopFoodDistributions"); stop.setAccessible(true); stop.invoke(null);
                check(!pending() && total() == 0 && chest.getItem(0).getCount() == 64, "shutdown cancels pending plan without moving food");
                submit(); // A new server lifecycle can create a new worker.
                stage = 3;
            } else {
                check(total() == 64 && chest.isEmpty(), "distribution works after executor restart");
                System.out.println("ASYNC_FOOD_DISTRIBUTION_PASS checks=" + checks + " snapshot_ms=" + snapshotNanos / 1_000_000.0);
                stage = -1;
                server.halt(false);
            }
        } catch (Throwable failure) {
            System.out.println("ASYNC_FOOD_DISTRIBUTION_FAIL"); failure.printStackTrace(); stage = -1; server.halt(false);
        }
    }
}
