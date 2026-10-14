package dev.betterforbiddenarcanus;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.stal111.forbidden_arcanus.core.init.ModItems;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.*;
import net.neoforged.neoforge.registries.*;

public final class DracoSkullLoot extends LootModifier {
    public static final MapCodec<DracoSkullLoot> CODEC=RecordCodecBuilder.mapCodec(instance->codecStart(instance).apply(instance,DracoSkullLoot::new));
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> SERIALIZERS=DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,BetterForbiddenArcanus.MOD_ID);
    static {SERIALIZERS.register("draco_skull",()->CODEC);}
    public DracoSkullLoot(LootItemCondition[] conditions){super(conditions);}
    @Override public MapCodec<? extends IGlobalLootModifier> codec(){return CODEC;}
    @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot,LootContext context) {
        var victim=context.getParamOrNull(LootContextParams.THIS_ENTITY);var source=context.getParamOrNull(LootContextParams.DAMAGE_SOURCE);
        if(victim==null||victim.getType()!=EntityType.WITHER_SKELETON||source==null||!(source.getEntity() instanceof Player p)
                ||source.getDirectEntity()!=p||!p.getMainHandItem().is(ModItems.DRACO_ARCANUS_AXE.get())||loot.stream().anyMatch(s->s.is(Items.WITHER_SKELETON_SKULL)))return loot;
        int looting=EnchantmentHelper.getEnchantmentLevel(context.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING),p);
        double nativeChance=Math.min(1,.025+.01*looting);
        // Conditional on the native roll failing: total probability is native + .30.
        double extraChance=nativeChance>=1?0:Math.min(1,.30/(1-nativeChance));
        if(context.getRandom().nextFloat()<extraChance)loot.add(new ItemStack(Items.WITHER_SKELETON_SKULL));
        return loot;
    }
}
