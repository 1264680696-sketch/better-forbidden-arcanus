package dev.betterforbiddenarcanus;

import com.stal111.forbidden_arcanus.common.essence.EssenceHelper;
import com.stal111.forbidden_arcanus.common.block.entity.forge.essence.EssenceType;
import com.stal111.forbidden_arcanus.core.init.ModItems;
import com.stal111.forbidden_arcanus.util.ModTags;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = BetterForbiddenArcanus.MOD_ID)
public final class RelicEffects {
    private static final Map<LivingEntity, Float> HEALTH_BEFORE_DAMAGE = new WeakHashMap<>();
    private RelicEffects() {}

    public static boolean isNight(Player player) {
        long time = Math.floorMod(player.level().getDayTime(), 24000);
        return player.level().dimensionType().hasSkyLight() && time >= 13000 && time < 23000;
    }

    public static boolean hasCrimson(Player player) {
        return RelicCurio.equipped(player, ModItems.CRIMSON_STONE.get());
    }

    @SubscribeEvent
    public static void beforePlayerTick(PlayerTickEvent.Pre event) {
        updateAttributes(event.getEntity());
    }

    public static void updateAttributes(Player player) {
        TyrArmor.update(player);
        // Remove the previous version's own modifier IDs if a player is already loaded.
        modifier(player, Attributes.ATTACK_DAMAGE, "artisan_attack", false, 0);
        modifier(player, Attributes.ATTACK_SPEED, "artisan_attack_speed", false, 0);
        modifier(player, Attributes.ARMOR, "artisan_armor", false, 0);
        modifier(player, Attributes.ARMOR_TOUGHNESS, "artisan_toughness", false, 0);
        modifier(player, Attributes.KNOCKBACK_RESISTANCE, "artisan_knockback", false, 0);
    }

    private static void modifier(Player player, Holder<Attribute> type, String name, boolean active, double amount) {
        var attribute = player.getAttribute(type);
        if (attribute == null) return;
        var id = SoulItems.id(name);
        if (active && attribute.getModifier(id) == null) {
            attribute.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else if (!active) attribute.removeModifier(id);
    }

    @SubscribeEvent
    public static void afterPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
        boolean divine = RelicCurio.equipped(player, ModItems.DIVINE_PACT.get());
        boolean moon = RelicCurio.equipped(player, ModItems.CRESCENT_MOON.get()) && isNight(player);
        effect(player, MobEffects.REGENERATION, "relic_regeneration", divine && player.getHealth() < player.getMaxHealth() * 0.2F, 60);
        boolean skull = AurealicSkulls.wearing(player);
        effect(player, MobEffects.NIGHT_VISION, "relic_night_vision", moon || skull, 240);
        effect(player, MobEffects.MOVEMENT_SPEED, "relic_speed", moon, 60);
        effect(player, MobEffects.JUMP, "relic_jump", moon, 60);
        if ((moon || skull) && player.level().getGameTime() % 20 == 0) {
            EssenceHelper.getEssenceProvider(player).ifPresent(provider -> provider.updateAmount(EssenceType.AUREAL, amount -> amount + (moon ? 2 : 0) + (skull ? 2 : 0)));
        }
    }

    private static void effect(Player player, Holder<MobEffect> effect, String name, boolean active, int duration) {
        var data = player.getPersistentData();
        String key = BetterForbiddenArcanus.MOD_ID + ":" + name;
        var current = player.getEffect(effect);
        boolean ours = data.getBoolean(key) && current != null && current.getAmplifier() == 0
                && current.getDuration() <= duration && current.isAmbient() && !current.isVisible();
        if (!active) {
            if (ours) player.removeEffect(effect);
            data.remove(key);
        } else if (current == null || ours) {
            // Longer or stronger potion effects retain their own lifetime and source.
            if (current == null || current.getDuration() <= duration - 20) {
                player.addEffect(new MobEffectInstance(effect, duration, 0, true, false, true));
            }
            data.putBoolean(key, true);
        } else data.remove(key);
    }

    @SubscribeEvent
    public static void incomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player) || player == event.getEntity() || player.level().isClientSide()) return;
        float multiplier = 1;
        if (RelicCurio.equipped(player, ModItems.MALEDICTUS_PACT.get())) multiplier *= 1.4F;
        if (event.getEntity().getType().is(EntityTypeTags.UNDEAD) && RelicCurio.equipped(player, ModItems.DIVINE_PACT.get())) multiplier *= 1.25F;
        event.setAmount(event.getAmount() * multiplier);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void beforeDamage(LivingDamageEvent.Pre event) {
        if (event.getSource().getEntity() instanceof Player player && player != event.getEntity() && hasCrimson(player)) {
            HEALTH_BEFORE_DAMAGE.put(event.getEntity(), event.getEntity().getHealth());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void afterDamage(LivingDamageEvent.Post event) {
        Float health = HEALTH_BEFORE_DAMAGE.remove(event.getEntity());
        if (!(event.getSource().getEntity() instanceof Player player) || player == event.getEntity() || player.level().isClientSide() || !hasCrimson(player)) return;
        if (health != null && event.getNewDamage() > 0) player.heal(Math.min(health, event.getNewDamage()) * 0.1F);
        // Upstream's NORMAL-priority listener already generated the base blood.
        // Add half of that generation, not of subsequent transfers of stored blood.
        if (event.getSource().is(DamageTypes.PLAYER_ATTACK) && !event.getEntity().getType().is(ModTags.EntityTypes.TEST_TUBE_BLACKLISTED)
                && player.getOffhandItem().is(ModItems.BLOOD_TEST_TUBE.get())) {
            var tube = player.getOffhandItem();
            int base = (int) (20 * event.getNewDamage());
            EssenceHelper.getEssenceStorage(tube).ifPresent(storage -> storage.addEssence(tube, base / 2));
        }
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void tooltip(ItemTooltipEvent event) {
        Item item = event.getItemStack().getItem();
        if (!RelicCurio.items().contains(item)) return;
        var lines = event.getToolTip();
        for (int i = 0; i < lines.size(); i++) {
            if (containsEnhancerLabel(lines.get(i))) { lines.subList(i + 1, lines.size()).clear(); break; }
        }
    }
    private static boolean containsEnhancerLabel(Component line) {
        if (line.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text
                && text.getKey().equals("item.forbidden_arcanus.enhancer")) return true;
        return line.getSiblings().stream().anyMatch(RelicEffects::containsEnhancerLabel);
    }
}
