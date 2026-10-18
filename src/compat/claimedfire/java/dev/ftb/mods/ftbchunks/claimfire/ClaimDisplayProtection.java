package dev.ftb.mods.ftbchunks.claimfire;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Claimed displays may only receive a player's direct empty-hand or sword melee hit. */
@Mod.EventBusSubscriber(modid = "ftbchunks")
public final class ClaimDisplayProtection {
    private static final TagKey<Item> SWORDS = TagKey.create(Registries.ITEM, new ResourceLocation("forge", "tools/swords"));
    private ClaimDisplayProtection() { }

    public static boolean protectedDisplay(Entity entity) {
        return entity != null && !entity.level().isClientSide
                && (entity instanceof ArmorStand || entity instanceof ItemFrame)
                && ClaimFireProtection.protectedAt(entity.level(), entity instanceof ItemFrame frame ? frame.getPos() : entity.blockPosition());
    }

    public static boolean blockDamage(Entity entity, DamageSource source) {
        return !allowedPlayerMelee(source) && protectedDisplay(entity);
    }

    private static boolean allowedPlayerMelee(DamageSource source) {
        if (source == null || !source.is(DamageTypes.PLAYER_ATTACK)
                || source.is(DamageTypeTags.IS_PROJECTILE) || source.is(DamageTypeTags.IS_EXPLOSION)
                || !(source.getEntity() instanceof Player player) || source.getDirectEntity() != player) return false;
        ItemStack hand = player.getMainHandItem();
        return hand.isEmpty() || hand.getItem() instanceof SwordItem || hand.is(SWORDS);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!event.getLevel().isClientSide) event.getAffectedEntities().removeIf(ClaimDisplayProtection::protectedDisplay);
    }
}
