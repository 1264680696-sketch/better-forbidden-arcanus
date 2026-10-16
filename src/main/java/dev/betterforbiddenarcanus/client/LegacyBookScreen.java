package dev.betterforbiddenarcanus.client;

import com.google.gson.*;
import dev.betterforbiddenarcanus.SoulItems;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import org.lwjgl.glfw.GLFW;

/** The official legacy atlas/layout, ported to modern GUI and recipe APIs. Not a Patchouli screen. */
public final class LegacyBookScreen extends Screen {
    private static final ResourceLocation TEXTURE = SoulItems.id("textures/gui/forbiddenmicon.png");
    public record Entry(int category, String title, String item, String text) {}
    private record Control(int x, int y, int width, String text, Runnable action) {}
    private final List<Entry> entries = new ArrayList<>();
    private final List<Control> controls = new ArrayList<>();
    private List<Entry> visible = List.of();
    private List<RecipeHolder<?>> recipes = List.of();
    private List<FormattedCharSequence> lines = List.of();
    private int category, index, recipe, textPage;
    private float scale = 1;
    private int left, top;
    private ItemStack hovered = ItemStack.EMPTY;
    public LegacyBookScreen() { super(Component.literal("禁忌之书")); }
    @Override public boolean isPauseScreen() { return false; }
    public List<Entry> entries() { return List.copyOf(entries); }
    public int textPages() { return Math.max(1, (lines.size()+11)/12); }
    public int recipeCount() { return recipes.size(); }
    public void selectCategory(int value) { category = Math.clamp(value, 0, 2); index = 0; refresh(); }
    public void selectEntry(int value) { index = Math.clamp(value, 0, Math.max(0, visible.size()-1)); refresh(); }
    public void selectRecipe(int value) { recipe = recipes.isEmpty() ? 0 : Math.floorMod(value, recipes.size()); }
    public Entry currentEntry() { return visible.isEmpty() ? null : visible.get(index); }
    @Override protected void init() {
        scale = Math.min(1.35F, Math.min(width/340F, height/236F));
        left = (int)((width-340*scale)/2); top = (int)((height-236*scale)/2);
        if (entries.isEmpty()) {
            load();
            // The original book has no MAIN entries. Open its first populated category.
            if (!entries.isEmpty()) category = entries.getFirst().category();
        }
        refresh();
    }
    private void load() {
        try (var reader = minecraft.getResourceManager().openAsReader(SoulItems.id("legacy_book/zh_cn.json"))) {
            for (var element : JsonParser.parseReader(reader).getAsJsonArray()) {
                var object = element.getAsJsonObject();
                entries.add(new Entry(object.get("category").getAsInt(), object.has("title") ? object.get("title").getAsString() : "",
                        object.get("item").getAsString(), object.get("text").getAsString()));
            }
        } catch (Exception error) { throw new IllegalStateException("Cannot load legacy Forbiddenmicon", error); }
    }
    private ItemStack icon(Entry entry) {
        var id = ResourceLocation.tryParse(entry.item());
        return new ItemStack(id != null && BuiltInRegistries.ITEM.containsKey(id) ? BuiltInRegistries.ITEM.get(id) : Items.BOOK);
    }
    private void refresh() {
        visible = entries.stream().filter(entry -> entry.category() == category).toList();
        index = Math.clamp(index, 0, Math.max(0, visible.size()-1)); recipe = textPage = 0;
        var entry = currentEntry();
        lines = entry == null ? List.of() : font.split(Component.literal(entry.text()), 130);
        if (minecraft.level != null && entry != null) {
            var id = ResourceLocation.tryParse(entry.item());
            var item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
            recipes = minecraft.level.getRecipeManager().getRecipes().stream()
                    .filter(holder -> holder.value() instanceof CraftingRecipe || holder.value() instanceof AbstractCookingRecipe)
                    .filter(holder -> item != null && holder.value().getResultItem(minecraft.level.registryAccess()).is(item))
                    .sorted(Comparator.comparing(holder -> holder.id().toString())).toList();
        } else recipes = List.of();
        controls.clear();
        String[] categories = {"主页", "物品", "方块"};
        for (int i = 0; i < 3; i++) { final int c = i; controls.add(new Control(0, 45+i*24, 35, categories[i], () -> selectCategory(c))); }
        controls.add(new Control(49, 196, 28, "◀", () -> selectEntry(index-1)));
        controls.add(new Control(267, 196, 28, "▶", () -> selectEntry(index+1)));
        controls.add(new Control(145, 218, 70, "完成", this::onClose));
        controls.add(new Control(51, 172, 38, "配方‹", () -> selectRecipe(recipe-1)));
        controls.add(new Control(126, 172, 38, "配方›", () -> selectRecipe(recipe+1)));
        controls.add(new Control(186, 177, 35, "正文‹", () -> textPage = Math.max(0, textPage-1)));
        controls.add(new Control(264, 177, 35, "正文›", () -> textPage = Math.min(textPages()-1, textPage+1)));
    }
    private boolean inside(Control c, double x, double y) { return x>=c.x && y>=c.y && x<c.x+c.width && y<c.y+17; }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (button == 0) for (var c : List.copyOf(controls)) if (inside(c, (x-left)/scale, (y-top)/scale)) { c.action.run(); return true; }
        return super.mouseClicked(x, y, button);
    }
    @Override public boolean mouseScrolled(double x, double y, double dx, double dy) {
        textPage = Math.clamp(textPage-(int)Math.signum(dy), 0, textPages()-1); return true;
    }
    @Override public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_LEFT) { selectEntry(index-1); return true; }
        if (key == GLFW.GLFW_KEY_RIGHT) { selectEntry(index+1); return true; }
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_3) { selectCategory(key-GLFW.GLFW_KEY_1); return true; }
        return super.keyPressed(key, scan, mods);
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        // Modern Screen.render draws the blurred background itself. It must run before the book.
        super.render(g, mouseX, mouseY, partial); hovered = ItemStack.EMPTY;
        double mx = (mouseX-left)/scale, my = (mouseY-top)/scale;
        g.pose().pushPose(); g.pose().translate(left, top, 0); g.pose().scale(scale, scale, 1);
        g.blit(TEXTURE, 38, 15, 0, 0, 264, 186, 512, 256);
        var entry = currentEntry();
        if (entry != null) {
            var stack = icon(entry);
            var title = entry.title().isEmpty() ? stack.getHoverName() : Component.literal(entry.title());
            g.blit(TEXTURE, 50, 25, 272, 7, 116, 18, 512, 256);
            g.pose().pushPose(); g.pose().translate(108, 30, 0); g.pose().scale(.75F, .75F, 1);
            g.drawCenteredString(font, title, 0, 0, 0xfff5e9cf); g.pose().popPose();
            g.blit(TEXTURE, 52, 49, 274, 28, 113, 65, 512, 256);
            g.pose().pushPose(); g.pose().translate(92, 64, 0); g.pose().scale(2, 2, 1); g.renderItem(stack, 0, 0); g.pose().popPose();
            if (mx>=92 && mx<124 && my>=64 && my<96) hovered = stack;
            g.blit(TEXTURE, 181, 45, 394, 8, 108, 131, 512, 256);
            g.pose().pushPose(); g.pose().translate(186, 49, 0); g.pose().scale(.75F, .75F, 1);
            for (int i = textPage*12; i < Math.min(lines.size(), (textPage+1)*12); i++) g.drawString(font, lines.get(i), 0, (i%12)*13, 0xff2f2921, false);
            g.pose().popPose();
            g.drawCenteredString(font, (textPage+1)+"/"+textPages(), 242, 183, 0xffe4ce9b);
            renderRecipe(g, mx, my);
            g.drawCenteredString(font, (index+1)+" / "+visible.size(), 173, 201, 0xffefe3c5);
        }
        for (var control : controls) {
            boolean over = inside(control, mx, my);
            g.fill(control.x, control.y, control.x+control.width, control.y+17, over ? 0xee9d813d : 0xdd4a4031);
            g.drawCenteredString(font, control.text, control.x+control.width/2, control.y+5, 0xfff4e4bf);
        }
        g.pose().popPose();
        if (!hovered.isEmpty()) g.renderTooltip(font, hovered, mouseX, mouseY);
    }
    private void renderRecipe(GuiGraphics g, double mx, double my) {
        if (recipes.isEmpty()) { g.drawCenteredString(font, "详见右页获取说明", 109, 143, 0xff433c30); return; }
        var holder = recipes.get(recipe); var value = holder.value(); var ingredients = value.getIngredients();
        int cycle = (int)(System.currentTimeMillis()/1000);
        for (int i=0; i<ingredients.size(); i++) {
            int x = value instanceof ShapedRecipe shaped ? i%shaped.getWidth() : i%3;
            int y = value instanceof ShapedRecipe shaped ? i/shaped.getWidth() : i/3;
            if (y>=3) continue;
            int px = 54+x*17, py = 118+y*17;
            g.fill(px-1, py-1, px+16, py+16, 0xff766b56);
            var choices = ingredients.get(i).getItems();
            if (choices.length>0) { var ingredient = choices[Math.floorMod(cycle, choices.length)]; g.renderItem(ingredient, px, py);
                if (mx>=px && mx<px+16 && my>=py && my<py+16) hovered = ingredient; }
        }
        var result = value.getResultItem(minecraft.level.registryAccess());
        g.drawString(font, "→", 110, 139, 0xff433c30, false); g.renderItem(result, 135, 135); g.renderItemDecorations(font, result, 135, 135);
        if (mx>=135 && mx<151 && my>=135 && my<151) hovered = result;
        g.pose().pushPose(); g.pose().translate(109, 109, 0); g.pose().scale(.65F, .65F, 1);
        g.drawCenteredString(font, (value instanceof AbstractCookingRecipe ? "烧炼" : "工作台")+" "+(recipe+1)+"/"+recipes.size(), 0, 0, 0xff433c30); g.pose().popPose();
    }
}
