package com.github.alexthe668.domesticationinnovation.server.tameslevel;

import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.github.alexthe668.domesticationinnovation.server.block.DrumBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.TickEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server-thread index maintained by drum block-entity load/unload callbacks. */
@Mod.EventBusSubscriber(modid = DomesticationMod.MODID)
public final class LoadedChestDrums {
    public static final int RANGE = 25;
    public static final int HEIGHT = 5;
    private static final Map<ServerLevel, Map<BlockPos, DrumBlockEntity>> LOADED = new IdentityHashMap<>();

    private static final Map<ServerLevel, Map<BlockPos, BlockPos>> LAST_BLOCKERS = new IdentityHashMap<>();

    public record Status(Chest chest, BlockPos blocker) {
        public boolean active() { return blocker == null; }
    }

    public record Chest(BlockEntity blockEntity, IItemHandler inventory, UUID owner) {
        public BlockPos pos() { return blockEntity.getBlockPos(); }
    }

    private LoadedChestDrums() { }

    public static void add(DrumBlockEntity drum) {
        if (drum.getLevel() instanceof ServerLevel level) {
            LOADED.computeIfAbsent(level, ignored -> new HashMap<>()).put(drum.getBlockPos(), drum);
        }
    }

    public static void remove(DrumBlockEntity drum) {
        if (drum.getLevel() instanceof ServerLevel level) {
            Map<BlockPos, DrumBlockEntity> drums = LOADED.get(level);
            if (drums != null) {
                drums.remove(drum.getBlockPos(), drum);
                if (drums.isEmpty()) LOADED.remove(level);
            }
        }
    }

    public static List<Chest> chests(ServerLevel level) {
        return chests(level, null, 0);
    }

    public static List<Chest> chests(ServerLevel level, BlockPos center, int radius) {
        List<Chest> result = new ArrayList<>();
        Map<BlockPos, DrumBlockEntity> drums = LOADED.get(level);
        if (drums == null) return result;
        for (DrumBlockEntity drum : drums.values()) {
            BlockPos pos = drum.getBlockPos().above();
            if (center != null && center.distSqr(pos) > (double) radius * radius) continue;
            if (drum.isRemoved()) continue;
            var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            if (chunk == null || chunk.getBlockEntity(drum.getBlockPos()) != drum) continue;
            BlockEntity container = chunk.getBlockEntity(pos);
            if (container == null || container.isRemoved()) continue;
            IItemHandler inventory = container.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
            if (inventory != null) result.add(new Chest(container, inventory, drum.getPlacerUUID()));
        }
        result.sort(java.util.Comparator.comparingLong(chest -> chest.pos().asLong()));
        return result;
    }

    /** Fixed position priority prevents two drums from disabling each other, including after reload. */
    public static List<Status> statuses(ServerLevel level) {
        List<Status> result = new ArrayList<>();
        Map<Long, List<BlockPos>> activeByChunk = new HashMap<>();
        for (Chest chest : chests(level)) {
            BlockPos pos = chest.pos();
            BlockPos blocker = null;
            search:
            for (int x = (pos.getX() - RANGE) >> 4; x <= (pos.getX() + RANGE) >> 4; x++) {
                for (int z = (pos.getZ() - RANGE) >> 4; z <= (pos.getZ() + RANGE) >> 4; z++) {
                    for (BlockPos active : activeByChunk.getOrDefault(ChunkPos.asLong(x, z), List.of())) {
                        if (inRange(active, pos)) {
                            blocker = active;
                            break search;
                        }
                    }
                }
            }
            result.add(new Status(chest, blocker));
            if (blocker == null) activeByChunk.computeIfAbsent(
                    ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4), ignored -> new ArrayList<>()).add(pos);
        }
        return result;
    }

    public static List<Chest> activeChests(ServerLevel level) {
        return statuses(level).stream().filter(Status::active).map(Status::chest).toList();
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 20 != 0) return;
        for (ServerLevel level : event.getServer().getAllLevels()) {
            Map<BlockPos, BlockPos> previous = LAST_BLOCKERS.getOrDefault(level, Map.of());
            Map<BlockPos, BlockPos> current = new HashMap<>();
            for (Status status : statuses(level)) {
                if (status.active()) continue;
                BlockPos pos = status.chest().pos();
                current.put(pos, status.blocker());
                if (status.blocker().equals(previous.get(pos))) continue;
                Component message = Component.literal("Chest x drum at " + pos.toShortString()
                        + " is deactivated because it is in range of the chest x drum at "
                        + status.blocker().toShortString() + ".").withStyle(ChatFormatting.YELLOW);
                for (var player : level.players()) {
                    if (player.getUUID().equals(status.chest().owner()) || player.blockPosition().distSqr(pos) <= 250.0 * 250.0) {
                        player.sendSystemMessage(message);
                    }
                }
            }
            LAST_BLOCKERS.put(level, current);
        }
    }

    public static boolean inRange(BlockPos chest, BlockPos tame) {
        return Math.abs((long) chest.getX() - tame.getX()) <= RANGE
                && Math.abs((long) chest.getZ() - tame.getZ()) <= RANGE
                && Math.abs((long) chest.getY() - tame.getY()) <= HEIGHT;
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            LOADED.remove(level);
            LAST_BLOCKERS.remove(level);
        }
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) {
        LOADED.clear();
        LAST_BLOCKERS.clear();
    }
}
