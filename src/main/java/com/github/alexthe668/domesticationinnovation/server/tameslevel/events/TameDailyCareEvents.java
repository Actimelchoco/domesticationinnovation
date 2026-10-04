package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.LoadedTameIndex;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DomesticationMod.MODID)
public final class TameDailyCareEvents {
    private static MinecraftServer observedServer;
    private static long lastObservedDay;

    private TameDailyCareEvents() { }

    @SubscribeEvent
    public static void started(ServerStartedEvent event) {
        observedServer = event.getServer();
        lastObservedDay = Math.floorDiv(observedServer.overworld().getDayTime(), 24000L);
    }

    // Evaluate before morning respawns, so a pet revived at dawn cannot earn yesterday's reward.
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer() == null) return;
        MinecraftServer server = event.getServer();
        long day = Math.floorDiv(server.overworld().getDayTime(), 24000L);
        if (observedServer != server) {
            observedServer = server;
            lastObservedDay = day;
            return;
        }
        if (day <= lastObservedDay) return;
        lastObservedDay = day;
        rewardDailyCare(server, day);
    }

    private static void rewardDailyCare(MinecraftServer server, long day) {
        boolean changed = false;
        for (ServerLevel level : server.getAllLevels()) {
            for (var tame : LoadedTameIndex.snapshotForLevel(level)) {
                TameData data = TameRegistry.get(tame.getUUID());
                if (data == null || data.dead || data.stored || data.lastDailyCareDay >= day) continue;
                // New tames must reach a later day before receiving a daily reward.
                if (data.bornDayTime >= 0 && day <= Math.floorDiv(data.bornDayTime, 24000L)) continue;
                data.lastDailyCareDay = day;
                changed = true;
                if (tame.getHealth() < tame.getMaxHealth() || !TameCommands.hasEdibleStoredFood(tame, data)) continue;
                // This fixed care reward is not recovery XP or an armor-mending payment.
                data.xp = (int) Math.min(Integer.MAX_VALUE, (long) Math.max(0, data.xp) + 1L);
                LevelSystem.checkLevelUp(tame, data);
            }
        }
        if (changed) TameRegistry.markDirty();
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        if (observedServer == event.getServer()) observedServer = null;
    }
}
