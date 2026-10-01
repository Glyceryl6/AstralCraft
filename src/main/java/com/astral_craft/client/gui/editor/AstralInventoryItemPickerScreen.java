package com.astral_craft.client.gui.editor;

import com.astral_craft.client.gui.components.AstralFancyButton;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AstralInventoryItemPickerScreen extends Screen {

    private static final int COLUMNS = 9;
    private static final int SLOT_SIZE = 22;

    private final Screen parent;
    private final Consumer<ItemStack> selected;
    private List<ItemStack> stacks = List.of();

    public AstralInventoryItemPickerScreen(Screen parent, Consumer<ItemStack> selected) {
        super(Component.translatable("gui.astral_craft.creator.inventory_picker.title"));
        this.parent = parent;
        this.selected = selected;
    }

    @Override
    protected void init() {
        if (this.minecraft.player == null) {
            this.stacks = List.of();
            return;
        }
        List<ItemStack> values = new ArrayList<>();
        for (ItemStack stack : this.minecraft.player.getInventory().getNonEquipmentItems()) {
            if (!stack.isEmpty()) values.add(stack.copy());
        }
        this.stacks = List.copyOf(values);
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int rows = Math.max(1, (this.stacks.size() + COLUMNS - 1) / COLUMNS);
        int panelW = COLUMNS * SLOT_SIZE + 20;
        int panelH = 62 + rows * SLOT_SIZE;
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;
        AstralFancyButton.renderOutlinedBox(graphics, panelX, panelY, panelW, panelH,
                0xF0181B26, 0xFFE83CA8, 0xD0101018, 1, 2);
        graphics.centeredText(this.font, this.title, this.width / 2, panelY + 8, 0xFFFFFFFF);
        graphics.text(this.font, Component.translatable("gui.astral_craft.creator.inventory_picker.help"), panelX + 10, panelY + 25, 0xFF9AA8BA);

        int gridX = panelX + 10;
        int gridY = panelY + 39;
        if (this.stacks.isEmpty()) {
            graphics.centeredText(this.font, Component.translatable("gui.astral_craft.creator.inventory_picker.empty"), this.width / 2, gridY + 6, 0xFF9AA8BA);
        } else {
            for (int index = 0; index < this.stacks.size(); index++) {
                int x = gridX + index % COLUMNS * SLOT_SIZE;
                int y = gridY + index / COLUMNS * SLOT_SIZE;
                boolean hovered = this.inside(mouseX, mouseY, x, y, SLOT_SIZE - 2, SLOT_SIZE - 2);
                graphics.fill(x, y, x + SLOT_SIZE - 2, y + SLOT_SIZE - 2, hovered ? 0x884F6A9B : 0x66404A5B);
                ItemStack stack = this.stacks.get(index);
                graphics.item(stack, x + 2, y + 2);
                if (stack.getCount() > 1) {
                    Component count = Component.literal(Integer.toString(stack.getCount()));
                    graphics.text(this.font, count, x + 18 - this.font.width(count), y + 11, 0xFFFFFFFF, true);
                }
                if (hovered) {
                    String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                    Component tooltip = Component.literal(stack.getHoverName().getString() + "\n" + id)
                            .append("\n").append(Component.translatable("gui.astral_craft.creator.inventory_picker.data_kept"));
                    graphics.setTooltipForNextFrame(this.font, this.font.split(tooltip, Math.clamp(this.width - 32, 120, 260)), mouseX, mouseY);
                    graphics.requestCursor(CursorTypes.POINTING_HAND);
                }
            }
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        int rows = Math.max(1, (this.stacks.size() + COLUMNS - 1) / COLUMNS);
        int panelW = COLUMNS * SLOT_SIZE + 20;
        int panelH = 62 + rows * SLOT_SIZE;
        int gridX = (this.width - panelW) / 2 + 10;
        int gridY = (this.height - panelH) / 2 + 39;
        for (int index = 0; index < this.stacks.size(); index++) {
            int x = gridX + index % COLUMNS * SLOT_SIZE;
            int y = gridY + index / COLUMNS * SLOT_SIZE;
            if (!this.inside(event.x(), event.y(), x, y, SLOT_SIZE - 2, SLOT_SIZE - 2)) continue;
            this.selected.accept(this.stacks.get(index).copy());
            this.returnToParent();
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.returnToParent();
            return true;
        }
        return super.keyPressed(event);
    }

    private boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private void returnToParent() {
        this.minecraft.setScreen(this.parent);
    }

}