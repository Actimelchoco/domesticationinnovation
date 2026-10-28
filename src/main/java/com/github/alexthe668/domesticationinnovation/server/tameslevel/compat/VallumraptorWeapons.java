package com.github.alexthe668.domesticationinnovation.server.tameslevel.compat;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.registries.ForgeRegistries;

/** Native Vallumraptor hooks delegate shared hand behavior to HeldItemTameWeapons. */
public final class VallumraptorWeapons {
    private VallumraptorWeapons() { }
    public static boolean isWeapon(ItemStack stack){return HeldItemTameWeapons.isWeapon(stack);}
    public static boolean isTamedRaptor(LivingEntity entity){
        return entity instanceof TamableAnimal tame&&tame.isTame()
                &&"alexscaves:vallumraptor".equals(String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType())));
    }
    public static boolean isArmed(LivingEntity entity){return HeldItemTameWeapons.isArmed(entity);}
    public static void unsealOnKill(LivingDeathEvent event){HeldItemTameWeapons.unsealOnKill(event);}
    public static boolean canRetrieve(Player player,InteractionHand hand,Entity target){return HeldItemTameWeapons.canRetrieve(player,hand,target);}
    public static boolean retrieve(PlayerInteractEvent event,Entity target){return HeldItemTameWeapons.retrieve(event,target);}
}
