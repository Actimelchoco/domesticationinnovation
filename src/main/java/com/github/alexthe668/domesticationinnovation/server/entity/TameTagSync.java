package com.github.alexthe668.domesticationinnovation.server.entity;

import com.github.alexthe666.citadel.Citadel;
import com.github.alexthe666.citadel.server.entity.CitadelEntityData;
import com.github.alexthe666.citadel.server.message.PropertiesMessage;
import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.BiConsumer;

/** Server-thread-only batching of this mod's Citadel updates. Local state changes immediately. */
@Mod.EventBusSubscriber(modid = DomesticationMod.MODID)
public final class TameTagSync {
    private static final Set<LivingEntity> PENDING = Collections.newSetFromMap(new IdentityHashMap<>());
    private static MinecraftServer pendingServer;

    private TameTagSync() { }

    public static void queue(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level) || entity.isRemoved()) return;
        if (pendingServer != level.getServer()) {
            PENDING.clear();
            pendingServer = level.getServer();
        }
        PENDING.add(entity);
    }

    private static void flush(MinecraftServer server, BiConsumer<LivingEntity, CompoundTag> send) {
        if (pendingServer != server || PENDING.isEmpty()) return;
        LivingEntity[] entities = PENDING.toArray(LivingEntity[]::new);
        PENDING.clear();
        for (LivingEntity entity : entities) {
            if (entity.isRemoved() || !(entity.level() instanceof ServerLevel level)
                    || level.getServer() != server || level.getEntity(entity.getUUID()) != entity) continue;
            CompoundTag tag = CitadelEntityData.getCitadelTag(entity);
            if (tag != null) send.accept(entity, tag.copy());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        flush(event.getServer(), (entity, tag) -> Citadel.NETWORK_WRAPPER.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity),
                new PropertiesMessage("CitadelTagUpdate", tag, entity.getId())));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(event.getTarget() instanceof LivingEntity entity)) return;
        sendCurrent(player, entity);
    }

    public static void sendCurrent(ServerPlayer player, LivingEntity entity) {
        if (player.connection == null || player.level() != entity.level()) return;
        CompoundTag tag = CitadelEntityData.getCitadelTag(entity);
        if (tag != null && !tag.isEmpty()) {
            Citadel.NETWORK_WRAPPER.send(PacketDistributor.PLAYER.with(() -> player),
                    new PropertiesMessage("CitadelTagUpdate", tag.copy(), entity.getId()));
        }
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof LivingEntity entity) PENDING.remove(entity);
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        if (pendingServer == event.getServer()) {
            PENDING.clear();
            pendingServer = null;
        }
    }
}
