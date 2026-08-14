package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameEntityAdapter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class TameRenameEvents {
    private static final java.util.regex.Pattern LEVEL_PREFIX =
            java.util.regex.Pattern.compile("^\\s*\\[(?:(?:lvl|level)\\s*)?\\d+\\]\\s*", java.util.regex.Pattern.CASE_INSENSITIVE);
    private static final String DUPLICATE_RENAME_REJECT_TICK_TAG = "TLLastDuplicateRenameRejectTick";

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (handleStatShortcut(event, event.getTarget())) return;
        handleRename(event, event.getTarget(), event.getItemStack());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (handleStatShortcut(event, event.getTarget())) return;
        handleRename(event, event.getTarget(), event.getItemStack());
    }

    private static boolean handleStatShortcut(PlayerInteractEvent event, net.minecraft.world.entity.Entity target) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return false;
        if (!player.isShiftKeyDown()) return false;
        if (!(target instanceof LivingEntity tame) || !TameEntityAdapter.isTame(tame)) return false;
        if (!player.getUUID().equals(TameEntityAdapter.ownerUuid(tame))) return false;

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

    private static void handleRename(PlayerInteractEvent event, net.minecraft.world.entity.Entity target, ItemStack stack) {
        if (!(target instanceof LivingEntity tame) || !TameEntityAdapter.isTame(tame)) return;
        if (stack.isEmpty()) return;
        String id = stack.getItem().builtInRegistryHolder().key().location().toString();
        if (!"minecraft:name_tag".equals(id) && !"domesticationinnovation:collar_tag".equals(id)) return;
        if (!stack.hasCustomHoverName()) return;

        TameData data = TameRegistry.get(tame.getUUID());
        if (data == null) return;

        String renamed = stripLevelPrefixes(stack.getHoverName().getString());
        if (!renamed.isBlank()) {
            if (TameSpawnEvents.hasServerNameConflict(tame, renamed)) {
                LevelSystem.updateTameName(tame, data);
                if (event.getEntity() instanceof ServerPlayer player) {
                    long now = player.serverLevel().getGameTime();
                    long last = tame.getPersistentData().getLong(DUPLICATE_RENAME_REJECT_TICK_TAG);
                    if (last != now) {
                        player.sendSystemMessage(Component.literal("That tame name is already used on this server. " + data.name + " kept its previous name."));
                        tame.getPersistentData().putLong(DUPLICATE_RENAME_REJECT_TICK_TAG, now);
                    }
                }
                event.setCanceled(true);
                event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
                event.setResult(Event.Result.DENY);
                return;
            }
            if (renamed.equals(data.name)) return;
            data.name = renamed;
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
