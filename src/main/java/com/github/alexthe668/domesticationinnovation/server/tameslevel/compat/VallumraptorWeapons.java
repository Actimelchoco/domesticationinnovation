package com.github.alexthe668.domesticationinnovation.server.tameslevel.compat;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelManager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.registries.ForgeRegistries;

public final class VallumraptorWeapons {
    private VallumraptorWeapons() { }

    public static boolean isWeapon(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem);
    }

    public static boolean isArmed(LivingEntity entity) {
        return entity instanceof TamableAnimal tame && tame.isTame()
                && "alexscaves:vallumraptor".equals(String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType())))
                && isWeapon(entity.getMainHandItem());
    }

    /** Sprint is deliberately separate from sneak's hunger inventory/stat shortcuts. */
    public static boolean canRetrieve(Player player, InteractionHand hand, Entity target) {
        if (hand != InteractionHand.MAIN_HAND || !player.isSprinting() || player.isShiftKeyDown()
                || !player.getMainHandItem().isEmpty() || !(target instanceof TamableAnimal tame)
                || !tame.isTame() || !player.getUUID().equals(tame.getOwnerUUID()) || !tame.isAlive()
                || !"alexscaves:vallumraptor".equals(String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(tame.getType())))
                || tame.getMainHandItem().isEmpty()
                || TameDuelManager.isEntityInDuel(player.getUUID()) || TameDuelManager.isEntityInDuel(tame.getUUID())) return false;
        return true;
    }

    public static boolean retrieve(PlayerInteractEvent event, Entity target) {
        var player = event.getEntity();
        if (!canRetrieve(player, event.getHand(), target)) return false;
        var tame = (TamableAnimal) target;
        if (!player.level().isClientSide) {
            ItemStack held = tame.getMainHandItem();
            tame.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            // Empty main hand guarantees room and avoids dropping an item beside its collector.
            player.setItemInHand(InteractionHand.MAIN_HAND, held);
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide));
        return true;
    }
}
