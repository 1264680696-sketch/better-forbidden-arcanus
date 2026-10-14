package dev.betterforbiddenarcanus.mixin;

import dev.betterforbiddenarcanus.BeaconFlight;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BeaconBlockEntity.class)
public abstract class BeaconFlightMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private static void bfa$trackFlight(Level level, BlockPos pos, BlockState state, BeaconBlockEntity beacon, CallbackInfo ci) {
        BeaconFlight.track(level, pos, beacon);
    }
}
