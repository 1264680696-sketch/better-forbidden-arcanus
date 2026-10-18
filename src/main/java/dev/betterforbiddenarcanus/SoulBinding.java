package dev.betterforbiddenarcanus;
import com.stal111.forbidden_arcanus.core.init.ModDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
public final class SoulBinding {
    private SoulBinding() {}
    /** Called on the retained stack at actual death respawn, preserving all other components. */
    public static ItemStack consume(ItemStack input) {
        var modifier=input.get(ModDataComponents.ITEM_MODIFIER);
        if(modifier==null||!modifier.is(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("forbidden_arcanus","soulbound")))return input;
        var output=input.copy();output.remove(ModDataComponents.ITEM_MODIFIER);
        var name=output.get(DataComponents.ITEM_NAME);
        if(name!=null&&name.getContents() instanceof TranslatableContents key&&key.getKey().equals("modifier.forbidden_arcanus.soulbound")&&name.getSiblings().size()>=2)
            output.set(DataComponents.ITEM_NAME,name.getSiblings().getLast().copy());
        return output;
    }
}
