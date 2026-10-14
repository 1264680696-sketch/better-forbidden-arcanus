package dev.betterforbiddenarcanus.mixin;
import com.stal111.forbidden_arcanus.common.block.entity.forge.essence.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value = EssenceManager.class, remap = false)
public interface EssenceCapacityAccessor {
    @Accessor("maxEssences") EssencesDefinition bfa$capacity();
}
