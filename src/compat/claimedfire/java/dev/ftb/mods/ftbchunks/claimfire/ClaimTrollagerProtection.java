package dev.ftb.mods.ftbchunks.claimfire;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** Preserve claimed destinations in a Trollager smash, including border explosions. */
@Mod.EventBusSubscriber(modid="ftbchunks")
public final class ClaimTrollagerProtection {
    private static final ResourceLocation TROLLAGER=new ResourceLocation("primitive_mobs","trollager");
    private ClaimTrollagerProtection() { }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void detonate(ExplosionEvent.Detonate event){
        if(!(event.getLevel() instanceof ServerLevel level))return;
        var source=event.getExplosion().getDirectSourceEntity();
        if(source==null||!TROLLAGER.equals(ForgeRegistries.ENTITY_TYPES.getKey(source.getType())))return;
        event.getAffectedBlocks().removeIf(pos->ClaimFireProtection.protectedAt(level,pos));
    }
}
