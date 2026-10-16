package dev.betterforbiddenarcanus;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(BetterForbiddenArcanus.MOD_ID)
public final class BetterForbiddenArcanus {
    public static final String MOD_ID = "better_forbidden_arcanus";

    public BetterForbiddenArcanus(IEventBus modBus) {
        dev.betterforbiddenarcanus.legacy.LegacyContent.register(modBus);
        SoulItems.ITEMS.register(modBus);
        ScepterStorm.ENTITIES.register(modBus);
        DracoSkullLoot.SERIALIZERS.register(modBus);
        SoulItems.TABS.register(modBus);
        ElytraDuplicationRecipe.SERIALIZERS.register(modBus);
        CorruptedObelisk.INTERACTIONS.register(modBus);
        ForgeUpgrades.COMPONENTS.register(modBus);
        ForgeUpgrades.INGREDIENTS.register(modBus);
        ForgeUpgrades.RESULTS.register(modBus);
        modBus.addListener(BatSoupComponents::modifyDefaults);
        modBus.addListener(DracoToolComponents::modifyDefaults);
        modBus.addListener(RelicCurio::setup);
        modBus.addListener(TyrCooldown::register);
        modBus.addListener(MeteorScan::register);
    }
}
