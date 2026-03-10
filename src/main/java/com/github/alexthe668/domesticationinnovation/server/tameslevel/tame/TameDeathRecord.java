package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.TamableAnimal;

import java.util.UUID;

public class TameDeathRecord {
    public UUID uuid;
    public UUID ownerUUID;
    public String name = "";
    public String type = "";
    public int level = 1;
    public int kills = 0;
    public int assists = 0;
    public int deaths = 0;
    public long deathGameTime = 0L;
    public long deathUnixMillis = 0L;
    public String deathDimension = "";
    public int deathX = 0;
    public int deathY = 0;
    public int deathZ = 0;
    public int reviveXpCost = 0;
    public String tameClass = "";
    public CompoundTag snapshot = new CompoundTag();
    public boolean reincarnated = false;
    public boolean autoReincarnateOnRespawn = false;

    public static TameDeathRecord fromTame(TameData data, TamableAnimal tame, long gameTime) {
        TameDeathRecord record = new TameDeathRecord();
        record.uuid = data.uuid;
        record.ownerUUID = data.ownerUUID;
        record.name = data.name;
        record.type = data.type;
        record.level = data.level;
        record.kills = data.kills;
        record.assists = data.assists;
        record.deaths = data.deaths;
        record.deathGameTime = gameTime;
        record.deathUnixMillis = System.currentTimeMillis();
        if (tame != null) {
            record.deathDimension = tame.level().dimension().location().toString();
            record.deathX = tame.blockPosition().getX();
            record.deathY = tame.blockPosition().getY();
            record.deathZ = tame.blockPosition().getZ();
        }
        record.reviveXpCost = Math.max(0, LevelSystem.estimateInvestedXp(data));
        record.tameClass = data.tameClass == null ? "" : data.tameClass.name();
        record.snapshot = data.toTag();
        return record;
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        if (uuid != null) tag.putUUID("uuid", uuid);
        if (ownerUUID != null) tag.putUUID("ownerUUID", ownerUUID);
        tag.putString("name", name == null ? "" : name);
        tag.putString("type", type == null ? "" : type);
        tag.putInt("level", level);
        tag.putInt("kills", kills);
        tag.putInt("assists", assists);
        tag.putInt("deaths", deaths);
        tag.putLong("deathGameTime", deathGameTime);
        tag.putLong("deathUnixMillis", deathUnixMillis);
        tag.putString("deathDimension", deathDimension == null ? "" : deathDimension);
        tag.putInt("deathX", deathX);
        tag.putInt("deathY", deathY);
        tag.putInt("deathZ", deathZ);
        tag.putInt("reviveXpCost", reviveXpCost);
        tag.putString("tameClass", tameClass == null ? "" : tameClass);
        tag.put("snapshot", snapshot == null ? new CompoundTag() : snapshot.copy());
        tag.putBoolean("reincarnated", reincarnated);
        tag.putBoolean("autoReincarnateOnRespawn", autoReincarnateOnRespawn);
        return tag;
    }

    public static TameDeathRecord fromTag(CompoundTag tag) {
        TameDeathRecord record = new TameDeathRecord();
        if (tag.hasUUID("uuid")) record.uuid = tag.getUUID("uuid");
        if (tag.hasUUID("ownerUUID")) record.ownerUUID = tag.getUUID("ownerUUID");
        if (tag.contains("name", Tag.TAG_STRING)) record.name = tag.getString("name");
        if (tag.contains("type", Tag.TAG_STRING)) record.type = tag.getString("type");
        record.level = Math.max(1, tag.getInt("level"));
        record.kills = Math.max(0, tag.getInt("kills"));
        record.assists = Math.max(0, tag.getInt("assists"));
        record.deaths = Math.max(0, tag.getInt("deaths"));
        record.deathGameTime = tag.getLong("deathGameTime");
        record.deathUnixMillis = tag.contains("deathUnixMillis", Tag.TAG_LONG) ? tag.getLong("deathUnixMillis") : 0L;
        if (tag.contains("deathDimension", Tag.TAG_STRING)) record.deathDimension = tag.getString("deathDimension");
        record.deathX = tag.getInt("deathX");
        record.deathY = tag.getInt("deathY");
        record.deathZ = tag.getInt("deathZ");
        record.reviveXpCost = Math.max(0, tag.getInt("reviveXpCost"));
        if (tag.contains("tameClass", Tag.TAG_STRING)) record.tameClass = tag.getString("tameClass");
        if (tag.contains("snapshot", Tag.TAG_COMPOUND)) record.snapshot = tag.getCompound("snapshot").copy();
        if (tag.contains("reincarnated", Tag.TAG_BYTE)) record.reincarnated = tag.getBoolean("reincarnated");
        if (tag.contains("autoReincarnateOnRespawn", Tag.TAG_BYTE)) {
            record.autoReincarnateOnRespawn = tag.getBoolean("autoReincarnateOnRespawn");
        }
        return record;
    }
}
