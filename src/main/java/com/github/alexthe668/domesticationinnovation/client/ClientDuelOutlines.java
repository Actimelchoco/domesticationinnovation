package com.github.alexthe668.domesticationinnovation.client;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameEntityAdapter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import static com.github.alexthe668.domesticationinnovation.client.ClientDuelOutlinePolicy.*;

/** Rendering-only view of existing duel membership. Never writes entity flags or scoreboard teams. */
public final class ClientDuelOutlines {
    private static ClientLevel level;
    private static Player viewer;
    private static String battle;
    private static long nextRefresh;

    private ClientDuelOutlines() {}

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (level != minecraft.level || viewer != minecraft.player) {
            level = minecraft.level;viewer = minecraft.player;battle = null;nextRefresh = 0;
        }
        if (level == null || viewer == null || !ClientDuelGlowSettings.duelGlow()) {
            battle = null;return;
        }
        long now = level.getGameTime();
        if (now < nextRefresh && nextRefresh - now <= 20) return;
        nextRefresh = now + 20;
        battle = battleKey(teamName(viewer));
        if (battle != null) return;
        // Owners may select only tames rather than themselves. One throttled pass
        // over client-tracked entities also handles vanilla and interface-based pets.
        for (Entity entity : level.entitiesForRendering()) {
            String key = battleKey(teamName(entity));
            if (key != null && viewer.getUUID().equals(TameEntityAdapter.ownerUuid(entity))) {
                battle = key;return;
            }
        }
    }

    public static int team(Entity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!ClientDuelGlowSettings.duelGlow() || level == null || level != minecraft.level
                || viewer == null || viewer != minecraft.player || entity == null || entity.level() != level
                || !(entity instanceof LivingEntity living) || !living.isAlive()) return 0;
        String name = teamName(entity);
        return teamForBattle(battle, name);
    }

    static String teamName(Entity entity) {
        return entity.getTeam() == null ? null : entity.getTeam().getName();
    }

}
