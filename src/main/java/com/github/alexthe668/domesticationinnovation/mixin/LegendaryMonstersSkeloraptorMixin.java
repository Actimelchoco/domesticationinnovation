package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/** Preserve native attack damage and timing while allowing assigned duel opponents. */
@Pseudo
@Mixin(targets = "net.miauczel.legendary_monsters.entity.AnimatedMonster.Mobs.Pets.SkeloraptorEntity")
public abstract class LegendaryMonstersSkeloraptorMixin extends TamableAnimal {
    protected LegendaryMonstersSkeloraptorMixin(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        if (other != null && TameDuelManager.areDuelOpponents(this.getUUID(), other.getUUID())) {
            return false;
        }
        return super.isAlliedTo(other);
    }
}
