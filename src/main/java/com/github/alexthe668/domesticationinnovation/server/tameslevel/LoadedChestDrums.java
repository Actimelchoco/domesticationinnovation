package com.github.alexthe668.domesticationinnovation.server.tameslevel;

import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.github.alexthe668.domesticationinnovation.server.block.DrumBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
    public static final int RANGE_PER_DRUM = 10;
    public static final int HEIGHT_PER_DRUM = 2;
    private static final int MAX_RANGE = RANGE + 4 * RANGE_PER_DRUM;
    private static final Map<ServerLevel, Map<BlockPos, DrumBlockEntity>> LOADED = new IdentityHashMap<>();


    public record Status(Chest chest, BlockPos blocker) {
        public boolean active() { return blocker == null; }
    }

    public record Chest(BlockEntity blockEntity, IItemHandler inventory, UUID owner, int extraDrums, BlockPos drumPos) {
        public Chest(BlockEntity blockEntity, IItemHandler inventory, UUID owner, int extraDrums) {
            this(blockEntity,inventory,owner,extraDrums,blockEntity.getBlockPos().below());
        }
        public BlockPos pos() { return blockEntity.getBlockPos(); }
        public int range() { return RANGE + extraDrums * RANGE_PER_DRUM; }
        public int height() { return HEIGHT + extraDrums * HEIGHT_PER_DRUM; }
        public boolean inRange(BlockPos target) { return LoadedChestDrums.inRange(pos(), target, range(), height()); }
    }

    private LoadedChestDrums() { }

    public static void add(DrumBlockEntity drum) {
        if (drum.getLevel() instanceof ServerLevel level) {
            LOADED.computeIfAbsent(level, ignored -> new HashMap<>()).put(drum.getBlockPos(), drum);
        }
    }

    public static void remove(DrumBlockEntity drum) {
        if (drum.getLevel() instanceof ServerLevel level) {
            GuardianChestAutomation.removeDrum(level, drum.getBlockPos());
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
            Chest chest=chestAtDrum(level,drum.getBlockPos());
            if(chest!=null&&(center==null||center.distSqr(chest.pos())<=(double)radius*radius))result.add(chest);
        }
        result.sort(java.util.Comparator.comparingLong(chest -> chest.pos().asLong()));
        return result;
    }

    /** Prefer the inventory above a drum, so the center foundation chest is never a second controller. */
    public static Chest chestAtDrum(ServerLevel level,BlockPos drumPos) {
        Map<BlockPos,DrumBlockEntity> drums=LOADED.get(level);
        var chunk=level.getChunkSource().getChunkNow(drumPos.getX()>>4,drumPos.getZ()>>4);
        if(drums==null||chunk==null)return null;
        DrumBlockEntity drum=drums.get(drumPos);
        if(drum==null||drum.isRemoved()||chunk.getBlockEntity(drumPos)!=drum)return null;
        for(Direction side:List.of(Direction.UP,Direction.DOWN)) {
            BlockPos pos=drumPos.relative(side);
            BlockEntity container=chunk.getBlockEntity(pos);
            if(container==null||container.isRemoved())continue;
            if(side==Direction.DOWN&&chunk.getBlockEntity(pos.below()) instanceof DrumBlockEntity lower
                    &&!lower.isRemoved()&&drums.get(lower.getBlockPos())==lower)continue;
            IItemHandler inventory=inventory(container,side.getOpposite());
            if(inventory!=null)return new Chest(container,inventory,drum.getPlacerUUID(),adjacentDrums(level,drums,drumPos),drumPos);
        }
        return null;
    }

    /** Covers Forge inventories, sided-only machines, and ordinary Container
     * implementations without requiring chest tags or a particular mod class. */
    public static IItemHandler inventory(BlockEntity container, Direction attachedFace) {
        IItemHandler found=container.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
        if(found!=null&&found.getSlots()>0)return found;
        if(attachedFace!=null){
            found=container.getCapability(ForgeCapabilities.ITEM_HANDLER,attachedFace).orElse(null);
            if(found!=null&&found.getSlots()>0)return found;
        }
        for(Direction side:Direction.values()){
            found=container.getCapability(ForgeCapabilities.ITEM_HANDLER,side).orElse(null);
            if(found!=null&&found.getSlots()>0)return found;
        }
        if(container instanceof net.minecraft.world.Container vanilla&&vanilla.getContainerSize()>0)
            return new net.minecraftforge.items.wrapper.InvWrapper(vanilla);
        return null;
    }

    private static int adjacentDrums(ServerLevel level, Map<BlockPos, DrumBlockEntity> drums, BlockPos center) {
        int count = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos pos = center.relative(direction);
            DrumBlockEntity drum = drums.get(pos);
            if (drum == null || drum.isRemoved()) continue;
            var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            if (chunk != null && chunk.getBlockEntity(pos) == drum) count++;
        }
        return count;
    }

    /** Fixed position priority prevents two drums from disabling each other, including after reload. */
    public static List<Status> statuses(ServerLevel level) {
        List<Status> result = new ArrayList<>();
        Map<Long, List<Chest>> activeByChunk = new HashMap<>();
        for (Chest chest : chests(level)) {
            BlockPos pos = chest.pos();
            BlockPos blocker = null;
            search:
            for (int x = (pos.getX() - MAX_RANGE) >> 4; x <= (pos.getX() + MAX_RANGE) >> 4; x++) {
                for (int z = (pos.getZ() - MAX_RANGE) >> 4; z <= (pos.getZ() + MAX_RANGE) >> 4; z++) {
                    for (Chest active : activeByChunk.getOrDefault(ChunkPos.asLong(x, z), List.of())) {
                        if (active.inRange(pos)) {
                            blocker = active.pos();
                            break search;
                        }
                    }
                }
            }
            result.add(new Status(chest, blocker));
            if (blocker == null) activeByChunk.computeIfAbsent(
                    ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4), ignored -> new ArrayList<>()).add(chest);
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
            GuardianChestAutomation.refresh(level, statuses(level));
        }
    }

    public static boolean inRange(BlockPos chest, BlockPos tame) {
        return inRange(chest, tame, RANGE, HEIGHT);
    }

    private static boolean inRange(BlockPos chest, BlockPos tame, int range, int height) {
        return Math.abs((long) chest.getX() - tame.getX()) <= range
                && Math.abs((long) chest.getZ() - tame.getZ()) <= range
                && Math.abs((long) chest.getY() - tame.getY()) <= height;
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            LOADED.remove(level);
        }
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) {
        LOADED.clear();
    }
}
