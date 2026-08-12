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
    private final Set<String> cheapApprovedReincarnateItems = new LinkedHashSet<>();
    private final Map<UUID, String> respawnOrders = new HashMap<>();
    private final Map<UUID, Boolean> autoReincarnation = new HashMap<>();
    private final Map<UUID, Set<String>> doNotAttackTypes = new HashMap<>();
    private final Map<UUID, Boolean> doNotAttackAnimals = new HashMap<>();
    private final Map<UUID, Boolean> healthSiphon = new HashMap<>();
    private final Map<UUID, Boolean> enterPortalsByThemselves = new HashMap<>();
    private final Map<UUID, Set<String>> ownerGroups = new HashMap<>();
    private final Map<UUID, Set<String>> removeFromAllExclusions = new HashMap<>();
    private final Set<String> invertedCallOrderTypeIds = new LinkedHashSet<>();
    private final Set<String> disabledTameTypeIds = new LinkedHashSet<>(Set.of("minecraft:horse"));
    private final Map<UUID, PlayerDuelStats> playerDuelStats = new HashMap<>();
    private final Map<UUID, Integer> ownerTeleportApprovedCredits = new HashMap<>();
    private final Map<UUID, CompoundTag> playerDebugSettings = new HashMap<>();
    private String rankedArenaName = "";
    private final Set<UUID> rankedParticipants = new LinkedHashSet<>();
    private final List<CompoundTag> temporaryTames = new ArrayList<>();

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

    public Set<String> getCheapApprovedReincarnateItems() {
        return cheapApprovedReincarnateItems;
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

    public Map<UUID, Set<String>> getDoNotAttackTypes() {
        return doNotAttackTypes;
    }

    public void setDoNotAttackTypes(Map<UUID, Set<String>> doNotAttackTypes) {
        this.doNotAttackTypes.clear();
        if (doNotAttackTypes == null) {
            return;
        }
        for (Map.Entry<UUID, Set<String>> entry : doNotAttackTypes.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            Set<String> ids = new LinkedHashSet<>();
            if (entry.getValue() != null) {
                for (String id : entry.getValue()) {
                    if (id == null || id.isBlank()) {
                        continue;
                    }
                    ids.add(id.trim().toLowerCase(java.util.Locale.ROOT));
                }
            }
            if (!ids.isEmpty()) {
                this.doNotAttackTypes.put(entry.getKey(), ids);
            }
        }
    }

    public Map<UUID, Boolean> getDoNotAttackAnimals() {
        return doNotAttackAnimals;
    }

    public void setDoNotAttackAnimals(Map<UUID, Boolean> doNotAttackAnimals) {
        this.doNotAttackAnimals.clear();
        if (doNotAttackAnimals != null) {
            this.doNotAttackAnimals.putAll(doNotAttackAnimals);
        }
    }

    public Map<UUID, Boolean> getHealthSiphon() {
        return healthSiphon;
    }

    public void setHealthSiphon(Map<UUID, Boolean> healthSiphon) {
        this.healthSiphon.clear();
        if (healthSiphon != null) {
            this.healthSiphon.putAll(healthSiphon);
        }
    }

    public Map<UUID, Boolean> getEnterPortalsByThemselves() {
        return enterPortalsByThemselves;
    }

    public void setEnterPortalsByThemselves(Map<UUID, Boolean> enterPortalsByThemselves) {
        this.enterPortalsByThemselves.clear();
        if (enterPortalsByThemselves != null) {
            this.enterPortalsByThemselves.putAll(enterPortalsByThemselves);
        }
    }

    public Map<UUID, Set<String>> getOwnerGroups() {
        return ownerGroups;
    }

    public void setOwnerGroups(Map<UUID, Set<String>> ownerGroups) {
        this.ownerGroups.clear();
        if (ownerGroups == null) {
            return;
        }
        for (Map.Entry<UUID, Set<String>> entry : ownerGroups.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            Set<String> groups = new LinkedHashSet<>();
            for (String group : entry.getValue()) {
                if (group == null || group.isBlank()) {
                    continue;
                }
                groups.add(group.trim());
            }
            if (!groups.isEmpty()) {
                this.ownerGroups.put(entry.getKey(), groups);
            }
        }
    }

    public Set<String> getInvertedCallOrderTypeIds() {
        return invertedCallOrderTypeIds;
    }

    public void setInvertedCallOrderTypeIds(Set<String> invertedCallOrderTypeIds) {
        this.invertedCallOrderTypeIds.clear();
        if (invertedCallOrderTypeIds != null) {
            for (String id : invertedCallOrderTypeIds) {
                if (id == null || id.isBlank()) {
                    continue;
                }
                this.invertedCallOrderTypeIds.add(id.trim().toLowerCase(java.util.Locale.ROOT));
            }
        }
    }

    public Map<UUID, Set<String>> getRemoveFromAllExclusions() {
        return removeFromAllExclusions;
    }

    public void setRemoveFromAllExclusions(Map<UUID, Set<String>> removeFromAllExclusions) {
        this.removeFromAllExclusions.clear();
        if (removeFromAllExclusions == null) {
            return;
        }
        for (Map.Entry<UUID, Set<String>> entry : removeFromAllExclusions.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            Set<String> rules = new LinkedHashSet<>();
            for (String rule : entry.getValue()) {
                if (rule == null || rule.isBlank()) {
                    continue;
                }
                rules.add(rule.trim().toLowerCase(java.util.Locale.ROOT));
            }
            if (!rules.isEmpty()) {
                this.removeFromAllExclusions.put(entry.getKey(), rules);
            }
        }
    }

    public Set<String> getDisabledTameTypeIds() {
        return disabledTameTypeIds;
    }

    public void setDisabledTameTypeIds(Set<String> disabledTameTypeIds) {
        this.disabledTameTypeIds.clear();
        if (disabledTameTypeIds != null) {
            for (String id : disabledTameTypeIds) {
                if (id == null || id.isBlank()) {
                    continue;
                }
                this.disabledTameTypeIds.add(id.trim().toLowerCase(java.util.Locale.ROOT));
            }
        }
    }

    public Map<UUID, PlayerDuelStats> getPlayerDuelStats() {
        return playerDuelStats;
    }

    public void setPlayerDuelStats(Map<UUID, PlayerDuelStats> playerDuelStats) {
        this.playerDuelStats.clear();
        if (playerDuelStats == null) {
            return;
        }
        for (Map.Entry<UUID, PlayerDuelStats> entry : playerDuelStats.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            PlayerDuelStats copy = PlayerDuelStats.fromTag(entry.getValue().toTag());
            copy.playerUuid = entry.getKey();
            this.playerDuelStats.put(entry.getKey(), copy);
        }
    }

    public Map<UUID, Integer> getOwnerTeleportApprovedCredits() {
        return ownerTeleportApprovedCredits;
    }

    public void setOwnerTeleportApprovedCredits(Map<UUID, Integer> ownerTeleportApprovedCredits) {
        this.ownerTeleportApprovedCredits.clear();
        if (ownerTeleportApprovedCredits == null) {
            return;
        }
        for (Map.Entry<UUID, Integer> entry : ownerTeleportApprovedCredits.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            int credits = Math.max(0, entry.getValue() == null ? 0 : entry.getValue());
            if (credits > 0) {
                this.ownerTeleportApprovedCredits.put(entry.getKey(), credits);
            }
        }
    }

    public Map<UUID, CompoundTag> getPlayerDebugSettings() {
        Map<UUID, CompoundTag> copy = new HashMap<>();
        for (Map.Entry<UUID, CompoundTag> entry : playerDebugSettings.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                copy.put(entry.getKey(), entry.getValue().copy());
            }
        }
        return copy;
    }

    public void setPlayerDebugSettings(Map<UUID, CompoundTag> playerDebugSettings) {
        this.playerDebugSettings.clear();
        if (playerDebugSettings == null) {
            return;
        }
        for (Map.Entry<UUID, CompoundTag> entry : playerDebugSettings.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            this.playerDebugSettings.put(entry.getKey(), entry.getValue().copy());
        }
    }

    public String getRankedArenaName() {
        return rankedArenaName == null ? "" : rankedArenaName;
    }

    public void setRankedArenaName(String rankedArenaName) {
        this.rankedArenaName = rankedArenaName == null ? "" : rankedArenaName.trim();
    }

    public Set<UUID> getRankedParticipants() {
        return rankedParticipants;
    }

    public void setRankedParticipants(Set<UUID> rankedParticipants) {
        this.rankedParticipants.clear();
        if (rankedParticipants != null) {
            for (UUID participantId : rankedParticipants) {
                if (participantId != null) {
                    this.rankedParticipants.add(participantId);
                }
            }
        }
    }

    public List<CompoundTag> getTemporaryTames() {
        return temporaryTames;
    }

    public void setTemporaryTames(List<CompoundTag> temporaryTames) {
        this.temporaryTames.clear();
        if (temporaryTames == null) {
            return;
        }
        for (CompoundTag row : temporaryTames) {
            if (row != null && !row.isEmpty()) {
                this.temporaryTames.add(row.copy());
            }
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
        ListTag cheapApprovedItemsTag = new ListTag();
        for (String itemId : cheapApprovedReincarnateItems) {
            cheapApprovedItemsTag.add(net.minecraft.nbt.StringTag.valueOf(itemId));
        }
        tag.put("cheapApprovedReincarnateItems", cheapApprovedItemsTag);
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
        ListTag doNotAttackTypesTag = new ListTag();
        for (Map.Entry<UUID, Set<String>> entry : doNotAttackTypes.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            CompoundTag row = new CompoundTag();
            row.putUUID("ownerUUID", entry.getKey());
            ListTag ids = new ListTag();
            for (String id : entry.getValue()) {
                if (id == null || id.isBlank()) {
                    continue;
                }
                ids.add(net.minecraft.nbt.StringTag.valueOf(id));
            }
            row.put("mobTypes", ids);
            doNotAttackTypesTag.add(row);
        }
        tag.put("doNotAttackTypes", doNotAttackTypesTag);
        ListTag doNotAttackAnimalsTag = new ListTag();
        for (Map.Entry<UUID, Boolean> entry : doNotAttackAnimals.entrySet()) {
            if (entry.getKey() == null || !Boolean.TRUE.equals(entry.getValue())) {
                continue;
            }
            CompoundTag row = new CompoundTag();
            row.putUUID("ownerUUID", entry.getKey());
            row.putBoolean("enabled", true);
            doNotAttackAnimalsTag.add(row);
        }
        tag.put("doNotAttackAnimals", doNotAttackAnimalsTag);
        ListTag healthSiphonTag = new ListTag();
        for (Map.Entry<UUID, Boolean> entry : healthSiphon.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            CompoundTag row = new CompoundTag();
            row.putUUID("ownerUUID", entry.getKey());
            row.putBoolean("enabled", entry.getValue());
            healthSiphonTag.add(row);
        }
        tag.put("healthSiphon", healthSiphonTag);
        ListTag enterPortalsTag = new ListTag();
        for (Map.Entry<UUID, Boolean> entry : enterPortalsByThemselves.entrySet()) {
            if (entry.getKey() == null || !Boolean.TRUE.equals(entry.getValue())) {
                continue;
            }
            CompoundTag row = new CompoundTag();
            row.putUUID("ownerUUID", entry.getKey());
            row.putBoolean("enabled", true);
            enterPortalsTag.add(row);
        }
        tag.put("enterPortalsByThemselves", enterPortalsTag);
        ListTag ownerGroupsTag = new ListTag();
        for (Map.Entry<UUID, Set<String>> entry : ownerGroups.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            CompoundTag row = new CompoundTag();
            row.putUUID("ownerUUID", entry.getKey());
            ListTag groupsTag = new ListTag();
            for (String group : entry.getValue()) {
                if (group == null || group.isBlank()) {
                    continue;
                }
                groupsTag.add(net.minecraft.nbt.StringTag.valueOf(group));
            }
            row.put("groups", groupsTag);
            ownerGroupsTag.add(row);
        }
        tag.put("ownerGroups", ownerGroupsTag);
        ListTag removeFromAllTag = new ListTag();
        for (Map.Entry<UUID, Set<String>> entry : removeFromAllExclusions.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            CompoundTag row = new CompoundTag();
            row.putUUID("ownerUUID", entry.getKey());
            ListTag rulesTag = new ListTag();
            for (String rule : entry.getValue()) {
                if (rule == null || rule.isBlank()) {
                    continue;
                }
                rulesTag.add(net.minecraft.nbt.StringTag.valueOf(rule));
            }
            row.put("rules", rulesTag);
            removeFromAllTag.add(row);
        }
        tag.put("removeFromAllExclusions", removeFromAllTag);
        ListTag invertedCallOrderTag = new ListTag();
        for (String id : invertedCallOrderTypeIds) {
            if (id == null || id.isBlank()) {
                continue;
            }
            invertedCallOrderTag.add(net.minecraft.nbt.StringTag.valueOf(id));
        }
        tag.put("invertedCallOrderTypeIds", invertedCallOrderTag);
        ListTag disabledTameTypesTag = new ListTag();
        for (String id : disabledTameTypeIds) {
            if (id == null || id.isBlank()) {
                continue;
            }
            disabledTameTypesTag.add(net.minecraft.nbt.StringTag.valueOf(id));
        }
        tag.put("disabledTameTypeIds", disabledTameTypesTag);
        ListTag playerDuelStatsTag = new ListTag();
        for (PlayerDuelStats stats : playerDuelStats.values()) {
            if (stats == null || stats.playerUuid == null) {
                continue;
            }
            playerDuelStatsTag.add(stats.toTag());
        }
        tag.put("playerDuelStats", playerDuelStatsTag);
        ListTag teleportCreditsTag = new ListTag();
        for (Map.Entry<UUID, Integer> entry : ownerTeleportApprovedCredits.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            int credits = Math.max(0, entry.getValue() == null ? 0 : entry.getValue());
            if (credits <= 0) {
                continue;
            }
            CompoundTag row = new CompoundTag();
            row.putUUID("ownerUUID", entry.getKey());
            row.putInt("credits", credits);
            teleportCreditsTag.add(row);
        }
        tag.put("ownerTeleportApprovedCredits", teleportCreditsTag);
        ListTag playerDebugSettingsTag = new ListTag();
        for (Map.Entry<UUID, CompoundTag> entry : playerDebugSettings.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            CompoundTag row = new CompoundTag();
            row.putUUID("playerUUID", entry.getKey());
            row.put("settings", entry.getValue().copy());
            playerDebugSettingsTag.add(row);
        }
        tag.put("playerDebugSettings", playerDebugSettingsTag);
        if (rankedArenaName != null && !rankedArenaName.isBlank()) {
            tag.putString("rankedArenaName", rankedArenaName);
        }
        ListTag rankedParticipantsTag = new ListTag();
        for (UUID participantId : rankedParticipants) {
            if (participantId == null) {
                continue;
            }
            CompoundTag row = new CompoundTag();
            row.putUUID("participantUUID", participantId);
            rankedParticipantsTag.add(row);
        }
        tag.put("rankedParticipants", rankedParticipantsTag);
        ListTag temporaryTamesTag = new ListTag();
        for (CompoundTag row : temporaryTames) {
            if (row != null && !row.isEmpty()) {
                temporaryTamesTag.add(row.copy());
            }
        }
        tag.put("temporaryTames", temporaryTamesTag);
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
        if (tag.contains("cheapApprovedReincarnateItems", Tag.TAG_LIST)) {
            ListTag approved = tag.getList("cheapApprovedReincarnateItems", Tag.TAG_STRING);
            for (Tag entry : approved) {
                data.cheapApprovedReincarnateItems.add(entry.getAsString().trim().toLowerCase(java.util.Locale.ROOT));
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
        if (tag.contains("doNotAttackTypes", Tag.TAG_LIST)) {
            ListTag doNotAttackTypesList = tag.getList("doNotAttackTypes", Tag.TAG_COMPOUND);
            for (Tag entry : doNotAttackTypesList) {
                if (!(entry instanceof CompoundTag row) || !row.hasUUID("ownerUUID")) {
                    continue;
                }
                Set<String> ids = new LinkedHashSet<>();
                if (row.contains("mobTypes", Tag.TAG_LIST)) {
                    ListTag types = row.getList("mobTypes", Tag.TAG_STRING);
                    for (Tag typeEntry : types) {
                        String id = typeEntry.getAsString();
                        if (id != null && !id.isBlank()) {
                            ids.add(id.trim().toLowerCase(java.util.Locale.ROOT));
                        }
                    }
                }
                if (!ids.isEmpty()) {
                    data.doNotAttackTypes.put(row.getUUID("ownerUUID"), ids);
                }
            }
        }
        if (tag.contains("doNotAttackAnimals", Tag.TAG_LIST)) {
            ListTag doNotAttackAnimalsList = tag.getList("doNotAttackAnimals", Tag.TAG_COMPOUND);
            for (Tag entry : doNotAttackAnimalsList) {
                if (!(entry instanceof CompoundTag row) || !row.hasUUID("ownerUUID")) {
                    continue;
                }
                data.doNotAttackAnimals.put(row.getUUID("ownerUUID"), row.getBoolean("enabled"));
            }
        }
        if (tag.contains("healthSiphon", Tag.TAG_LIST)) {
            ListTag healthSiphonList = tag.getList("healthSiphon", Tag.TAG_COMPOUND);
            for (Tag entry : healthSiphonList) {
                if (!(entry instanceof CompoundTag row) || !row.hasUUID("ownerUUID")) {
                    continue;
                }
                data.healthSiphon.put(row.getUUID("ownerUUID"), row.getBoolean("enabled"));
            }
        }
        if (tag.contains("enterPortalsByThemselves", Tag.TAG_LIST)) {
            ListTag portalList = tag.getList("enterPortalsByThemselves", Tag.TAG_COMPOUND);
            for (Tag entry : portalList) {
                if (!(entry instanceof CompoundTag row) || !row.hasUUID("ownerUUID")) {
                    continue;
                }
                data.enterPortalsByThemselves.put(row.getUUID("ownerUUID"), row.getBoolean("enabled"));
            }
        }
        if (tag.contains("ownerGroups", Tag.TAG_LIST)) {
            ListTag ownerGroupsList = tag.getList("ownerGroups", Tag.TAG_COMPOUND);
            for (Tag entry : ownerGroupsList) {
                if (!(entry instanceof CompoundTag row) || !row.hasUUID("ownerUUID")) {
                    continue;
                }
                Set<String> groups = new LinkedHashSet<>();
                if (row.contains("groups", Tag.TAG_LIST)) {
                    ListTag groupsTag = row.getList("groups", Tag.TAG_STRING);
                    for (Tag groupEntry : groupsTag) {
                        String group = groupEntry.getAsString();
                        if (group != null && !group.isBlank()) {
                            groups.add(group.trim());
                        }
                    }
                }
                if (!groups.isEmpty()) {
                    data.ownerGroups.put(row.getUUID("ownerUUID"), groups);
                }
            }
        }
        if (tag.contains("removeFromAllExclusions", Tag.TAG_LIST)) {
            ListTag removeFromAllList = tag.getList("removeFromAllExclusions", Tag.TAG_COMPOUND);
            for (Tag entry : removeFromAllList) {
                if (!(entry instanceof CompoundTag row) || !row.hasUUID("ownerUUID")) {
                    continue;
                }
                Set<String> rules = new LinkedHashSet<>();
                if (row.contains("rules", Tag.TAG_LIST)) {
                    ListTag rulesTag = row.getList("rules", Tag.TAG_STRING);
                    for (Tag ruleEntry : rulesTag) {
                        String rule = ruleEntry.getAsString();
                        if (rule != null && !rule.isBlank()) {
                            rules.add(rule.trim().toLowerCase(java.util.Locale.ROOT));
                        }
                    }
                }
                if (!rules.isEmpty()) {
                    data.removeFromAllExclusions.put(row.getUUID("ownerUUID"), rules);
                }
            }
        }

        if (tag.contains("invertedCallOrderTypeIds", Tag.TAG_LIST)) {
            ListTag invertedCallOrderTag = tag.getList("invertedCallOrderTypeIds", Tag.TAG_STRING);
            for (Tag entry : invertedCallOrderTag) {
                if (entry instanceof net.minecraft.nbt.StringTag stringTag) {
                    String id = stringTag.getAsString();
                    if (id != null && !id.isBlank()) {
                        data.invertedCallOrderTypeIds.add(id.trim().toLowerCase(java.util.Locale.ROOT));
                    }
                }
            }
        }
        if (tag.contains("disabledTameTypeIds", Tag.TAG_LIST)) {
            data.disabledTameTypeIds.clear();
            ListTag disabledTameTypesTag = tag.getList("disabledTameTypeIds", Tag.TAG_STRING);
            for (Tag entry : disabledTameTypesTag) {
                if (entry instanceof net.minecraft.nbt.StringTag stringTag) {
                    String id = stringTag.getAsString();
                    if (id != null && !id.isBlank()) {
                        data.disabledTameTypeIds.add(id.trim().toLowerCase(java.util.Locale.ROOT));
                    }
                }
            }
        }
        if (tag.contains("playerDuelStats", Tag.TAG_LIST)) {
            ListTag playerDuelStatsList = tag.getList("playerDuelStats", Tag.TAG_COMPOUND);
            for (Tag entry : playerDuelStatsList) {
                if (!(entry instanceof CompoundTag row)) {
                    continue;
                }
                PlayerDuelStats stats = PlayerDuelStats.fromTag(row);
                if (stats.playerUuid != null) {
                    data.playerDuelStats.put(stats.playerUuid, stats);
                }
            }
        }
        if (tag.contains("ownerTeleportApprovedCredits", Tag.TAG_LIST)) {
            ListTag teleportCreditsList = tag.getList("ownerTeleportApprovedCredits", Tag.TAG_COMPOUND);
            for (Tag entry : teleportCreditsList) {
                if (!(entry instanceof CompoundTag row) || !row.hasUUID("ownerUUID")) {
                    continue;
                }
                int credits = Math.max(0, row.getInt("credits"));
                if (credits > 0) {
                    data.ownerTeleportApprovedCredits.put(row.getUUID("ownerUUID"), credits);
                }
            }
        }
        if (tag.contains("playerDebugSettings", Tag.TAG_LIST)) {
            ListTag playerDebugSettingsList = tag.getList("playerDebugSettings", Tag.TAG_COMPOUND);
            for (Tag entry : playerDebugSettingsList) {
                if (!(entry instanceof CompoundTag row) || !row.hasUUID("playerUUID") || !row.contains("settings", Tag.TAG_COMPOUND)) {
                    continue;
                }
                CompoundTag settings = row.getCompound("settings");
                if (!settings.isEmpty()) {
                    data.playerDebugSettings.put(row.getUUID("playerUUID"), settings.copy());
                }
            }
        }
        if (tag.contains("rankedArenaName", Tag.TAG_STRING)) {
            data.rankedArenaName = tag.getString("rankedArenaName").trim();
        }
        if (tag.contains("rankedParticipants", Tag.TAG_LIST)) {
            ListTag rankedParticipantsList = tag.getList("rankedParticipants", Tag.TAG_COMPOUND);
            for (Tag entry : rankedParticipantsList) {
                if (!(entry instanceof CompoundTag row) || !row.hasUUID("participantUUID")) {
                    continue;
                }
                data.rankedParticipants.add(row.getUUID("participantUUID"));
            }
        }
        if (tag.contains("temporaryTames", Tag.TAG_LIST)) {
            ListTag temporaryList = tag.getList("temporaryTames", Tag.TAG_COMPOUND);
            for (Tag entry : temporaryList) {
                if (entry instanceof CompoundTag row && !row.isEmpty()) {
                    data.temporaryTames.add(row.copy());
                }
            }
        }
        return data;
    }
}
