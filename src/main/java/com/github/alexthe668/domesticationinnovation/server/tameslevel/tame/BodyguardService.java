package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid = DomesticationMod.MODID)
public final class BodyguardService {
    private BodyguardService() { }
    public static PlayerDuelStats roster(ServerPlayer owner) {
        return TameRegistry.getOrCreatePlayerDuelStats(owner.getUUID(), owner.getGameProfile().getName());
    }
    public static List<TameData> members(PlayerDuelStats roster) {
        List<TameData> result = new ArrayList<>(roster.bodyguards.size());
        for (UUID id : roster.bodyguards) {
            TameData data = TameRegistry.getByTlId(id);
            if (data != null && Objects.equals(data.ownerUUID, roster.playerUuid)) result.add(data);
        }
        return result;
    }
    public static Composition composition(PlayerDuelStats roster) {
        Composition result = new Composition();
        for (TameData data : members(roster)) result.add(data.type);
        return result;
    }
    /** Command-local counts: checking each candidate stays constant time even for large rosters. */
    public static final class Composition {
        private final Map<String, Integer> counts = new HashMap<>();
        private int duplicateTypes;
        public boolean canAdd(String type) {
            if (type == null || type.isBlank()) return false;
            if (counts.isEmpty() || counts.size() == 1 && counts.containsKey(type)) return true;
            return duplicateTypes == 0 && !counts.containsKey(type);
        }
        public void add(String type) {
            int previous = counts.getOrDefault(type, 0);
            if (previous == 1) duplicateTypes++;
            counts.put(type, previous + 1);
        }
    }
    public static double healingBonus(int alive) {
        return alive == 1 ? 2 : alive == 2 ? 1 : alive == 3 ? .5 : 0;
    }
    public static boolean usesOwnerSaturation(TameData data) {
        return data != null && !data.dead && !data.stored
                && (data.rosterBodyguard || TameMode.byId(data.mode) == TameMode.BODYGUARD);
    }
    /** Transfer only enough saturation for this action; one owner saturation is 100 tame units. */
    public static boolean feedFromOwner(TameData data, LivingEntity tame, int required) {
        if (!usesOwnerSaturation(data) || tame == null || tame.getServer() == null) return false;
        ServerPlayer owner = tame.getServer().getPlayerList().getPlayer(data.ownerUUID);
        if (owner == null) return false;
        int missing = Math.max(0, required - data.hungerSaturation);
        float needed = missing / 100.0F;
        float available = owner.getFoodData().getSaturationLevel();
        if (available + 1E-6F < needed) return false;
        owner.getFoodData().setSaturation(Math.max(0, available - needed));
        data.hungerSaturation += missing;
        TameRegistry.markDirty();
        return true;
    }
    public static boolean ownerCanFeed(TameData data, MinecraftServer server) {
        if (!usesOwnerSaturation(data) || server == null) return false;
        ServerPlayer owner = server.getPlayerList().getPlayer(data.ownerUUID);
        return owner != null && owner.getFoodData().getSaturationLevel() >= 0.01F;
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.getServer().getTickCount() % 60 == 0)
            refresh(event.getServer());
    }
    public static void refresh(MinecraftServer server) {
        for (PlayerDuelStats roster : TameRegistry.getPlayerDuelStats().values()) {
            if (roster.bodyguards.isEmpty()) continue;
            List<TameData> guards = members(roster);
            int alive = (int) guards.stream().filter(data -> !data.dead && !data.stored).count();
            ServerPlayer owner = server.getPlayerList().getPlayer(roster.playerUuid);
            for (TameData data : guards) {
                data.rosterBodyguard = true;
                data.bodyguardRange = Math.max(1, Math.min(22, roster.bodyguardRange));
                data.bodyguardHealingBonus = !data.dead && !data.stored ? healingBonus(alive) : 0;
                data.bodyguardXpBonus = owner != null && !data.dead && !data.stored
                        && owner.getHealth() >= owner.getMaxHealth() ? .5 : 0;
                TamePackService.updateModifiers(data);
                if (owner != null && !data.dead && !data.stored && !TameDuelManager.isTameInDuel(data.uuid))
                    TameCommands.maintainRosterBodyguard(owner, data);
            }
        }
    }
}
