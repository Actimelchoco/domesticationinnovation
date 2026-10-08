package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Pack checks run every 60 ticks. Combat and hunger handlers read precomputed multipliers. */
@Mod.EventBusSubscriber(modid = DomesticationMod.MODID)
public final class TamePackService {
    public static final int REFRESH_INTERVAL_TICKS = 60;
    public record Member(UUID id, String name, String type, int level) { }
    public record Bonuses(List<Member> members, double healingSpeed, double healingAmount,
                          double cooldownReduction, double damageBonus, double xpBonus, double defense) {
        public static final Bonuses NONE = new Bonuses(List.of(), 0, 1, 0, 0, 0, 0);
        public static final Bonuses LONELY = new Bonuses(List.of(), 2, 1, 0, .5, 0, 0);
        public int categories() {
            return (healingSpeed > 0 ? 1 : 0) + (cooldownReduction > 0 ? 1 : 0)
                    + (damageBonus > 0 ? 1 : 0) + (xpBonus > 0 ? 1 : 0) + (defense > 0 ? 1 : 0);
        }
    }
    public record Modifiers(double healingIntervalMultiplier, double healingAmount, double cooldownMultiplier,
                            double damageMultiplier, double incomingDamageMultiplier,
                            double xpBonus, double saturationMultiplier) {
        public static final Modifiers NONE = new Modifiers(1, 1, 1, 1, 1, 0, 1);
    }
    private static final Map<UUID, Bonuses> BONUSES = new HashMap<>();
    private static final Set<TameData> ACTIVE = new HashSet<>();
    private static final Map<UUID, Long> LAST_COMBAT = new HashMap<>();
    private static MinecraftServer observedServer;
    private static long lastDay;
    private static long nextRefreshTick;
    private TamePackService() { }

    public static Bonuses bonuses(TameData data) {
        return data == null || data.dead || data.stored ? Bonuses.NONE : BONUSES.getOrDefault(data.uuid, Bonuses.NONE);
    }
    public static Modifiers modifiers(TameData data) {
        if (data == null) return Modifiers.NONE;
        return data.dead || data.stored ? data.packRestModifiers : data.packModifiers;
    }
    /** Called only when pack membership, daily XP, or persisted stats change. */
    public static void updateModifiers(TameData data) {
        double daily = Math.max(0, Math.min(1, data.dailyPackXpBonus));
        double guardXp = Math.max(0, data.bodyguardXpBonus);
        double guardHeal = Math.max(0, data.bodyguardHealingBonus);
        int restCategories = (daily > 0 ? 1 : 0) + (guardXp > 0 ? 1 : 0) + (guardHeal > 0 ? 1 : 0);
        double restXp = daily + guardXp;
        data.packRestModifiers = new Modifiers(1 / (1 + guardHeal), 1, 1, 1, 1,
                restXp, 1 + .1 * restCategories + restXp);
        Bonuses pack = bonuses(data);
        data.packModifiers = pack == Bonuses.NONE ? data.packRestModifiers
                : new Modifiers(1 / (1 + pack.healingSpeed() + guardHeal), pack.healingAmount(), 1 - pack.cooldownReduction(),
                        1 + pack.damageBonus(), 1 - pack.defense(), restXp + pack.xpBonus(),
                        1 + .1 * (pack.categories() + restCategories) + restXp + pack.xpBonus());
    }
    public static double cooldownReduction(int packmates) {
        return .4 * (1 - Math.pow(.75, Math.max(0, packmates)));
    }
    public static Bonuses calculate(Member self, List<Member> nearby) {
        List<Member> others = nearby.stream().filter(m -> !m.id().equals(self.id())).toList();
        if (others.isEmpty()) return Bonuses.LONELY;
        int same = 0, different = 0, sameMod = 0, foreignMod = 0;
        int min = self.level(), max = self.level();
        String mod = namespace(self.type());
        for (Member member : others) {
            boolean sameType = self.type().equals(member.type());
            if (sameType) same++; else different++;
            if (mod.equals(namespace(member.type()))) { if (!sameType) sameMod++; }
            else foreignMod++;
            min = Math.min(min, member.level()); max = Math.max(max, member.level());
        }
        double sizeBonus = others.size() == 1 ? 1 : others.size() == 2 ? .5 : 0;
        double speed = sizeBonus + Math.max(0, .2 * same - .3 * different);
        // The twelfth member of one species is the first to reduce healing amount.
        double amount = Math.max(0, 1 - .2 * Math.max(0, same + 1 - 11));
        boolean levelsMatch = max - min <= 10;
        return new Bonuses(List.copyOf(others), speed, amount, cooldownReduction(different),
                levelsMatch ? .1 * others.size() : 0,
                levelsMatch && self.level() == min ? .1 * others.size() : 0,
                Math.min(.3, Math.max(0, .05 * sameMod - .3 * foreignMod)));
    }
    private static String namespace(String type) {
        int separator = type.indexOf(':');
        return separator < 0 ? type : type.substring(0, separator);
    }
    public static double xpBonus(TameData data) {
        return modifiers(data).xpBonus();
    }
    public static double saturationMultiplier(TameData data) {
        return modifiers(data).saturationMultiplier();
    }
    public static int bonusXp(TameData data, int base) {
        if (data == null || base <= 0) return base;
        double extra = base * xpBonus(data) + data.packXpRemainder;
        long whole = (long) Math.floor(extra + 1E-9);
        data.packXpRemainder = Math.max(0, extra - whole);
        return (int) Math.min(Integer.MAX_VALUE, (long) base + whole);
    }
    public static int saturationCost(TameData data, double base) {
        if (base <= 0) return 0;
        double cost = base * saturationMultiplier(data) + (data == null ? 0 : data.packSaturationRemainder);
        long whole = (long) Math.floor(cost + 1E-9);
        if (data != null) data.packSaturationRemainder = Math.max(0, cost - whole);
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1, whole));
    }
    public static void recordDeath(TameData data, long day) {
        if (data.packDeathDay != day) { data.packDeathDay = day; data.packDeathsToday = 0; }
        if (data.packDeathsToday < Integer.MAX_VALUE) data.packDeathsToday++;
        if (data.packDeathsToday > 1 && data.hasSavedProgress) {
            data.savedLevel = Math.max(data.level, data.savedLevel - 1);
            data.savedXpToNext = com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem.xpRequiredForLevel(data.savedLevel);
            data.savedXp = Math.min(data.savedXp, Math.max(0, data.savedXpToNext - 1));
            data.savedProgressCost = com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem.estimateSavedProgress(data);
        }
        data.dailyPackXpBonus = 0;
        data.packAliveDays = 0;
        data.packLastMorning = Math.max(data.packLastMorning, day);
        BONUSES.remove(data.uuid); LAST_COMBAT.remove(data.uuid);
        ACTIVE.remove(data);
        updateModifiers(data);
    }
    public static boolean morning(TameData data, long day, boolean online) {
        if (data.dead || data.stored || data.packLastMorning >= day
                || data.bornDayTime < 0 || day <= Math.floorDiv(data.bornDayTime, 24000L)) return false;
        data.packLastMorning = day;
        if (data.packAliveDays < Integer.MAX_VALUE) data.packAliveDays++;
        data.dailyPackXpBonus = Math.min(1, data.dailyPackXpBonus + (online ? .1 : .05));
        updateModifiers(data);
        return true;
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        if (server != observedServer) {
            clearRuntime();
            observedServer = server;
            lastDay = Math.floorDiv(server.overworld().getDayTime(), 24000L);
            nextRefreshTick = server.getTickCount();
        }
        refreshIfDue(server, server.getTickCount());
    }
    private static void refreshIfDue(MinecraftServer server, long tick) {
        if (tick < nextRefreshTick) return;
        nextRefreshTick = tick + REFRESH_INTERVAL_TICKS;
        long day = Math.floorDiv(server.overworld().getDayTime(), 24000L);
        if (day > lastDay) {
            lastDay = day;
            boolean changed = false;
            for (TameData data : TameRegistry.TAMES.values()) {
                changed |= morning(data, day, server.getPlayerList().getPlayer(data.ownerUUID) != null);
            }
            if (changed) TameRegistry.markDirty();
        }
        refresh(server);
    }
    public static void refresh(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        Map<UUID, List<LivingEntity>> groups = new HashMap<>();
        Map<UUID, Member> members = new HashMap<>();
        Set<UUID> seen = new HashSet<>();
        for (var level : server.getAllLevels()) {
            for (LivingEntity tame : LoadedTameIndex.snapshotForLevel(level)) {
                TameData data = TameRegistry.get(tame.getUUID());
                if (data == null || data.dead || data.stored || !tame.isAlive() || data.ownerUUID == null) continue;
                ServerPlayer owner = server.getPlayerList().getPlayer(data.ownerUUID);
                if (owner == null || owner.level() != tame.level() || owner.distanceToSqr(tame) > 67 * 67
                        || TameDuelManager.isTameInDuel(tame.getUUID())) continue;
                if (tame instanceof Mob mob && mob.getTarget() != null && mob.getTarget().isAlive()
                        && !data.ownerUUID.equals(mob.getTarget().getUUID())
                        && !data.ownerUUID.equals(TameEntityAdapter.ownerUuid(mob.getTarget()))) {
                    LAST_COMBAT.put(data.uuid, now);
                }
                Long last = LAST_COMBAT.get(data.uuid);
                if (last == null || now < last || now - last > 100) continue;
                if (!seen.add(data.uuid)) continue;
                groups.computeIfAbsent(data.ownerUUID, ignored -> new ArrayList<>()).add(tame);
                members.put(data.uuid, new Member(data.uuid, data.name,
                        BuiltInRegistries.ENTITY_TYPE.getKey(tame.getType()).toString(), data.level));
            }
        }
        BONUSES.clear();
        LAST_COMBAT.keySet().retainAll(seen);
        Set<TameData> updated = new HashSet<>();
        for (List<LivingEntity> group : groups.values()) {
            for (LivingEntity tame : group) {
                List<Member> nearby = new ArrayList<>();
                for (LivingEntity other : group) {
                    if (other != tame && other.level() == tame.level() && tame.distanceToSqr(other) <= 32 * 32)
                        nearby.add(members.get(other.getUUID()));
                }
                BONUSES.put(tame.getUUID(), calculate(members.get(tame.getUUID()), nearby));
                TameData data = TameRegistry.get(tame.getUUID());
                updateModifiers(data);
                updated.add(data);
            }
        }
        for (TameData data : ACTIVE) {
            if (!updated.contains(data)) data.packModifiers = data.packRestModifiers;
        }
        ACTIVE.clear();
        ACTIVE.addAll(updated);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide || event.getAmount() <= 0) return;
        TameData attacker = event.getSource().getEntity() == null ? null
                : TameRegistry.get(event.getSource().getEntity().getUUID());
        TameData victim = TameRegistry.get(event.getEntity().getUUID());
        event.setAmount((float) (event.getAmount() * modifiers(attacker).damageMultiplier()
                * modifiers(victim).incomingDamageMultiplier()));
    }
    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        clearRuntime(); observedServer = null;
    }
    private static void clearRuntime() {
        BONUSES.clear(); LAST_COMBAT.clear();
        for (TameData data : ACTIVE) data.packModifiers = data.packRestModifiers;
        ACTIVE.clear();
    }
}
