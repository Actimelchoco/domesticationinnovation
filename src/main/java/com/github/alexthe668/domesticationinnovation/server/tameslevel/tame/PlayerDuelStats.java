package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.UUID;

public class PlayerDuelStats {
    public static final int DEFAULT_MMR = 1000;

    public UUID playerUuid;
    public String lastKnownName = "";
    public int duelMmr = DEFAULT_MMR;
    public int duelKills = 0;
    public int duelAssists = 0;
    public int duelDeaths = 0;
    public int duelWins = 0;
    public int duelLosses = 0;
    public int duelCount = 0;
    public double duelPoints = 0.0D;

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        if (playerUuid != null) {
            tag.putUUID("playerUuid", playerUuid);
        }
        tag.putString("lastKnownName", lastKnownName == null ? "" : lastKnownName);
        tag.putInt("duelMmr", duelMmr);
        tag.putInt("duelKills", Math.max(0, duelKills));
        tag.putInt("duelAssists", Math.max(0, duelAssists));
        tag.putInt("duelDeaths", Math.max(0, duelDeaths));
        tag.putInt("duelWins", Math.max(0, duelWins));
        tag.putInt("duelLosses", Math.max(0, duelLosses));
        tag.putInt("duelCount", Math.max(0, duelCount));
        tag.putDouble("duelPoints", Math.max(0.0D, duelPoints));
        return tag;
    }

    public static PlayerDuelStats fromTag(CompoundTag tag) {
        PlayerDuelStats data = new PlayerDuelStats();
        if (tag != null && tag.hasUUID("playerUuid")) {
            data.playerUuid = tag.getUUID("playerUuid");
        }
        if (tag != null && tag.contains("lastKnownName", Tag.TAG_STRING)) {
            data.lastKnownName = tag.getString("lastKnownName");
        }
        if (tag != null) {
            if (tag.contains("duelMmr", Tag.TAG_INT)) {
                data.duelMmr = tag.getInt("duelMmr");
            } else {
                data.duelMmr = DEFAULT_MMR;
            }
            data.duelKills = Math.max(0, tag.getInt("duelKills"));
            data.duelAssists = Math.max(0, tag.getInt("duelAssists"));
            data.duelDeaths = Math.max(0, tag.getInt("duelDeaths"));
            data.duelWins = Math.max(0, tag.getInt("duelWins"));
            data.duelLosses = Math.max(0, tag.getInt("duelLosses"));
            data.duelCount = Math.max(0, tag.getInt("duelCount"));
            data.duelPoints = Math.max(0.0D, tag.getDouble("duelPoints"));
        }
        return data;
    }
}
