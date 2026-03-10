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
        return data;
    }
}
