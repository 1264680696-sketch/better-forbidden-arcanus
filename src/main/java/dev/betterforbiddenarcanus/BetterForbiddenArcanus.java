package dev.betterforbiddenarcanus;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(BetterForbiddenArcanus.MOD_ID)
public final class BetterForbiddenArcanus {
    public static final String MOD_ID = "better_forbidden_arcanus";

    public BetterForbiddenArcanus(IEventBus modBus) {
        SoulItems.ITEMS.register(modBus);
        SoulItems.TABS.register(modBus);
        ElytraDuplicationRecipe.SERIALIZERS.register(modBus);
        modBus.addListener(BatSoupComponents::modifyDefaults);
        modBus.addListener(RelicCurio::setup);
    }
}
