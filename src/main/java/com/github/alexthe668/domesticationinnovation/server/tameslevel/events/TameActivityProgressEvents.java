package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid = DomesticationMod.MODID)
public final class TameActivityProgressEvents {
    public static final int PARTICIPATION_TICKS = 5 * 60 * 20;
    public static final int FOLLOW_TICKS = 10 * 60 * 20;
    public static final int IDLE_TICKS = 60 * 20;
    public static final int GUARD_TICKS = 24000;
    private static final String GUARD_PROGRESS = "TLGuardActivityTicks";
    private static final String GUARD_PLACE = "TLGuardActivityPlace";
    private record Participation(UUID tameId, UUID tlId, long expires) {}
    private static final Map<UUID, Map<UUID, Participation>> PARTICIPANTS = new HashMap<>();
    private static final Map<LivingEntity, FollowProgress> FOLLOW = new WeakHashMap<>();
    private static long clock;
    private static final class FollowProgress {
        UUID owner;
        Vec3 position;
        int idle;
        int ticks;
    }

    private static TameData data(LivingEntity tame) {
        TameData data = TameRegistry.get(tame.getUUID());
        return data != null ? data : TameRegistry.getByTlId(TameData.getTlId(tame));
    }

    private static boolean eligible(LivingEntity tame, TameData data) {
        return data != null && !data.dead && !data.stored && tame.isAlive()
                && TameEntityAdapter.isTame(tame) && !TameDuelManager.isEntityInDuel(tame.getUUID())
                && !com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.BewereagerCompat.isTemporary(tame);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void damaged(LivingDamageEvent event) {
        LivingEntity tame = event.getEntity();
        if (tame.level().isClientSide || event.isCanceled() || event.getAmount() <= 0 || !TameEntityAdapter.isTame(tame)) return;
        if (!(event.getSource().getEntity() instanceof Mob attacker) || attacker == tame) return;
        TameData data = data(tame);
        if (!eligible(tame, data) || TameDuelManager.isEntityInDuel(attacker.getUUID())) return;
        PARTICIPANTS.computeIfAbsent(attacker.getUUID(), ignored -> new HashMap<>())
                .put(data.uuid, new Participation(data.uuid, data.tlId, clock + PARTICIPATION_TICKS));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void died(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity().level() instanceof ServerLevel level)) return;
        Map<UUID, Participation> participants = PARTICIPANTS.remove(event.getEntity().getUUID());
        if (participants == null || TameDuelManager.isEntityInDuel(event.getEntity().getUUID())) return;
        for (Participation entry : participants.values()) {
            if (clock >= entry.expires()) continue;
            TameData data = TameRegistry.get(entry.tameId());
            if (data == null && entry.tlId() != null) data = TameRegistry.getByTlId(entry.tlId());
            if (data == null || data.dead) continue;
            LivingEntity tame = TameEntityAdapter.findLoaded(level.getServer(), data.uuid, data.tlId);
            if (tame != null && !eligible(tame, data)) continue;
            grantOneXp(tame, data);
        }
    }

    private static void grantOneXp(LivingEntity tame, TameData data) {
        // These are fixed activity rewards, not combat XP to split or multiply.
        if (data.xp < Integer.MAX_VALUE) data.xp++;
        if (tame != null) LevelSystem.checkLevelUp(tame, data);
        TameRegistry.markDirty();
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        LivingEntity tame = event.getEntity();
        if (tame.level().isClientSide) return;
        if (!TameEntityAdapter.isTame(tame)) { FOLLOW.remove(tame); return; }
        TameData data = data(tame);
        if (!eligible(tame, data)) { FOLLOW.remove(tame); return; }
        // Finish participation level-ups awarded while the pet was unloaded.
        if (data.xpToNext > 0 && data.xp >= data.xpToNext) LevelSystem.checkLevelUp(tame, data);
        guard(tame, data);
        LivingEntity owner = TameEntityAdapter.owner(tame);
        if (data.movementOrder != 0 || data.hasHome || data.wanderLock || data.hungerSaturation <= 0
                || !TameEntityAdapter.isFollowingOwner(tame) || owner == null || !owner.isAlive()
                || owner.level() != tame.level()) { FOLLOW.remove(tame); return; }
        FollowProgress progress = FOLLOW.computeIfAbsent(tame, ignored -> new FollowProgress());
        if (!owner.getUUID().equals(progress.owner)) {
            progress.owner = owner.getUUID(); progress.position = owner.position();
            progress.idle = 0; progress.ticks = 0;
        }
        if (owner.position().distanceToSqr(progress.position) > 1.0E-8) progress.idle = 0;
        else progress.idle++;
        progress.position = owner.position();
        if (progress.idle >= IDLE_TICKS) { progress.ticks = 0; return; }
        if (++progress.ticks >= FOLLOW_TICKS) { progress.ticks = 0; grantOneXp(tame, data); }
    }

    private static void guard(LivingEntity tame, TameData data) {
        var tag = tame.getPersistentData();
        if (data.movementOrder != 3 || !data.hasHome) {
            tag.remove(GUARD_PROGRESS); tag.remove(GUARD_PLACE); return;
        }
        String place = data.homeDimension + ":" + data.homeX + ":" + data.homeY + ":" + data.homeZ;
        if (!place.equals(tag.getString(GUARD_PLACE))) {
            tag.putString(GUARD_PLACE, place); tag.putInt(GUARD_PROGRESS, 0);
        }
        if (!tame.level().dimension().location().toString().equals(data.homeDimension)
                || tame.distanceToSqr(data.homeX + 0.5, data.homeY, data.homeZ + 0.5) > 16 * 16) return;
        int ticks = tag.getInt(GUARD_PROGRESS) + 1;
        if (ticks >= GUARD_TICKS) {
            ticks = 0;
            data.bonusHealth += 1;
            LevelSystem.refreshTrackedHealthBonus(tame, data);
            TameRegistry.markDirty();
        }
        tag.putInt(GUARD_PROGRESS, ticks);
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) { FOLLOW.remove(event.getEntity()); }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        clock++;
        if (clock % 20 != 0) return;
        PARTICIPANTS.values().removeIf(entries -> {
            entries.values().removeIf(entry -> clock >= entry.expires());
            return entries.isEmpty();
        });
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) { FOLLOW.clear(); PARTICIPANTS.clear(); clock = 0; }
}
