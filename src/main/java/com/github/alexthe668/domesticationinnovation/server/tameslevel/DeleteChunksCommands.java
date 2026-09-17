package com.github.alexthe668.domesticationinnovation.server.tameslevel;

import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.alexthe668.domesticationinnovation.server.tameslevel.ChunkDeletionSelection.*;

@Mod.EventBusSubscriber(modid = DomesticationMod.MODID)
public final class DeleteChunksCommands {
    private static final Map<UUID, Point> POINTS_A = new HashMap<>();
    private static final Map<UUID, Point> POINTS_B = new HashMap<>();
    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    private static final Map<UUID, Scheduled> SCHEDULED = new HashMap<>();

    private record Scheduled(Area area, int requestedTick) { }

    private DeleteChunksCommands() { }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("deleteChunks")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("setA").executes(ctx -> setPoint(ctx.getSource(), true)))
                .then(Commands.literal("setB").executes(ctx -> setPoint(ctx.getSource(), false)))
                .then(Commands.literal("settings").executes(ctx -> settings(ctx.getSource())))
                .then(Commands.literal("delete").executes(ctx -> request(ctx.getSource())))
                .then(Commands.literal("confirm").executes(ctx -> confirm(ctx.getSource()))));
    }

    private static Point point(ServerPlayer player) {
        return new Point(player.blockPosition().getX(), player.blockPosition().getZ(),
                player.level().dimension().location().toString());
    }

    private static void tell(ServerPlayer player, String message, ChatFormatting color) {
        player.sendSystemMessage(Component.literal("[DeleteChunks] " + message).withStyle(color));
    }

    private static int setPoint(CommandSourceStack source, boolean a) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        UUID id = player.getUUID();
        Point point = point(player);
        (a ? POINTS_A : POINTS_B).put(id, point);
        PENDING.remove(id);
        SCHEDULED.remove(id);
        showPoint(player, a ? "A" : "B", point);
        return 1;
    }

    private static void showPoint(ServerPlayer player, String label, Point point) {
        if (point == null) {
            tell(player, label + ": Not set", ChatFormatting.GRAY);
            return;
        }
        tell(player, label + ": Chunk " + point.chunkX() + ", " + point.chunkZ(), ChatFormatting.GREEN);
        tell(player, "Block " + point.blockX() + ", " + point.blockZ() + "; Dimension: " + point.dimension(), ChatFormatting.GRAY);
    }

    private static int settings(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        UUID id = player.getUUID();
        Point a = POINTS_A.get(id);
        Point b = POINTS_B.get(id);
        tell(player, "===== Settings =====", ChatFormatting.GOLD);
        tell(player, "Validation only: actual chunk deletion is disabled.", ChatFormatting.YELLOW);
        showPoint(player, "A", a);
        showPoint(player, "B", b);
        if (a != null && b != null) {
            if (!a.dimension().equals(b.dimension())) {
                tell(player, "A and B are in different dimensions!", ChatFormatting.RED);
            } else {
                Area area = new Area(a, b);
                tell(player, "Area: " + area.width() + " x " + area.height() + " chunks; total: " + area.count(), ChatFormatting.YELLOW);
                tell(player, area.confirmationsRequired() + " confirmation(s) required.", ChatFormatting.YELLOW);
            }
        }
        Pending pending = PENDING.get(id);
        long now = System.nanoTime();
        if (pending != null && pending.expired(now)) {
            PENDING.remove(id);
            tell(player, "Confirmation expired.", ChatFormatting.RED);
        } else if (pending != null) {
            tell(player, "Pending confirmation: " + pending.secondsLeft(now) + "s remaining; "
                    + pending.confirmations() + "/" + pending.area().confirmationsRequired(), ChatFormatting.GOLD);
        }
        return 1;
    }

    private static int request(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        UUID id = player.getUUID();
        Point a = POINTS_A.get(id);
        Point b = POINTS_B.get(id);
        if (a == null || b == null) {
            tell(player, "Both A and B must be set first.", ChatFormatting.RED);
            return 0;
        }
        if (!a.dimension().equals(b.dimension())) {
            tell(player, "A and B are in different dimensions.", ChatFormatting.RED);
            return 0;
        }
        Area area = new Area(a, b);
        SCHEDULED.remove(id);
        PENDING.put(id, Pending.start(area, System.nanoTime()));
        tell(player, "DELETION REQUESTED: " + area.count() + " chunks in " + area.dimension()
                + "; X " + area.minX() + ".." + area.maxX() + ", Z " + area.minZ() + ".." + area.maxZ(), ChatFormatting.RED);
        tell(player, area.confirmationsRequired() == 2
                ? "This exceeds 100 chunks. TWO confirmations are required."
                : "One confirmation is required.", ChatFormatting.YELLOW);
        tell(player, "Validation only: actual chunk deletion is disabled.", ChatFormatting.YELLOW);
        tell(player, "Run /deleteChunks confirm within 60 seconds.", ChatFormatting.GOLD);
        return 1;
    }

    private static int confirm(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        UUID id = player.getUUID();
        Pending pending = PENDING.get(id);
        if (pending == null) {
            tell(player, "There is no pending deletion.", ChatFormatting.RED);
            return 0;
        }
        long now = System.nanoTime();
        if (pending.expired(now)) {
            PENDING.remove(id);
            tell(player, "Confirmation expired.", ChatFormatting.RED);
            return 0;
        }
        pending = pending.confirm(now);
        if (!pending.complete()) {
            PENDING.put(id, pending);
            tell(player, "FIRST CONFIRMATION ACCEPTED. " + pending.area().count() + " chunks queued."
                    + " Run /deleteChunks confirm AGAIN within 60 seconds.", ChatFormatting.RED);
            return 1;
        }
        PENDING.remove(id);
        if (blocked(source.getServer(), player, pending.area())) return 0;
        SCHEDULED.put(id, new Scheduled(pending.area(), source.getServer().getTickCount()));
        tell(player, "Safety check passed. Preparing " + pending.area().count() + " chunks...", ChatFormatting.YELLOW);
        return 1;
    }

    private static boolean blocked(MinecraftServer server, ServerPlayer requester, Area area) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers().stream()
                .filter(player -> area.contains(point(player))).toList();
        if (players.isEmpty()) return false;
        tell(requester, "DELETION CANCELLED. Players are inside the selected chunks:", ChatFormatting.RED);
        for (ServerPlayer player : players) {
            Point point = point(player);
            tell(requester, player.getGameProfile().getName() + " - chunk " + point.chunkX() + ", " + point.chunkZ(), ChatFormatting.RED);
        }
        tell(requester, "Move all players outside the area and run /deleteChunks delete again.", ChatFormatting.YELLOW);
        return true;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || SCHEDULED.isEmpty()) return;
        MinecraftServer server = event.getServer();
        for (UUID id : new ArrayList<>(SCHEDULED.keySet())) {
            Scheduled scheduled = SCHEDULED.get(id);
            if (server.getTickCount() == scheduled.requestedTick()) continue;
            SCHEDULED.remove(id);
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null) continue;
            if (!player.createCommandSourceStack().hasPermission(2)) {
                tell(player, "DELETION CANCELLED. Permission level 2 is required.", ChatFormatting.RED);
                continue;
            }
            if (!blocked(server, player, scheduled.area())) actuallyDeleteChunks(server, player, scheduled.area());
        }
    }

    private static void actuallyDeleteChunks(MinecraftServer server, ServerPlayer player, Area area) {
        // Deliberately isolated: deleting region entries while chunks or storage caches are live
        // can restore deleted data or corrupt storage. No unverified live deletion is attempted.
        tell(player, "Actual chunk deletion is not implemented. No chunks were deleted.", ChatFormatting.RED);
        tell(player, "All command and safety checks succeeded.", ChatFormatting.YELLOW);
        DomesticationMod.LOGGER.warn("[DeleteChunks] Would delete {} chunks from {}: X {}..{}, Z {}..{}",
                area.count(), area.dimension(), area.minX(), area.maxX(), area.minZ(), area.maxZ());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        POINTS_A.clear();
        POINTS_B.clear();
        PENDING.clear();
        SCHEDULED.clear();
    }
}
