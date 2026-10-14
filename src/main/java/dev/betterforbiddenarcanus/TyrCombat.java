package dev.betterforbiddenarcanus;

import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Damage bonuses are applied once to attributed damage, never to attack damage attributes. */
@EventBusSubscriber(modid = BetterForbiddenArcanus.MOD_ID)
public final class TyrCombat {
    public static final String COOLDOWN = "bfa_tyr_avatar_until", COUNTER = "bfa_tyr_counter";
    private record Nearby(long tick, int enemies) {}
    private record Strike(Player attacker, float health) {}
    private static final Map<Player, Nearby> NEARBY = new WeakHashMap<>();
    private static final Map<LivingEntity, Deque<Strike>> STRIKES = new WeakHashMap<>();
    private static final Set<Player> RESERVED = Collections.newSetFromMap(new WeakHashMap<>());
    private TyrCombat() {}

    public static long clock(Player player) { return player.level().getServer().overworld().getGameTime(); }
    public static boolean enemy(Player player, LivingEntity other) {
        return other.isAlive() && other != player && !(other instanceof Player) && !player.isAlliedTo(other)
                && !(other instanceof TamableAnimal pet && pet.isTame())
                && (other instanceof Enemy || other instanceof Mob mob && mob.getTarget() == player);
    }
    public static int enemies(Player player, boolean force) {
        long tick = player.level().getGameTime(); var cached = NEARBY.get(player);
        if (!force && cached != null && tick >= cached.tick && tick - cached.tick < 5) return cached.enemies;
        int count = player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(16),
                entity -> player.distanceToSqr(entity) <= 256 && enemy(player, entity)).size();
        NEARBY.put(player, new Nearby(tick, count)); return count;
    }
    public static long harmfulEffects(Player player) {
        return player.getActiveEffects().stream().filter(effect -> effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL).count();
    }
    public static double valor(Player player) {
        return TyrArmor.count(player) == 0 ? 1 : (1 + Math.min(.5, harmfulEffects(player) * .1)) * (1 + Math.min(.5, enemies(player, false) * .05));
    }
    public static double justice(Player player, LivingEntity target) {
        return TyrArmor.count(player) < 2 ? 1 : 1 + Math.clamp(target.getMaxHealth() / Math.max(.001, player.getMaxHealth()) - 1, -.5, .5);
    }
    private static void modifier(Player player, Holder<Attribute> type, String name, double amount) {
        var attribute = player.getAttribute(type); if (attribute == null) return;
        var id = SoulItems.id(name); var previous = attribute.getModifier(id);
        if (amount == 0) { attribute.removeModifier(id); return; }
        if (previous == null || Math.abs(previous.amount() - amount) > .000001) {
            attribute.removeModifier(id);
            attribute.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }
    private static void attributes(Player player, long effects) {
        int pieces = TyrArmor.count(player);
        double status = pieces > 0 ? Math.min(.5, Math.max(0, effects) * .1) : 0;
        double nearby = pieces > 0 ? Math.min(.5, enemies(player, false) * .05) : 0;
        for (var type : List.of(Attributes.ATTACK_SPEED, Attributes.MOVEMENT_SPEED, Attributes.ARMOR, Attributes.ARMOR_TOUGHNESS)) {
            modifier(player, type, "tyr_valor_status", status);
            modifier(player, type, "tyr_valor_enemies", nearby);
        }
    }
    public static void update(Player player) {
        if (player.level().isClientSide()) return;
        attributes(player, harmfulEffects(player));
        if (TyrArmor.count(player) != 4 || !player.isAlive()) player.getPersistentData().remove(COUNTER);
        else {
            if (player.isUsingItem() && shield(player.getUseItem())) player.stopUsingItem();
            stow(player, EquipmentSlot.MAINHAND); stow(player, EquipmentSlot.OFFHAND);
        }
    }
    public static boolean shield(ItemStack stack) { return stack.canPerformAction(ItemAbilities.SHIELD_BLOCK); }
    private static void stow(Player player, EquipmentSlot hand) {
        var shield = player.getItemBySlot(hand); if (!shield(shield)) return;
        player.setItemSlot(hand, ItemStack.EMPTY);
        var inventory = player.getInventory();
        // Never put the removed shield back into the selected hotbar slot.
        for (int i = 0; i < inventory.items.size() && !shield.isEmpty(); i++) {
            if (i == inventory.selected) continue;
            var destination = inventory.items.get(i);
            if (destination.isEmpty()) { inventory.items.set(i, shield); shield = ItemStack.EMPTY; }
            else if (ItemStack.isSameItemSameComponents(destination, shield)) {
                int move = Math.min(shield.getCount(), destination.getMaxStackSize() - destination.getCount());
                if (move > 0) { destination.grow(move); shield.shrink(move); }
            }
        }
        if (!shield.isEmpty()) player.drop(shield, false);
        inventory.setChanged(); player.inventoryMenu.broadcastChanges();
    }
    @SubscribeEvent public static void equipment(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof Player player) { NEARBY.remove(player); update(player); }
    }
    @SubscribeEvent public static void effectAdded(MobEffectEvent.Added event) {
        if (event.getEntity() instanceof Player p && !p.level().isClientSide()) {
            var type = event.getEffectInstance().getEffect();
            attributes(p, harmfulEffects(p) + (type.value().getCategory() == MobEffectCategory.HARMFUL && !p.hasEffect(type) ? 1 : 0));
        }
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void effectRemoved(MobEffectEvent.Remove event) {
        if (event.getEntity() instanceof Player p && !p.level().isClientSide())
            attributes(p, harmfulEffects(p) - (p.hasEffect(event.getEffect()) && event.getEffect().value().getCategory() == MobEffectCategory.HARMFUL ? 1 : 0));
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void effectExpired(MobEffectEvent.Expired event) {
        if (event.getEntity() instanceof Player p && !p.level().isClientSide())
            attributes(p, harmfulEffects(p) - (event.getEffectInstance().getEffect().value().getCategory() == MobEffectCategory.HARMFUL ? 1 : 0));
    }
    private static Player attacker(DamageSource source, LivingEntity target) {
        return source.getEntity() instanceof Player p && p != target
                && (source.getDirectEntity() == p || source.getDirectEntity() instanceof Projectile) ? p : null;
    }
    @SubscribeEvent(priority = EventPriority.LOW) public static void outgoing(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        var player = attacker(event.getSource(), event.getEntity());
        if (player != null) event.setAmount((float)(event.getAmount() * valor(player) * justice(player, event.getEntity())));
    }
    @SubscribeEvent(priority = EventPriority.HIGH) public static void reserveCounter(LivingDamageEvent.Pre event) {
        if (event.getEntity().level().isClientSide()) return;
        var p = attacker(event.getSource(), event.getEntity());
        boolean counter = p != null && TyrArmor.count(p) == 4 && p.getPersistentData().getBoolean(COUNTER)
                && event.getNewDamage() > 0 && RESERVED.add(p);
        STRIKES.computeIfAbsent(event.getEntity(), ignored -> new ArrayDeque<>()).push(new Strike(counter ? p : null, event.getEntity().getHealth()));
        if (counter) event.setNewDamage(event.getNewDamage() * 5);
    }
    @SubscribeEvent(priority = EventPriority.LOW) public static void projectileProtection(LivingDamageEvent.Pre event) {
        if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof Player p && TyrArmor.count(p) == 4
                && event.getSource().is(DamageTypeTags.IS_PROJECTILE)) event.setNewDamage(event.getNewDamage() * .4F);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void avertDeath(LivingDamageEvent.Pre event) {
        if (event.getEntity().level().isClientSide() || !(event.getEntity() instanceof Player p) || TyrArmor.count(p) != 4
                || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY) || clock(p) < p.getPersistentData().getLong(COOLDOWN)) return;
        float damage = event.getNewDamage(), absorption = p.getAbsorptionAmount();
        if (!Float.isFinite(damage) || damage <= 0 || damage - absorption < p.getHealth()) return;
        event.setNewDamage(absorption + Math.max(0, p.getHealth() - 1));
        if (p.getHealth() < 1) p.setHealth(1);
        p.getPersistentData().putLong(COOLDOWN, clock(p) + 2400);
        p.getPersistentData().putBoolean(COUNTER, true);
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), net.minecraft.sounds.SoundEvents.TOTEM_USE,
                net.minecraft.sounds.SoundSource.PLAYERS, 1, 1);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void confirmCounter(LivingDamageEvent.Post event) {
        var stack = STRIKES.get(event.getEntity()); if (stack == null || stack.isEmpty()) return;
        var strike = stack.pop(); if (stack.isEmpty()) STRIKES.remove(event.getEntity());
        if (strike.attacker != null) {
            if (event.getNewDamage() > 0 && event.getEntity().getHealth() < strike.health) strike.attacker.getPersistentData().remove(COUNTER);
            RESERVED.remove(strike.attacker);
        }
    }
    @SubscribeEvent public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player p) p.getPersistentData().remove(COUNTER);
    }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event) {
        var old = event.getOriginal().getPersistentData(); var data = event.getEntity().getPersistentData();
        if (old.contains(COOLDOWN)) data.putLong(COOLDOWN, old.getLong(COOLDOWN));
        if (!event.isWasDeath() && old.getBoolean(COUNTER)) data.putBoolean(COUNTER, true); else data.remove(COUNTER);
    }
    @SubscribeEvent public static void shieldStart(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof Player p && TyrArmor.count(p) == 4 && shield(event.getItem())) event.setCanceled(true);
    }
    @SubscribeEvent public static void shieldTick(LivingEntityUseItemEvent.Tick event) {
        if (event.getEntity() instanceof Player p && TyrArmor.count(p) == 4 && shield(event.getItem())) { p.stopUsingItem(); event.setCanceled(true); }
    }
    @SubscribeEvent public static void shieldBlock(LivingShieldBlockEvent event) {
        if (event.getEntity() instanceof Player p && TyrArmor.count(p) == 4) event.setBlocked(false);
    }
}
