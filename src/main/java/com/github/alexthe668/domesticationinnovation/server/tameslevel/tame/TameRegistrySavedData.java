package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Set;

public class TameRegistrySavedData extends SavedData {

    public static final String DATA_NAME = "tameslevel_registry";

    private final Map<UUID, TameData> tames = new HashMap<>();
    private final Map<UUID, TameDeathRecord> lastDeaths = new HashMap<>();
    private final List<TameDeathRecord> deathHistory = new ArrayList<>();
    private final Set<String> approvedReincarnateItems = new LinkedHashSet<>();
    private final Map<UUID, String> respawnOrders = new HashMap<>();
    private final Map<UUID, Boolean> autoReincarnation = new HashMap<>();

    public Map<UUID, TameData> getTames() {
        return tames;
    }

    public Map<UUID, TameDeathRecord> getLastDeaths() {
        return lastDeaths;
    }

    public List<TameDeathRecord> getDeathHistory() {
        return deathHistory;
    }

    public Set<String> getApprovedReincarnateItems() {
        return approvedReincarnateItems;
    }

    public Map<UUID, String> getRespawnOrders() {
        return respawnOrders;
    }

    public void setRespawnOrders(Map<UUID, String> respawnOrders) {
        this.respawnOrders.clear();
        if (respawnOrders != null) {
            this.respawnOrders.putAll(respawnOrders);
        }
    }

    public Map<UUID, Boolean> getAutoReincarnation() {
        return autoReincarnation;
    }

    public void setAutoReincarnation(Map<UUID, Boolean> autoReincarnation) {
        this.autoReincarnation.clear();
        if (autoReincarnation != null) {
            this.autoReincarnation.putAll(autoReincarnation);
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag tamesTag = new ListTag();
        for (TameData data : tames.values()) {
            tamesTag.add(data.toTag());
        }
        tag.put("tames", tamesTag);

        ListTag deathsTag = new ListTag();
        for (TameDeathRecord death : lastDeaths.values()) {
            deathsTag.add(death.toTag());
        }
        tag.put("lastDeaths", deathsTag);

        ListTag deathHistoryTag = new ListTag();
        for (TameDeathRecord death : deathHistory) {
            deathHistoryTag.add(death.toTag());
        }
        tag.put("deathHistory", deathHistoryTag);

        ListTag approvedItemsTag = new ListTag();
        for (String itemId : approvedReincarnateItems) {
            approvedItemsTag.add(net.minecraft.nbt.StringTag.valueOf(itemId));
        }
        tag.put("approvedReincarnateItems", approvedItemsTag);
        ListTag respawnOrdersTag = new ListTag();
        for (Map.Entry<UUID, String> entry : respawnOrders.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            CompoundTag row = new CompoundTag();
            row.putUUID("ownerUUID", entry.getKey());
            row.putString("order", entry.getValue() == null ? "default" : entry.getValue());
            respawnOrdersTag.add(row);
        }
        tag.put("respawnOrders", respawnOrdersTag);
        ListTag autoReincarnationTag = new ListTag();
        for (Map.Entry<UUID, Boolean> entry : autoReincarnation.entrySet()) {
            if (entry.getKey() == null || !Boolean.TRUE.equals(entry.getValue())) {
                continue;
            }
            CompoundTag row = new CompoundTag();
            row.putUUID("ownerUUID", entry.getKey());
            row.putBoolean("enabled", true);
            autoReincarnationTag.add(row);
        }
        tag.put("autoReincarnation", autoReincarnationTag);
        return tag;
    }

    public static TameRegistrySavedData load(CompoundTag tag) {
        TameRegistrySavedData data = new TameRegistrySavedData();
        if (!tag.contains("tames", Tag.TAG_LIST)) {
            return data;
        }

        ListTag list = tag.getList("tames", Tag.TAG_COMPOUND);
        for (Tag entry : list) {
            if (!(entry instanceof CompoundTag compound)) continue;
            TameData tameData = TameData.fromTag(compound);
            data.tames.put(tameData.uuid, tameData);
        }

        if (tag.contains("lastDeaths", Tag.TAG_LIST)) {
            ListTag deathList = tag.getList("lastDeaths", Tag.TAG_COMPOUND);
            for (Tag entry : deathList) {
                if (!(entry instanceof CompoundTag compound)) continue;
                TameDeathRecord death = TameDeathRecord.fromTag(compound);
                if (death.uuid != null) {
                    data.lastDeaths.put(death.uuid, death);
                }
            }
        }

        if (tag.contains("deathHistory", Tag.TAG_LIST)) {
            ListTag deathHistoryList = tag.getList("deathHistory", Tag.TAG_COMPOUND);
            for (Tag entry : deathHistoryList) {
                if (!(entry instanceof CompoundTag compound)) continue;
                data.deathHistory.add(TameDeathRecord.fromTag(compound));
            }
        }
        if (tag.contains("approvedReincarnateItems", Tag.TAG_LIST)) {
            ListTag approved = tag.getList("approvedReincarnateItems", Tag.TAG_STRING);
            for (Tag entry : approved) {
                data.approvedReincarnateItems.add(entry.getAsString().trim().toLowerCase(java.util.Locale.ROOT));
            }
        }
        if (tag.contains("respawnOrders", Tag.TAG_LIST)) {
            ListTag respawnOrderList = tag.getList("respawnOrders", Tag.TAG_COMPOUND);
            for (Tag entry : respawnOrderList) {
                if (!(entry instanceof CompoundTag row) || !row.hasUUID("ownerUUID")) {
                    continue;
                }
                data.respawnOrders.put(row.getUUID("ownerUUID"), row.getString("order"));
            }
        }
        if (tag.contains("autoReincarnation", Tag.TAG_LIST)) {
            ListTag autoReincarnationList = tag.getList("autoReincarnation", Tag.TAG_COMPOUND);
            for (Tag entry : autoReincarnationList) {
                if (!(entry instanceof CompoundTag row) || !row.hasUUID("ownerUUID")) {
                    continue;
                }
                data.autoReincarnation.put(row.getUUID("ownerUUID"), row.getBoolean("enabled"));
            }
        }
        return data;
    }
}
