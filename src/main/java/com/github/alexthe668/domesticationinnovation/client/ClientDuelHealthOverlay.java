package com.github.alexthe668.domesticationinnovation.client;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameEntityAdapter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import java.util.ArrayList;
import java.util.List;

public final class ClientDuelHealthOverlay {
    private static ClientLevel level;
    private static String battle;
    private static long nextRefresh;
    private static List<ClientDuelHealthPolicy.Candidate> candidates = List.of();
    private ClientDuelHealthOverlay() { }
    public static void clear() { level = null; battle = null; candidates = List.of(); nextRefresh = 0; }

    public static void tick() {
        var minecraft = Minecraft.getInstance();
        String currentBattle = ClientDuelOutlines.battle();
        if (!ClientDuelGlowSettings.duelHealth() || minecraft.level == null || currentBattle == null) {
            clear(); return;
        }
        if (level != minecraft.level || !currentBattle.equals(battle)) {
            clear(); level = minecraft.level; battle = currentBattle;
        }
        long now = level.getGameTime();
        if (now < nextRefresh && nextRefresh - now <= 10) return;
        nextRefresh = now + 10;
        var rows = new ArrayList<ClientDuelHealthPolicy.Candidate>();
        for (var entity : level.entitiesForRendering()) {
            if (entity instanceof LivingEntity living && living.isAlive() && TameEntityAdapter.isTame(entity)
                    && ClientDuelOutlinePolicy.teamForBattle(battle, ClientDuelOutlines.teamName(entity)) != 0) {
                rows.add(new ClientDuelHealthPolicy.Candidate(entity.getId(), entity.getName().getString(),
                        ClientDuelOutlines.teamName(entity), true, true));
            }
        }
        candidates = ClientDuelHealthPolicy.select(battle, rows, ClientDuelHealthPolicy.MAX_NAMES);
    }

    public static void render(GuiGraphics graphics, int startY) {
        var minecraft = Minecraft.getInstance();
        if (!ClientDuelGlowSettings.duelHealth() || minecraft.options.hideGui || minecraft.level != level
                || battle == null || !battle.equals(ClientDuelOutlines.battle())) return;
        var font = minecraft.font;
        int width = minecraft.getWindow().getGuiScaledWidth();
        int bottom = minecraft.getWindow().getGuiScaledHeight() / 2;
        int y = startY;
        var visible = candidates.subList(0, Math.min(candidates.size(), ClientDuelGlowSettings.duelHealthMaxNames()));
        for (var row : visible) {
            if (y + font.lineHeight > bottom) break;
            var entity = level.getEntity(row.entityId());
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) continue;
            int team = ClientDuelOutlinePolicy.teamForBattle(battle, ClientDuelOutlines.teamName(entity));
            if (team == 0 || !TameEntityAdapter.isTame(entity)) continue;
            String health = " " + Math.round(living.getHealth()) + "/" + Math.round(living.getMaxHealth());
            String name = living.getName().getString();
            int available = Math.max(0, width - 20 - font.width(health));
            if (font.width(name) > available) name = font.plainSubstrByWidth(name,
                    Math.max(0, available - font.width("..."))) + "...";
            String label = name + health;
            graphics.drawString(font, label, (width - font.width(label)) / 2, y,
                    ClientDuelOutlinePolicy.color(team), true);
            y += font.lineHeight + 2;
        }
    }
}
