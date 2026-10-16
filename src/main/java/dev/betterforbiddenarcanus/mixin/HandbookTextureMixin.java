package dev.betterforbiddenarcanus.mixin;

import dev.betterforbiddenarcanus.SoulItems;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.common.book.Book;

/** Reuses the original atlas without editing it or changing other Patchouli books. */
@Mixin(value = GuiBook.class, remap = false)
public abstract class HandbookTextureMixin {
    @Inject(method = "drawFromTexture", at = @At("HEAD"), cancellable = true)
    private static void bfa$legacyPages(GuiGraphics graphics, Book book, int x, int y,
                                        int u, int v, int width, int height, CallbackInfo ci) {
        if (!book.id.equals(SoulItems.id("forbidden_arcanus"))
                || !book.bookTexture.equals(SoulItems.id("textures/gui/forbiddenmicon.png"))) return;
        if (u == 0 && v == 0 && width == GuiBook.FULL_WIDTH && height == GuiBook.FULL_HEIGHT) {
            graphics.blit(book.bookTexture, x, y, width, height, 0, 0, 264, 182, 512, 256);
        } else {
            // Navigation and recipe widgets still need Patchouli's own UV layout.
            graphics.blit(SoulItems.id("textures/gui/guide_book.png"), x, y, u, v, width, height, 512, 256);
        }
        ci.cancel();
    }
}
