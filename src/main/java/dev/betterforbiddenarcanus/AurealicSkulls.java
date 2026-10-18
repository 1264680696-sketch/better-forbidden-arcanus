package dev.betterforbiddenarcanus;

import com.mojang.serialization.MapCodec;
import com.stal111.forbidden_arcanus.common.block.entity.forge.ritual.result.*;
import com.stal111.forbidden_arcanus.common.block.skull.ObsidianSkullType;
import com.stal111.forbidden_arcanus.core.init.ModBlocks;
import com.stal111.forbidden_arcanus.core.init.ModDataComponents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = BetterForbiddenArcanus.MOD_ID)
public final class AurealicSkulls {
    public static final net.neoforged.neoforge.registries.DeferredHolder<RitualResultType<?>, RitualResultType<ConsecrateResult>> RESULT =
            ForgeUpgrades.RESULTS.register("consecrate_skull", () -> new RitualResultType<>(ConsecrateResult.CODEC));
    private AurealicSkulls() {}
    public static boolean consecration(RitualResult result) {
        return result instanceof ConsecrateResult || result instanceof TransmuteInputResult transmute
                && transmute.result().is(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("forbidden_arcanus", "aurealic_obsidian_skull"));
    }
    public static void init() { /* Register the result before the deferred register joins the mod bus. */ }
    public static boolean wearing(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModBlocks.AUREALIC_OBSIDIAN_SKULL.getSkull().asItem());
    }
    public static boolean eternal(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModBlocks.ETERNAL_OBSIDIAN_SKULL.getSkull().asItem());
    }
    @SubscribeEvent
    public static void protect(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player && (wearing(player) || eternal(player))
                && event.getSource().is(DamageTypeTags.IS_FIRE)) event.setCanceled(true);
    }
    public record ConsecrateResult() implements RitualResult {
        public static final MapCodec<ConsecrateResult> CODEC = MapCodec.unit(new ConsecrateResult());
        @Override public ItemStack getResultItem(ItemStack input) {
            var result = new ItemStack(ModBlocks.AUREALIC_OBSIDIAN_SKULL.getSkull());
            result.applyComponents(input.getComponentsPatch());
            result.set(ModDataComponents.OBSIDIAN_SKULL_TYPE, ObsidianSkullType.AUREALIC);
            result.remove(ModDataComponents.TICKS_TILL_NEXT_STAGE);
            return result;
        }
        @Override public RitualResultType<?> getType() { return RESULT.get(); }
    }
}
