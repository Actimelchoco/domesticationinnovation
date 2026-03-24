package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.UUID;

public final class OpenPartiesClaimsCompat {

    private static final String MOD_ID = "openpartiesandclaims";
    private static volatile boolean initialized;
    private static volatile boolean available;
    private static Method getApiMethod;
    private static Method getServerClaimsManagerMethod;
    private static Method getClaimMethod;
    private static Method getPlayerIdMethod;

    private OpenPartiesClaimsCompat() {
    }

    public static UUID getClaimOwner(Entity entity) {
        if (entity == null || entity.level().isClientSide) {
            return null;
        }
        MinecraftServer server = entity.level().getServer();
        if (server == null || !ensureInitialized()) {
            return null;
        }
        try {
            Object api = getApiMethod.invoke(null, server);
            if (api == null) {
                return null;
            }
            Object claimsManager = getServerClaimsManagerMethod.invoke(api);
            if (claimsManager == null) {
                return null;
            }
            ResourceLocation dimensionId = entity.level().dimension().location();
            Object claim = getClaimMethod.invoke(claimsManager, dimensionId, new ChunkPos(entity.blockPosition()));
            if (claim == null) {
                return null;
            }
            Object owner = getPlayerIdMethod.invoke(claim);
            return owner instanceof UUID uuid ? uuid : null;
        } catch (Throwable ignored) {
            available = false;
            return null;
        }
    }

    private static boolean ensureInitialized() {
        if (initialized) {
            return available;
        }
        synchronized (OpenPartiesClaimsCompat.class) {
            if (initialized) {
                return available;
            }
            initialized = true;
            if (!ModList.get().isLoaded(MOD_ID)) {
                available = false;
                return false;
            }
            try {
                Class<?> apiClass = Class.forName("xaero.pac.common.server.api.OpenPACServerAPI");
                Class<?> claimsManagerClass = Class.forName("xaero.pac.common.server.claims.api.IServerClaimsManagerAPI");
                Class<?> claimClass = Class.forName("xaero.pac.common.claims.player.api.IPlayerChunkClaimAPI");
                getApiMethod = apiClass.getMethod("get", MinecraftServer.class);
                getServerClaimsManagerMethod = apiClass.getMethod("getServerClaimsManager");
                getClaimMethod = claimsManagerClass.getMethod("get", ResourceLocation.class, ChunkPos.class);
                getPlayerIdMethod = claimClass.getMethod("getPlayerId");
                available = true;
            } catch (Throwable ignored) {
                available = false;
            }
            return available;
        }
    }
}
