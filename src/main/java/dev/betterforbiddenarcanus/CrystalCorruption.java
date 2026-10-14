package dev.betterforbiddenarcanus;

import com.stal111.forbidden_arcanus.core.init.ModBlocks;
import com.stal111.forbidden_arcanus.core.init.ModItems;
import com.stal111.forbidden_arcanus.core.init.ModSounds;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = BetterForbiddenArcanus.MOD_ID)
public final class CrystalCorruption {
    private CrystalCorruption() {}
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void use(PlayerInteractEvent.RightClickBlock event) {
        var player = event.getEntity(); var level = event.getLevel();
        var pos = event.getPos(); var stack = event.getItemStack();
        if (!stack.is(ModItems.CORRUPTI_DUST.get()) || !level.getBlockState(pos).is(ModBlocks.ARCANE_CRYSTAL_BLOCK.get())
                || player.isSpectator() || !level.mayInteract(player, pos)
                || (!player.mayBuild() && !stack.canPlaceOnBlockInAdventureMode(new BlockInWorld(level, pos, false)))) return;
        if (level instanceof ServerLevel server) {
            var converted = ModBlocks.CORRUPTED_ARCANE_CRYSTAL_BLOCK.get().defaultBlockState();
            if (!server.setBlockAndUpdate(pos, converted)) return;
            stack.consume(1, player);
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, converted), pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, 32, .4, .4, .4, .08);
            server.playSound(null, pos, ModSounds.MUNDABITUR_DUST_USE.get(), SoundSource.PLAYERS, 1, server.random.nextFloat() * .15F + .9F);
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
    }
}
