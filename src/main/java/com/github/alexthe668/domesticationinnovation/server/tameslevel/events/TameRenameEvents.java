package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class TameRenameEvents {
    private static final java.util.regex.Pattern LEVEL_PREFIX =
            java.util.regex.Pattern.compile("^\\s*\\[(?:(?:lvl|level)\\s*)?\\d+\\]\\s*", java.util.regex.Pattern.CASE_INSENSITIVE);

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (handleStatShortcut(event, event.getTarget())) return;
        handleRename(event.getTarget(), event.getItemStack());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (handleStatShortcut(event, event.getTarget())) return;
        handleRename(event.getTarget(), event.getItemStack());
    }

    private static boolean handleStatShortcut(PlayerInteractEvent event, net.minecraft.world.entity.Entity target) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return false;
        if (!player.isShiftKeyDown()) return false;
        if (!(target instanceof TamableAnimal tame) || !tame.isTame()) return false;
        if (tame.getOwnerUUID() == null || !tame.getOwnerUUID().equals(player.getUUID())) return false;

        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || !(stack.getItem() instanceof SwordItem)) {
            return false;
        }

        TameData data = TameRegistry.get(tame.getUUID());
        if (data == null || data.name == null || data.name.isBlank()) return false;
        TameCommands.showSneakInteractStats(player, tame);

        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        event.setResult(Event.Result.ALLOW);
        return true;
    }

    private static void handleRename(net.minecraft.world.entity.Entity target, ItemStack stack) {
        if (!(target instanceof TamableAnimal tame)) return;
        if (!tame.isTame()) return;
        if (stack.isEmpty()) return;
        String id = stack.getItem().builtInRegistryHolder().key().location().toString();
        if (!"minecraft:name_tag".equals(id) && !"domesticationinnovation:collar_tag".equals(id)) return;
        if (!stack.hasCustomHoverName()) return;

        TameData data = TameRegistry.get(tame.getUUID());
        if (data == null) return;

        String renamed = stripLevelPrefixes(stack.getHoverName().getString());
        if (!renamed.isBlank()) {
            String unique = TameSpawnEvents.uniqueLoadedNameFor(tame, renamed);
            if (unique.equals(data.name)) return;
            data.name = unique;
            LevelSystem.updateTameName(tame, data);
            TameRegistry.markDirty();
        }
    }

    private static String stripLevelPrefixes(String name) {
        if (name == null) return "";
        String cleaned = name;
        while (true) {
            String next = LEVEL_PREFIX.matcher(cleaned).replaceFirst("");
            if (next.equals(cleaned)) break;
            cleaned = next;
        }
        return cleaned.trim();
    }
}
