package dev.betterforbiddenarcanus;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

/** Changes the existing soup's default food component; does not replace its item. */
final class BatSoupComponents {
    private static final ResourceLocation SOUP_ID = ResourceLocation.fromNamespaceAndPath("forbidden_arcanus", "bat_soup");

    private BatSoupComponents() {}

    static void modifyDefaults(ModifyDefaultComponentsEvent event) {
        Item soup = BuiltInRegistries.ITEM.getOptional(SOUP_ID)
                .orElseThrow(() -> new IllegalStateException("Required item missing: " + SOUP_ID));
        FoodProperties original = soup.components().get(DataComponents.FOOD);
        if (original == null) {
            throw new IllegalStateException("Required bat soup has no food component");
        }
        List<FoodProperties.PossibleEffect> effects = List.of(
                new FoodProperties.PossibleEffect(() -> new MobEffectInstance(MobEffects.NIGHT_VISION, 1800, 0), 1.0F),
                new FoodProperties.PossibleEffect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 0), 1.0F),
                new FoodProperties.PossibleEffect(() -> new MobEffectInstance(MobEffects.GLOWING, 1800, 0), 1.0F));
        FoodProperties improved = new FoodProperties(original.nutrition(), original.saturation(),
                original.canAlwaysEat(), original.eatSeconds(), original.usingConvertsTo(), effects);
        event.modify(soup, patch -> patch.set(DataComponents.FOOD, improved));
    }
}
