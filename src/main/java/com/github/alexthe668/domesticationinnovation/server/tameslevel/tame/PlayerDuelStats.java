package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.Map;
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
    public int rankedSaturation = 0;
    public long ariseCostDay = Long.MIN_VALUE;
    public int ariseUsesToday = 0;
    /** Successful Animights creations, keyed by the resulting entity type. */
    public final Map<String, Integer> animightConversions = new HashMap<>();

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
        tag.putInt("rankedSaturation", Math.max(0, rankedSaturation));
        tag.putLong("ariseCostDay", ariseCostDay);
        tag.putInt("ariseUsesToday", Math.max(0, ariseUsesToday));
        CompoundTag animightTag = new CompoundTag();
        animightConversions.forEach((type, count) -> {
            if (type != null && !type.isBlank() && count != null && count > 0) {
                animightTag.putInt(type, count);
            }
        });
        tag.put("animightConversions", animightTag);
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
            data.rankedSaturation = Math.max(0, tag.getInt("rankedSaturation"));
            if (tag.contains("ariseCostDay", Tag.TAG_LONG)) {
                data.ariseCostDay = tag.getLong("ariseCostDay");
            }
            data.ariseUsesToday = Math.max(0, tag.getInt("ariseUsesToday"));
            if (tag.contains("animightConversions", Tag.TAG_COMPOUND)) {
                CompoundTag animightTag = tag.getCompound("animightConversions");
                for (String type : animightTag.getAllKeys()) {
                    int count = Math.max(0, animightTag.getInt(type));
                    if (count > 0) data.animightConversions.put(type, count);
                }
            }
        }
        return data;
    }
}
