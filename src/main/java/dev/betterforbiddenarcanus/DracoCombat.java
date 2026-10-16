package dev.betterforbiddenarcanus;
import com.stal111.forbidden_arcanus.common.entity.projectile.EnergyBall;
import com.stal111.forbidden_arcanus.core.init.ModItems;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
@EventBusSubscriber(modid = BetterForbiddenArcanus.MOD_ID)
public final class DracoCombat {
    public static final String SCEPTER_SHOT = "bfa_draco_scepter_shot";
    private DracoCombat() {}
    public static float weaponBonus(DamageSource source, Player player) {
        if (source instanceof ScepterStorm.StormDamageSource) return 1;
        if (source.getDirectEntity() instanceof Projectile projectile) return projectile.getPersistentData().getBoolean(SCEPTER_SHOT) ? 1 : 0;
        if (source.getDirectEntity() == player && source.is(DamageTypes.PLAYER_ATTACK)
                && (player.getMainHandItem().is(ModItems.DRACO_ARCANUS_SWORD.get()) || player.getMainHandItem().is(ModItems.DRACO_ARCANUS_SCEPTER.get()))) return 1;
        return 0;
    }
    @SubscribeEvent public static void spawn(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof EnergyBall ball && ball.getOwner() instanceof Player)
            ball.getPersistentData().putBoolean(SCEPTER_SHOT, true);
    }
    @SubscribeEvent public static void breath(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        // Scale damage after instant-effect integer rounding: 6 -> 7.5, rather than 6 -> 8.
        if (event.getSource().getDirectEntity() instanceof net.minecraft.world.entity.AreaEffectCloud cloud
                && cloud.getPersistentData().getBoolean("bfa_draco_breath")) event.setAmount(event.getAmount()*1.25F);
    }
}
