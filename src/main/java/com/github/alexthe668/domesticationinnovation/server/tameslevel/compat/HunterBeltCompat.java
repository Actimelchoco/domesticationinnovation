package com.github.alexthe668.domesticationinnovation.server.tameslevel.compat;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameEntityAdapter;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

/** Optional Relics/Curios compatibility; no dependency is required when they are absent. */
public final class HunterBeltCompat {
    private static final ResourceLocation BELT_ID = new ResourceLocation("relics", "hunter_belt");
    private static boolean failureReported;

    private record Api(Method getHelper, Method findCurios, Method stack,
                       Class<?> relicInterface, Method abilityValue, Method spreadExperience) {
        private static Api load() {
            try {
                Class<?> curios = Class.forName("top.theillusivec4.curios.api.CuriosApi");
                Class<?> helper = Class.forName("top.theillusivec4.curios.api.type.util.ICuriosHelper");
                Class<?> slotResult = Class.forName("top.theillusivec4.curios.api.SlotResult");
                Class<?> relic = Class.forName("it.hurts.sskirillss.relics.items.relics.base.IRelicItem");
                Method experience;
                try {
                    experience = relic.getMethod("spreadExperience", LivingEntity.class, ItemStack.class, int.class);
                } catch (NoSuchMethodException ignored) {
                    experience = null;
                }
                return new Api(curios.getMethod("getCuriosHelper"),
                        helper.getMethod("findCurios", LivingEntity.class, Item.class), slotResult.getMethod("stack"),
                        relic, relic.getMethod("getAbilityValue", ItemStack.class, String.class, String.class), experience);
            } catch (ReflectiveOperationException | LinkageError exception) {
                reportFailure(exception);
                return null;
            }
        }
    }

    private static final class ApiHolder {
        private static final Api API = Api.load();
    }

    private HunterBeltCompat() { }

    private static LivingEntity resolveAttacker(Entity source) {
        Entity attacker = source;
        if (source instanceof Projectile projectile) attacker = projectile.getOwner();
        else if (source instanceof EvokerFangs fangs) attacker = fangs.getOwner();
        else if (!(source instanceof LivingEntity) && source instanceof OwnableEntity ownable) attacker = ownable.getOwner();
        return attacker instanceof LivingEntity living && TameEntityAdapter.isTame(living) ? living : null;
    }

    /** True means the vanilla Relics listener can skip its single-belt damage calculation. */
    public static boolean applyDamage(LivingHurtEvent event) {
        if (event.isCanceled() || event.getEntity().level().isClientSide || event.getAmount() <= 0) return false;
        Item belt = ForgeRegistries.ITEMS.getValue(BELT_ID);
        if (belt == null || belt == Items.AIR) return false;
        LivingEntity tame = resolveAttacker(event.getSource().getEntity());
        if (tame == null) tame = resolveAttacker(event.getSource().getDirectEntity());
        if (tame == null || tame.getServer() == null) return false;
        TameData data = TameRegistry.get(tame.getUUID());
        if (data == null) {
            UUID tlId = TameData.getTlId(tame);
            if (tlId != null) data = TameRegistry.getByTlId(tlId);
        }
        // Registry ownership avoids borrowing belts from a fox's other trusted players.
        UUID ownerId = data != null && data.ownerUUID != null ? data.ownerUUID : TameEntityAdapter.ownerUuid(tame);
        Player player = ownerId == null ? null : tame.getServer().getPlayerList().getPlayer(ownerId);
        if (player == null) return false;
        Api api = ApiHolder.API;
        if (api == null) return false;
        try {
            Object helper = api.getHelper().invoke(null);
            Object found = api.findCurios().invoke(helper, player, belt);
            if (!(found instanceof List<?> slots)) return false;
            double multiplier = 1.0D;
            for (Object slot : slots) {
                Object value = api.stack().invoke(slot);
                if (!(value instanceof ItemStack stack) || stack.isEmpty() || !stack.is(belt)
                        || !api.relicInterface().isInstance(stack.getItem())) continue;
                if (api.spreadExperience() != null) {
                    try {
                        api.spreadExperience().invoke(stack.getItem(), player, stack, 1);
                    } catch (ReflectiveOperationException | LinkageError ignored) {
                        // Damage remains usable if optional XP bookkeeping fails.
                    }
                }
                Object ability = api.abilityValue().invoke(stack.getItem(), stack, "training", "damage");
                if (ability instanceof Number number && Double.isFinite(number.doubleValue())) {
                    multiplier += number.doubleValue() - 1.0D;
                }
            }
            event.setAmount((float) Math.min(Float.MAX_VALUE, event.getAmount() * Math.max(0.0D, multiplier)));
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            reportFailure(exception);
            return false;
        }
    }

    private static void reportFailure(Throwable exception) {
        if (!failureReported) {
            failureReported = true;
            System.err.println("[TamesLevel] Hunter Belt compatibility failed: " + exception);
        }
    }
}
