package dev.betterforbiddenarcanus;

import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = BetterForbiddenArcanus.MOD_ID)
public final class GaleMace {
    public static final String COOLDOWN = "bfa_gale_until";
    private GaleMace() {}
    public static boolean isGale(ItemStack stack) {
        if (!stack.is(Items.MACE)) return false;
        var modifier = stack.get(com.stal111.forbidden_arcanus.core.init.ModDataComponents.ITEM_MODIFIER);
        return modifier != null && modifier.is(SoulItems.id("gale"));
    }
    @SubscribeEvent public static void use(PlayerInteractEvent.RightClickBlock event) {
        var player = event.getEntity(); var level = event.getLevel(); var pos = event.getPos();
        if (!isGale(event.getItemStack()) || player.isSpectator() || event.getFace() != Direction.UP
                || level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                || !player.canInteractWithBlock(pos, 0) || !level.mayInteract(player, pos)) return;
        event.setCanceled(true);
        if (level instanceof ServerLevel server) {
            long now = TyrCombat.clock(player);
            if (now < player.getPersistentData().getLong(COOLDOWN)) { event.setCancellationResult(InteractionResult.FAIL); return; }
            player.getPersistentData().putLong(COOLDOWN, now + 20);
            var velocity = player.getDeltaMovement();
            player.setDeltaMovement(velocity.x, 1.2, velocity.z); player.hurtMarked = true;
            player.fallDistance = 0;
            player.currentImpulseImpactPos = player.position();
            player.currentExplosionCause = null;
            player.setIgnoreFallDamageFromCurrentImpulse(true);
            player.getCooldowns().addCooldown(Items.MACE, 20);
            if (player instanceof ServerPlayer remote) remote.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
            var point = event.getHitVec().getLocation();
            server.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, point.x, point.y + .1, point.z, 1, 0, 0, 0, 0);
            server.playSound(null, point.x, point.y, point.z, SoundEvents.WIND_CHARGE_BURST, SoundSource.PLAYERS, 1, 1);
        }
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
    }
}
