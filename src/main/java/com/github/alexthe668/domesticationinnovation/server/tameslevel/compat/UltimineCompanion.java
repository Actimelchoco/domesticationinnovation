package com.github.alexthe668.domesticationinnovation.server.tameslevel.compat;

import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelManager;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameEntityAdapter;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;

/** Optional FTB Ultimine bridge: all world access stays on the server thread. */
public final class UltimineCompanion {
    public static final double RANGE = 16.0D;
    public static final TagKey<EntityType<?>> ELIGIBLE = TagKey.create(Registries.ENTITY_TYPE,
            new ResourceLocation(DomesticationMod.MODID, "ultimine"));
    private static final ResourceLocation ROLYPOLY = new ResourceLocation("crittersandcompanions", "roly_poly");
    private static final ResourceKey<DamageType> STRAIN = ResourceKey.create(Registries.DAMAGE_TYPE,
            new ResourceLocation(DomesticationMod.MODID, "ultimine"));

    private UltimineCompanion() { }

    private static boolean isRolypoly(LivingEntity tame) {
        return ROLYPOLY.equals(ForgeRegistries.ENTITY_TYPES.getKey(tame.getType()));
    }

    private static TameData data(LivingEntity tame) {
        TameData data = TameRegistry.get(tame.getUUID());
        return data != null ? data : TameRegistry.getByTlId(TameData.getTlId(tame));
    }

    private static boolean eligible(ServerPlayer player, LivingEntity tame) {
        if (!tame.isAlive() || !TameEntityAdapter.isTame(tame)
                || !player.getUUID().equals(TameEntityAdapter.ownerUuid(tame))
                || tame.distanceToSqr(player) > RANGE * RANGE
                || TameDuelManager.isEntityInDuel(tame.getUUID())) return false;
        TameData data = data(tame);
        return data != null && player.getUUID().equals(data.ownerUUID) && !data.dead && !data.stored
                && !TameDuelManager.isEntityInDuel(data.tlId)
                && (isRolypoly(tame) || tame.getType().is(ELIGIBLE)
                    || LevelSystem.getAttributeLevel(data, "ultimine") > 0);
    }

    private static List<LivingEntity> companions(ServerPlayer player) {
        return player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(RANGE), tame -> eligible(player, tame));
    }

    /** Never overrides FTB's own permissions, tool checks, cooldowns or hunger checks. */
    public static boolean hasCompanion(ServerPlayer player) {
        return !companions(player).isEmpty();
    }

    public static int miningLevel(BlockState state) {
        int level = 1; // Wood/gold/no tool requirement still has a nonzero cost.
        for (var tier : TierSortingRegistry.getSortedTiers()) {
            if (tier.getTag() != null && state.is(tier.getTag())) level = Math.max(level, tier.getLevel());
        }
        return level;
    }

    /** Called around an individual FTB block removal, preserving protection hooks and drops. */
    public static boolean breakBlock(ServerPlayer player, BlockPos pos, BooleanSupplier destroy) {
        BlockState state = player.serverLevel().getBlockState(pos);
        int multiplier = miningLevel(state);
        List<LivingEntity> candidates = companions(player);
        candidates.sort(Comparator.<LivingEntity, Boolean>comparing(tame -> !isRolypoly(tame))
                .thenComparingDouble(tame -> tame.distanceToSqr(player)));
        for (LivingEntity tame : candidates) {
            TameData data = data(tame);
            boolean rolypoly = isRolypoly(tame);
            int saturation = (int) Math.min(Integer.MAX_VALUE, Math.ceil((double) (rolypoly ? 67 : 100)
                    * multiplier * HeldItemTameWeapons.weaponSaturationMultiplier(tame)));
            if (!TameCommands.prepareHungerForUltimine(data, tame, saturation)) continue;
            if (!destroy.getAsBoolean()) return false;
            data.hungerSaturation = Math.max(0, data.hungerSaturation - saturation);
            data.hungerEmptyNotified = false;
            TameRegistry.markDirty();
            // Custom physical damage allows armor/collar mitigation, but bypasses hurt cooldowns
            // so every block costs damage even when the entire batch breaks in one tick.
            tame.hurt(new DamageSource(player.serverLevel().registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(STRAIN)),
                    (rolypoly ? 1.0F : 5.0F) * multiplier);
            return true;
        }
        return false;
    }
}
