package com.github.alexthe668.domesticationinnovation.server.item;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameDrumEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.RegistryObject;

public class DrumItem extends DIBlockItem {
    private static final String TAG_DRUM_COMMAND = "DrumCommand";

    public DrumItem(RegistryObject<net.minecraft.world.level.block.Block> blockSupplier, Item.Properties props) {
        super(blockSupplier, props);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            return super.useOn(context);
        }
        if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
            serverPlayer.startUsingItem(context.getHand());
            TameDrumEvents.beginRightClickHold(serverPlayer, context.getClickedPos());
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer) || player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        serverPlayer.startUsingItem(hand);
        TameDrumEvents.beginRightClickHold(serverPlayer, null);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 120;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (level.isClientSide || !(living instanceof ServerPlayer player)) {
            return;
        }
        resolveHeldUse(player, stack, getUseDuration(stack) - timeLeft);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity living) {
        if (!level.isClientSide && living instanceof ServerPlayer player) {
            resolveHeldUse(player, stack, getUseDuration(stack));
        }
        return stack;
    }

    private void resolveHeldUse(ServerPlayer player, ItemStack stack, int heldTicks) {
        TameDrumEvents.RightClickHold hold = TameDrumEvents.finishRightClickHold(player);
        if (hold != null && heldTicks >= 100) {
            int result = hold.blockPos() == null
                    ? TameCommands.drumTeleportHome(player, stack)
                    : TameCommands.drumTeleportToBlock(player, stack, hold.blockPos());
            if (result > 0) {
                player.displayClientMessage(Component.literal("Drum: teleport triggered.")
                        .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
            return;
        }
        int command = getDrumCommand(stack);
        int affected = TameCommands.drumIssueMovementCommand(player, stack, command);
        advanceDrumCommand(stack);
        player.displayClientMessage(Component.literal("Drum: " + TameCommands.drumCommandLabel(command) + " (" + affected + ")")
                .withStyle(ChatFormatting.GOLD), true);
    }

    public static int getDrumCommand(ItemStack stack) {
        int value = stack.getOrCreateTag().getInt(TAG_DRUM_COMMAND);
        return value < 0 || value > 2 ? 0 : value;
    }

    public static void advanceDrumCommand(ItemStack stack) {
        stack.getOrCreateTag().putInt(TAG_DRUM_COMMAND, (getDrumCommand(stack) + 1) % 3);
    }
}
