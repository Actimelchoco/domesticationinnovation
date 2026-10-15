package dev.ftb.mods.ftbchunks.claimfire;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.logging.LogUtils;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import net.minecraft.commands.Commands;
import net.minecraft.server.rcon.RconConsoleSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.util.DirectoryLock;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.*;

@Mod.EventBusSubscriber(modid = "ftbchunks")
public final class ClaimChunkPruneCommand {
    private static final Logger LOGGER = LogUtils.getLogger();
    private record Job(Path root, List<ClaimChunkPruner.Dimension> dimensions, int distance, Path backup) { }
    private static Job pending;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void register(RegisterCommandsEvent event) {
        var commandRoot = event.getDispatcher().register(Commands.literal("ftbchunks")
                .then(Commands.literal("deleteNoClaimedChunks").requires(source -> source.hasPermission(4))
                        .then(Commands.argument("distance", IntegerArgumentType.integer(0)).executes(ctx -> {
                            var source = ctx.getSource();
                            var server = source.getServer();
                            if (!server.isDedicatedServer() || !(source.source == server || source.source instanceof RconConsoleSource)
                                    || server.getPlayerList().getPlayerCount() != 0) {
                                source.sendFailure(Component.literal("Use the dedicated server console with no players online."));
                                return 0;
                            }
                            var api = FTBChunksAPI.api();
                            if (!api.isManagerLoaded() || pending != null) {
                                source.sendFailure(Component.literal("Claims are unavailable or a deletion is already scheduled."));
                                return 0;
                            }
                            int distance = IntegerArgumentType.getInteger(ctx, "distance");
                            var claims = api.getManager().getAllClaimedChunks();
                            Path root = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
                            List<ClaimChunkPruner.Dimension> dimensions = new ArrayList<>();
                            for (var level : server.getAllLevels()) {
                                var positions = claims.stream().map(claim -> claim.getPos())
                                        .filter(pos -> pos.dimension().equals(level.dimension()))
                                        .map(pos -> new ClaimChunkPruner.Claim(pos.x(), pos.z())).toList();
                                dimensions.add(new ClaimChunkPruner.Dimension(DimensionType.getStorageFolder(level.dimension(), root), positions));
                            }
                            Path backup = root.resolve("chunk-pruning-backups").resolve(UUID.randomUUID().toString());
                            pending = new Job(root, List.copyOf(dimensions), distance, backup);
                            source.sendSuccess(() -> Component.literal("Stopping server to delete chunks outside an inclusive " + distance
                                    + "-chunk square buffer around claims in all dimensions. Dimensions without claims are fully pruned. Backup: "
                                    + backup + ". Restart after completion."), true);
                            server.halt(false);
                            return 1;
                        }))));
        // FTB's /chunks is a Brigadier redirect; attach to its target, not the alias.
        if (event.getDispatcher().getRoot().getChild("chunks") == null) {
            event.getDispatcher().register(Commands.literal("chunks").redirect(commandRoot));
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        Job job = pending;
        pending = null;
        if (job == null) return;
        try (DirectoryLock lock = DirectoryLock.create(job.root)) {
            long deleted = ClaimChunkPruner.prune(job.root, job.dimensions, job.distance, job.backup);
            LOGGER.info("[ChunkPruning] Deleted {} terrain chunks and associated entity/POI data. Backup: {}. Server can now be restarted.", deleted, job.backup);
        } catch (Exception failure) {
            LOGGER.error("[ChunkPruning] Pruning failed. Any modified files were restored where possible; inspect errors and backup before restarting: " + job.backup, failure);
        }
    }
}
