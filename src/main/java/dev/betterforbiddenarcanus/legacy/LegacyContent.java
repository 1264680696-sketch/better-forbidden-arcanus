package dev.betterforbiddenarcanus.legacy;

import dev.betterforbiddenarcanus.SoulItems;
import dev.betterforbiddenarcanus.BetterForbiddenArcanus;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

/** Restored official legacy content. IDs belong to the addon, never the dependency. */
public final class LegacyContent {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, BetterForbiddenArcanus.MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, BetterForbiddenArcanus.MOD_ID);
    public static final DeferredHolder<Block, LegacyCoreBlock> FRAME = BLOCKS.register("runic_tenebris_frame", () -> new LegacyCoreBlock(false));
    public static final DeferredHolder<Block, LegacyCoreBlock> CORE = BLOCKS.register("runic_tenebris_core", () -> new LegacyCoreBlock(true));
    public static final DeferredHolder<Block, LegacyRuneBlock> WARD = BLOCKS.register("runic_chiseled_polished_darkstone", LegacyRuneBlock::new);
    public static final DeferredHolder<Block, Block> DARK_ORE = BLOCKS.register("dark_runestone", () -> new Block(Block.Properties.ofFullCopy(Blocks.IRON_ORE)));
    public static final DeferredHolder<Item, Item> ZOMBIE_ARM = SoulItems.ITEMS.register("zombie_arm", ZombieArmItem::new);
    public static final DeferredHolder<Item, Item> SHINY_ARM = SoulItems.ITEMS.register("shiny_zombie_arm", ZombieArmItem::new);
    public static final DeferredHolder<Item, Item> AXE = SoulItems.ITEMS.register("runic_battleaxe", () -> new AxeItem(Tiers.DIAMOND,
            new Item.Properties().attributes(AxeItem.createAttributes(Tiers.DIAMOND, 1, -2.8F))));
    public static final DeferredHolder<Item, Item> DARK_RUNE = simple("dark_rune");
    public static final DeferredHolder<Item, Item> SKULL = simple("skull");
    public static final DeferredHolder<Item, Item> RUNE_BAG = simple("rune_bag");
    public static final DeferredHolder<Item, Item> DARK_BAG = simple("dark_rune_bag");
    public static final DeferredHolder<SoundEvent, SoundEvent> ACTIVATE = sound("runic_tenebris_core_activated");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMBIENT = sound("runic_tenebris_core_ambient");
    private static DeferredHolder<Item, Item> simple(String id) { return SoulItems.ITEMS.register(id, () -> new Item(new Item.Properties())); }
    private static DeferredHolder<SoundEvent, SoundEvent> sound(String id) { return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(SoulItems.id(id))); }
    public static void register(IEventBus bus) {
        for (var block : BLOCKS.getEntries()) SoulItems.ITEMS.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties()));
        BLOCKS.register(bus); SOUNDS.register(bus);
    }
    private LegacyContent() {}
}
