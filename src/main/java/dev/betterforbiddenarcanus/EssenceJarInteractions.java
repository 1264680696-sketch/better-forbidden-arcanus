package dev.betterforbiddenarcanus;

import com.stal111.forbidden_arcanus.common.block.entity.EssenceUtremJarBlockEntity;
import com.stal111.forbidden_arcanus.common.block.entity.forge.essence.EssenceType;
import com.stal111.forbidden_arcanus.common.block.properties.ModBlockStateProperties;
import com.stal111.forbidden_arcanus.common.essence.EssenceHelper;
import com.stal111.forbidden_arcanus.common.item.BloodTestTubeItem;
import com.stal111.forbidden_arcanus.core.init.ModItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = BetterForbiddenArcanus.MOD_ID)
public final class EssenceJarInteractions {
    private EssenceJarInteractions() {}

    public static int aurealAmount() {
        return EssenceHelper.getEssenceAmount(new ItemStack(ModItems.AUREAL_BOTTLE.get()), EssenceType.AUREAL);
    }
    public static int experienceAmount() {
        return EssenceHelper.getEssenceAmount(new ItemStack(Items.EXPERIENCE_BOTTLE), EssenceType.EXPERIENCE);
    }
    public static boolean hasSoul(EssenceUtremJarBlockEntity jar) {
        return jar.getBlockState().getValue(ModBlockStateProperties.ESSENCE_TYPE) == EssenceType.SOULS
                && jar.getAmount() >= 1;
    }

    @SubscribeEvent
    public static void fill(PlayerInteractEvent.RightClickBlock event) {
        var player = event.getEntity();
        var level = event.getLevel();
        if (player.isSpectator() || !level.mayInteract(player, event.getPos())
                || !(level.getBlockEntity(event.getPos()) instanceof EssenceUtremJarBlockEntity jar)) return;
        var stack = event.getItemStack();
        var type = jar.getBlockState().getValue(ModBlockStateProperties.ESSENCE_TYPE);
        ItemStack filled;
        int cost;
        if (stack.is(Items.GLASS_BOTTLE) && (type == EssenceType.AUREAL || type == EssenceType.EXPERIENCE)) {
            filled = new ItemStack(type == EssenceType.AUREAL ? ModItems.AUREAL_BOTTLE.get() : Items.EXPERIENCE_BOTTLE);
            cost = type == EssenceType.AUREAL ? aurealAmount() : experienceAmount();
        } else if (type == EssenceType.BLOOD && (stack.is(ModItems.TEST_TUBE.get()) || stack.is(ModItems.BLOOD_TEST_TUBE.get()))) {
            // Sneak with a filled tube retains the dependency's existing deposit operation.
            if (stack.is(ModItems.BLOOD_TEST_TUBE.get()) && player.isShiftKeyDown()) return;
            var storage = EssenceHelper.getEssenceStorage(stack).orElse(BloodTestTubeItem.DEFAULT_DATA);
            int amount = stack.is(ModItems.TEST_TUBE.get()) ? 0 : storage.value().amount();
            int capacity = Math.min(storage.limit(), BloodTestTubeItem.MAX_BLOOD);
            cost = Math.min(Math.max(0, capacity - amount), Math.max(0, jar.getAmount()));
            if (stack.is(ModItems.BLOOD_TEST_TUBE.get())) {
                filled = stack.copyWithCount(1);
                storage.addEssence(filled, cost);
            } else {
                filled = EssenceHelper.createStorageItem(ModItems.BLOOD_TEST_TUBE.get(), EssenceType.BLOOD, cost, capacity);
            }
        } else return;

        event.setCanceled(true);
        if (cost <= 0 || jar.getAmount() < cost) {
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }
        if (!level.isClientSide()) {
            jar.addEssence(-cost);
            RelicJars.sync(jar);
            // Empty stacks split correctly, inventory overflow drops the result, and creative
            // handling follows the same conventions as filling a vanilla glass bottle.
            player.setItemInHand(event.getHand(), ItemUtils.createFilledResult(stack, player, filled));
            level.playSound(null, event.getPos(), SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1, 1);
        }
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
    }
}
