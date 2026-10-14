package dev.betterforbiddenarcanus.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BeaconBlockEntity.class)
public interface BeaconAccessor {
    @Accessor("levels") int bfa$levels();
    @Accessor("primaryPower") Holder<MobEffect> bfa$primaryPower();
}
