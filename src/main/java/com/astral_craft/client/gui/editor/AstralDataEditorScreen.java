package com.astral_craft.client.gui.editor;

import com.astral_craft.AstralCraft;
import com.astral_craft.client.gui.components.AstralFancyButton;
import com.astral_craft.common.gameplay.event.type.AstralEventTargetScopes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

public class AstralDataEditorScreen extends Screen {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int GAP = 5;
    private static final int FIELD_HEIGHT = 20;
    private static final int BUTTON_HEIGHT = 20;
    private static final int TAB_HEIGHT = 22;
    private static final int SLOT_HEIGHT = 50;
    private static final int DATA_PACK_MAJOR = 101;
    private static final int DATA_PACK_MINOR = 1;
    private static final int RESOURCE_PACK_MAJOR = 84;
    private static final int RESOURCE_PACK_MINOR = 0;
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg");

    private EditorTab tab = EditorTab.EVENT;
    private Path exportRoot;
    private Path appearanceSource;
    private Component status = Component.empty();
    private boolean statusError;

    private EditBox packNameBox;

    private EditBox eventIdBox;
    private EditBox eventNameBox;
    private EditBox eventDescriptionBox;
    private EditBox eventTextureBox;
    private EditBox eventCooldownBox;
    private EditBox eventChanceBox;
    private EditBox eventRadiusBox;
    private TargetScope eventTarget = TargetScope.TRIGGER_PLAYER;
    private boolean eventTriggers = true;
    private boolean eventBroadcast;
    private EventEffectFilter eventEffectFilter = EventEffectFilter.ALL;
    private int eventScrollOffset;
    private boolean eventScrollbarDragging;
    private final ConditionDraft[] eventConditions = {new ConditionDraft(), new ConditionDraft()};
    private final EffectDraft[] eventEffects = {new EffectDraft(), new EffectDraft(), new EffectDraft()};

    private EditBox skinEntryIdBox;
    private EditBox skinNameBox;
    private EditBox skinCharacterBox;
    private EditBox skinIdBox;
    private EditBox skinTextureBox;
    private EditBox skinRarityBox;
    private boolean skinUnlocked;

    private EditBox rarityIdBox;
    private EditBox rarityNameBox;
    private EditBox rarityBorderBox;
    private EditBox rarityBadgeBox;
    private EditBox rarityTextBox;

    private EditBox appearanceNamespaceBox;
    private EditBox appearanceNameBox;
    private EditBox appearanceSourceBox;
    private AppearanceType appearanceType = AppearanceType.CARD_BACK;

    public AstralDataEditorScreen() {
        super(Component.translatable("gui.astral_craft.creator.title"));
        Path gameDirectory = Minecraft.getInstance().gameDirectory.toPath().toAbsolutePath().normalize();
        this.exportRoot = gameDirectory.resolve("astral_creator_exports");
    }

    @Override
    protected void init() {
        EditorLayout layout = this.layout();
        this.packNameBox = this.recreateBox(this.packNameBox, "gui.astral_craft.creator.pack_name_hint", 64, "astral_creator_pack");

        this.eventIdBox = this.recreateBox(this.eventIdBox, "gui.astral_craft.creator.event.id_hint", 160, "example:welcome_event");
        this.eventNameBox = this.recreateBox(this.eventNameBox, "gui.astral_craft.creator.event.name_hint", 160, "");
        this.eventDescriptionBox = this.recreateBox(this.eventDescriptionBox, "gui.astral_craft.creator.event.description_hint", 256, "");
        this.eventTextureBox = this.recreateBox(this.eventTextureBox, "gui.astral_craft.creator.event.texture_hint", 256, "astral_craft:textures/gui/cards/event.png");
        this.eventCooldownBox = this.recreateBox(this.eventCooldownBox, "gui.astral_craft.creator.event.cooldown_hint", 12, "600");
        this.eventChanceBox = this.recreateBox(this.eventChanceBox, "gui.astral_craft.creator.event.chance_hint", 12, "1.0");
        this.eventRadiusBox = this.recreateBox(this.eventRadiusBox, "gui.astral_craft.creator.event.radius_hint", 12, "16.0");
        for (ConditionDraft condition : this.eventConditions) condition.create(this);
        for (EffectDraft effect : this.eventEffects) effect.create(this);

        this.skinEntryIdBox = this.recreateBox(this.skinEntryIdBox, "gui.astral_craft.creator.skin.entry_id_hint", 160, "example:skins/character/default");
        this.skinNameBox = this.recreateBox(this.skinNameBox, "gui.astral_craft.creator.skin.name_hint", 160, "");
        this.skinCharacterBox = this.recreateBox(this.skinCharacterBox, "gui.astral_craft.creator.skin.character_hint", 160, "astral_craft:default");
        this.skinIdBox = this.recreateBox(this.skinIdBox, "gui.astral_craft.creator.skin.skin_id_hint", 96, "default");
        this.skinTextureBox = this.recreateBox(this.skinTextureBox, "gui.astral_craft.creator.skin.texture_hint", 256, "example:entity/character/skin_custom");
        this.skinRarityBox = this.recreateBox(this.skinRarityBox, "gui.astral_craft.creator.skin.rarity_hint", 160, "none");

        this.rarityIdBox = this.recreateBox(this.rarityIdBox, "gui.astral_craft.creator.rarity.id_hint", 160, "example:rare");
        this.rarityNameBox = this.recreateBox(this.rarityNameBox, "gui.astral_craft.creator.rarity.name_hint", 160, "");
        this.rarityBorderBox = this.recreateBox(this.rarityBorderBox, "gui.astral_craft.creator.rarity.border_hint", 10, "#FF8ACB");
        this.rarityBadgeBox = this.recreateBox(this.rarityBadgeBox, "gui.astral_craft.creator.rarity.badge_hint", 10, "#F05BAE");
        this.rarityTextBox = this.recreateBox(this.rarityTextBox, "gui.astral_craft.creator.rarity.text_hint", 10, "#FFFFFF");

        this.appearanceNamespaceBox = this.recreateBox(this.appearanceNamespaceBox, "gui.astral_craft.creator.appearance.namespace_hint", 64, "example");
        this.appearanceNameBox = this.recreateBox(this.appearanceNameBox, "gui.astral_craft.creator.appearance.name_hint", 96, "custom");
        this.appearanceSourceBox = this.recreateBox(this.appearanceSourceBox, "gui.astral_craft.creator.appearance.source_hint", 1024, "");
        this.updateWidgets(layout);
    }

    private EditBox recreateBox(EditBox previous, String hintKey, int maxLength, String fallback) {
        String value = previous == null ? fallback : previous.getValue();
        EditBox box = this.createBox(hintKey, maxLength);
        box.setValue(value);
        return box;
    }

    private EditBox createBox(String hintKey, int maxLength) {
        EditBox box = this.addRenderableWidget(new EditBox(this.font, 0, 0, 100, FIELD_HEIGHT, Component.translatable(hintKey)));
        box.setMaxLength(maxLength);
        box.setHint(Component.translatable(hintKey));
        return box;
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        EditorLayout layout = this.layout();
        this.updateWidgets(layout);
        AstralFancyButton.renderOutlinedBox(graphics, layout.panelX(), layout.panelY(), layout.panelW(), layout.panelH(),
                0xEE151723, 0xE8545B70, 0xD0101018, 1, 2);
        graphics.fill(layout.panelX(), layout.panelY(), layout.panelRight(), layout.panelY() + 3, 0xFFE83CA8);
        graphics.centeredText(this.font, this.title, this.width / 2, layout.panelY() + 8, 0xFFFFFFFF);
        this.renderTabs(graphics, layout, mouseX, mouseY);
        this.renderCommonHeader(graphics, layout, mouseX, mouseY);
        switch (this.tab) {
            case EVENT -> this.renderEventTab(graphics, layout, mouseX, mouseY);
            case CHARACTER_SKIN -> this.renderSkinTab(graphics, layout, mouseX, mouseY);
            case SKIN_RARITY -> this.renderRarityTab(graphics, layout, mouseX, mouseY);
            case APPEARANCE -> this.renderAppearanceTab(graphics, layout, mouseX, mouseY);
        }
        this.renderActions(graphics, layout, mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (this.anyVisibleBoxHovered(mouseX, mouseY)) graphics.requestCursor(CursorTypes.IBEAM);
        else if (this.hoveredManualControl(layout, mouseX, mouseY)) graphics.requestCursor(CursorTypes.POINTING_HAND);
        else graphics.requestCursor(CursorTypes.ARROW);
    }

    private void renderTabs(GuiGraphicsExtractor graphics, EditorLayout layout, int mouseX, int mouseY) {
        for (int index = 0; index < EditorTab.values().length; index++) {
            EditorTab value = EditorTab.values()[index];
            int x = layout.tabX(index);
            boolean hovered = this.isInside(mouseX, mouseY, x, layout.tabY(), layout.tabW(), TAB_HEIGHT);
            AstralFancyButton.renderButton(graphics, this.font, Component.translatable(value.translationKey), x, layout.tabY(), layout.tabW(), TAB_HEIGHT,
                    this.tab == value, hovered, AstralFancyButton.ButtonStyle.button(value.color));
        }
    }

    private void renderCommonHeader(GuiGraphicsExtractor graphics, EditorLayout layout, int mouseX, int mouseY) {
        this.label(graphics, "gui.astral_craft.creator.pack_name", layout.contentX(), layout.packLabelY());
        this.label(graphics, "gui.astral_craft.creator.export_root", layout.pathX(), layout.packLabelY());
        String root = this.exportRoot == null ? "" : this.exportRoot.toString();
        graphics.text(this.font, this.font.plainSubstrByWidth(root, layout.pathW() - 4), layout.pathX(), layout.packBoxY() + 6, 0xFFBBC7D7);
        this.renderButton(graphics, layout.choosePathX(), layout.packBoxY(), layout.choosePathW(), FIELD_HEIGHT,
                "gui.astral_craft.creator.choose_folder", mouseX, mouseY, 0xFF5664B7, false);
    }

    private void renderEventTab(GuiGraphicsExtractor graphics, EditorLayout layout, int mouseX, int mouseY) {
        this.label(graphics, "gui.astral_craft.creator.event.id", layout.leftX(), layout.rowLabelY(0));
        this.label(graphics, "gui.astral_craft.creator.event.name", layout.rightX(), layout.rowLabelY(0));
        this.label(graphics, "gui.astral_craft.creator.event.description", layout.leftX(), layout.rowLabelY(1));
        this.label(graphics, "gui.astral_craft.creator.event.texture", layout.rightX(), layout.rowLabelY(1));
        this.label(graphics, "gui.astral_craft.creator.event.cooldown", layout.eventSmallX(0), layout.rowLabelY(2));
        this.label(graphics, "gui.astral_craft.creator.event.chance", layout.eventSmallX(1), layout.rowLabelY(2));
        this.label(graphics, "gui.astral_craft.creator.event.radius", layout.eventSmallX(2), layout.rowLabelY(2));
        this.renderButton(graphics, layout.eventSmallX(3), layout.rowBoxY(2), layout.eventSmallW(), FIELD_HEIGHT,
                this.eventTarget.translationKey, mouseX, mouseY, 0xFF5664B7, false);
        this.renderBooleanButton(graphics, layout.eventSmallX(4), layout.rowBoxY(2), layout.eventSmallW(), FIELD_HEIGHT,
                "gui.astral_craft.creator.event.auto_trigger", this.eventTriggers, mouseX, mouseY);
        this.renderBooleanButton(graphics, layout.eventSmallX(5), layout.rowBoxY(2), layout.eventSmallW(), FIELD_HEIGHT,
                "gui.astral_craft.creator.event.broadcast", this.eventBroadcast, mouseX, mouseY);

        int slotsY = layout.eventSlotsY();
        int columnW = (layout.contentW() - GAP) / 2;
        graphics.text(this.font, Component.translatable("gui.astral_craft.creator.event.conditions"), layout.contentX(), slotsY - 11, 0xFFD7E4F2);
        graphics.text(this.font, Component.translatable("gui.astral_craft.creator.event.effects"), layout.contentX() + columnW + GAP, slotsY - 11, 0xFFD7E4F2);
        int viewportBottom = layout.eventViewportBottom();
        graphics.enableScissor(layout.contentX(), slotsY, layout.contentX() + layout.contentW(), viewportBottom);
        for (int index = 0; index < this.eventConditions.length; index++) this.renderConditionSlot(graphics, layout, index, mouseX, mouseY);
        for (int index = 0; index < this.eventEffects.length; index++) this.renderEffectSlot(graphics, layout, index, mouseX, mouseY);
        graphics.disableScissor();
        this.renderButton(graphics, layout.effectFilterX(), slotsY - 16, layout.effectFilterW(), 14, this.eventEffectFilter.translationKey, mouseX, mouseY, 0xFF5664B7, false);
        this.renderEventScrollbar(graphics, layout);
    }

    private void renderConditionSlot(GuiGraphicsExtractor graphics, EditorLayout layout, int index, int mouseX, int mouseY) {
        ConditionDraft draft = this.eventConditions[index];
        int x = layout.conditionX();
        int y = layout.slotY(index) - this.eventScrollOffset;
        this.renderButton(graphics, x, y, layout.slotTypeW(), BUTTON_HEIGHT, draft.type.translationKey, mouseX, mouseY, 0xFF6B5AA7, false);
        draft.renderLabels(graphics, this, x, y + BUTTON_HEIGHT + 1, layout.slotArgW());
    }

    private void renderEffectSlot(GuiGraphicsExtractor graphics, EditorLayout layout, int index, int mouseX, int mouseY) {
        EffectDraft draft = this.eventEffects[index];
        int x = layout.effectX();
        int y = layout.slotY(index) - this.eventScrollOffset;
        this.renderButton(graphics, x, y, layout.slotTypeW(), BUTTON_HEIGHT, draft.type.translationKey, mouseX, mouseY, 0xFFB05282, false);
        draft.renderLabels(graphics, this, x, y + BUTTON_HEIGHT + 1, layout.slotArgW());
    }

    private void renderSkinTab(GuiGraphicsExtractor graphics, EditorLayout layout, int mouseX, int mouseY) {
        this.label(graphics, "gui.astral_craft.creator.skin.entry_id", layout.leftX(), layout.rowLabelY(0));
        this.label(graphics, "gui.astral_craft.creator.skin.name", layout.rightX(), layout.rowLabelY(0));
        this.label(graphics, "gui.astral_craft.creator.skin.character", layout.leftX(), layout.rowLabelY(1));
        this.label(graphics, "gui.astral_craft.creator.skin.skin_id", layout.rightX(), layout.rowLabelY(1));
        this.label(graphics, "gui.astral_craft.creator.skin.texture", layout.leftX(), layout.rowLabelY(2));
        this.label(graphics, "gui.astral_craft.creator.skin.rarity", layout.rightX(), layout.rowLabelY(2));
        this.renderBooleanButton(graphics, layout.leftX(), layout.rowBoxY(3), layout.halfW(), FIELD_HEIGHT,
                "gui.astral_craft.creator.skin.unlocked", this.skinUnlocked, mouseX, mouseY);
        graphics.text(this.font, Component.translatable("gui.astral_craft.creator.skin.help"), layout.leftX(), layout.rowBoxY(3) + 31, 0xFF9AA8BA);
    }

    private void renderRarityTab(GuiGraphicsExtractor graphics, EditorLayout layout, int mouseX, int mouseY) {
        this.label(graphics, "gui.astral_craft.creator.rarity.id", layout.leftX(), layout.rowLabelY(0));
        this.label(graphics, "gui.astral_craft.creator.rarity.name", layout.rightX(), layout.rowLabelY(0));
        this.label(graphics, "gui.astral_craft.creator.rarity.border", layout.rarityColorX(0), layout.rowLabelY(1));
        this.label(graphics, "gui.astral_craft.creator.rarity.badge", layout.rarityColorX(1), layout.rowLabelY(1));
        this.label(graphics, "gui.astral_craft.creator.rarity.text", layout.rarityColorX(2), layout.rowLabelY(1));
        graphics.text(this.font, Component.translatable("gui.astral_craft.creator.rarity.help"), layout.leftX(), layout.rowBoxY(2), 0xFF9AA8BA);
    }

    private void renderAppearanceTab(GuiGraphicsExtractor graphics, EditorLayout layout, int mouseX, int mouseY) {
        this.label(graphics, "gui.astral_craft.creator.appearance.namespace", layout.leftX(), layout.rowLabelY(0));
        this.label(graphics, "gui.astral_craft.creator.appearance.name", layout.rightX(), layout.rowLabelY(0));
        this.label(graphics, "gui.astral_craft.creator.appearance.source", layout.leftX(), layout.rowLabelY(1));
        this.renderButton(graphics, layout.rightX(), layout.rowBoxY(1), layout.halfW(), FIELD_HEIGHT,
                "gui.astral_craft.creator.appearance.choose_source", mouseX, mouseY, 0xFF5664B7, false);
        this.renderButton(graphics, layout.leftX(), layout.rowBoxY(2), layout.halfW(), FIELD_HEIGHT,
                this.appearanceType.translationKey, mouseX, mouseY, 0xFFB05282, false);
        graphics.text(this.font, Component.translatable("gui.astral_craft.creator.appearance.help"), layout.leftX(), layout.rowBoxY(2) + 32, 0xFF9AA8BA);
    }

    private void renderActions(GuiGraphicsExtractor graphics, EditorLayout layout, int mouseX, int mouseY) {
        if (this.status != null && !this.status.getString().isBlank()) {
            int color = this.statusError ? 0xFFFF8F9E : 0xFF8FE2A9;
            graphics.text(this.font, this.font.plainSubstrByWidth(this.status.getString(), layout.contentW() - 4), layout.contentX(), layout.statusY(), color);
        }
        this.renderButton(graphics, layout.exportX(), layout.actionY(), layout.actionButtonW(), 22,
                "gui.astral_craft.creator.export", mouseX, mouseY, 0xFF4F9D69, false);
        this.renderButton(graphics, layout.closeX(), layout.actionY(), layout.actionButtonW(), 22,
                "gui.astral_craft.cancel", mouseX, mouseY, 0xFF646477, false);
    }

    private void label(GuiGraphicsExtractor graphics, String key, int x, int y) {
        graphics.text(this.font, Component.translatable(key), x, y, 0xFFD7E4F2);
    }

    private void renderBooleanButton(GuiGraphicsExtractor graphics, int x, int y, int width, int height, String key, boolean value, int mouseX, int mouseY) {
        Component state = Component.translatable(value ? "gui.astral_craft.creator.enabled" : "gui.astral_craft.creator.disabled");
        Component label = Component.translatable(key, state);
        boolean hovered = this.isInside(mouseX, mouseY, x, y, width, height);
        AstralFancyButton.renderButton(graphics, this.font, label, x, y, width, height, value, hovered,
                AstralFancyButton.ButtonStyle.button(value ? 0xFF4F9D69 : 0xFF666A79));
    }

    private void renderButton(GuiGraphicsExtractor graphics, int x, int y, int width, int height, String key, int mouseX, int mouseY, int color, boolean selected) {
        boolean hovered = this.isInside(mouseX, mouseY, x, y, width, height);
        AstralFancyButton.renderButton(graphics, this.font, Component.translatable(key), x, y, width, height, selected, hovered,
                AstralFancyButton.ButtonStyle.button(color));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        EditorLayout layout = this.layout();
        double mouseX = event.x();
        double mouseY = event.y();
        for (int index = 0; index < EditorTab.values().length; index++) {
            if (this.isInside(mouseX, mouseY, layout.tabX(index), layout.tabY(), layout.tabW(), TAB_HEIGHT)) {
                this.tab = EditorTab.values()[index];
                this.status = Component.empty();
                this.updateWidgets(layout);
                return true;
            }
        }
        if (this.tab == EditorTab.EVENT && this.handleEventScrollbarClick(layout, mouseX, mouseY)) return true;
        if (this.isInside(mouseX, mouseY, layout.choosePathX(), layout.packBoxY(), layout.choosePathW(), FIELD_HEIGHT)) {
            this.openExportFolderBrowser();
            return true;
        }
        if (this.isInside(mouseX, mouseY, layout.exportX(), layout.actionY(), layout.actionButtonW(), 22)) {
            this.exportCurrent();
            return true;
        }
        if (this.isInside(mouseX, mouseY, layout.closeX(), layout.actionY(), layout.actionButtonW(), 22)) {
            this.onClose();
            return true;
        }
        if (this.handleTabClick(layout, mouseX, mouseY)) return true;
        return super.mouseClicked(event, doubleClick);
    }

    private boolean handleTabClick(EditorLayout layout, double mouseX, double mouseY) {
        if (this.tab == EditorTab.EVENT) {
            if (this.isInside(mouseX, mouseY, layout.eventSmallX(3), layout.rowBoxY(2), layout.eventSmallW(), FIELD_HEIGHT)) {
                this.eventTarget = this.eventTarget.next();
                return true;
            }
            if (this.isInside(mouseX, mouseY, layout.eventSmallX(4), layout.rowBoxY(2), layout.eventSmallW(), FIELD_HEIGHT)) {
                this.eventTriggers = !this.eventTriggers;
                return true;
            }
            if (this.isInside(mouseX, mouseY, layout.eventSmallX(5), layout.rowBoxY(2), layout.eventSmallW(), FIELD_HEIGHT)) {
                this.eventBroadcast = !this.eventBroadcast;
                return true;
            }
            for (int index = 0; index < this.eventConditions.length; index++) {
                if (this.isInside(mouseX, mouseY, layout.conditionX(), layout.slotY(index) - this.eventScrollOffset, layout.slotTypeW(), BUTTON_HEIGHT)) {
                    this.eventConditions[index].nextType();
                    this.updateWidgets(layout);
                    return true;
                }
            }
            if (this.isInside(mouseX, mouseY, layout.effectFilterX(), layout.eventSlotsY() - 16, layout.effectFilterW(), 14)) {
                this.eventEffectFilter = this.eventEffectFilter.next();
                return true;
            }
            for (int index = 0; index < this.eventEffects.length; index++) {
                if (this.isInside(mouseX, mouseY, layout.effectX(), layout.slotY(index) - this.eventScrollOffset, layout.slotTypeW(), BUTTON_HEIGHT)) {
                    this.eventEffects[index].nextType(this.eventEffectFilter);
                    this.updateWidgets(layout);
                    return true;
                }
            }
        } else if (this.tab == EditorTab.CHARACTER_SKIN) {
            if (this.isInside(mouseX, mouseY, layout.leftX(), layout.rowBoxY(3), layout.halfW(), FIELD_HEIGHT)) {
                this.skinUnlocked = !this.skinUnlocked;
                return true;
            }
        } else if (this.tab == EditorTab.APPEARANCE) {
            if (this.isInside(mouseX, mouseY, layout.rightX(), layout.rowBoxY(1), layout.halfW(), FIELD_HEIGHT)) {
                this.openAppearanceFileBrowser();
                return true;
            }
            if (this.isInside(mouseX, mouseY, layout.leftX(), layout.rowBoxY(2), layout.halfW(), FIELD_HEIGHT)) {
                this.appearanceType = this.appearanceType.next();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
        if (event.button() == 0 && this.eventScrollbarDragging && this.tab == EditorTab.EVENT) {
            this.updateEventScrollFromMouse(this.layout(), event.y());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && this.eventScrollbarDragging) {
            this.eventScrollbarDragging = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        EditorLayout layout = this.layout();
        if (this.tab == EditorTab.EVENT && this.isInside(mouseX, mouseY, layout.contentX(), layout.eventSlotsY(), layout.contentW(), layout.eventViewportH())) {
            int max = this.maxEventScroll(layout);
            this.eventScrollOffset = Math.clamp(this.eventScrollOffset - (int) Math.signum(deltaY) * 24, 0, max);
            this.updateWidgets(layout);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    private boolean handleEventScrollbarClick(EditorLayout layout, double mouseX, double mouseY) {
        int max = this.maxEventScroll(layout);
        if (max <= 0) return false;
        int x = layout.contentX() + layout.contentW() - 6;
        if (!this.isInside(mouseX, mouseY, x, layout.eventSlotsY(), 6, layout.eventViewportH())) return false;
        this.eventScrollbarDragging = true;
        this.updateEventScrollFromMouse(layout, mouseY);
        return true;
    }

    private void updateEventScrollFromMouse(EditorLayout layout, double mouseY) {
        int max = this.maxEventScroll(layout);
        if (max <= 0) {
            this.eventScrollOffset = 0;
            return;
        }
        int height = layout.eventViewportH();
        int thumb = Math.max(16, height * height / (height + max));
        double track = Math.max(1.0D, height - thumb);
        double progress = Math.clamp((mouseY - layout.eventSlotsY() - thumb * 0.5D) / track, 0.0D, 1.0D);
        this.eventScrollOffset = Math.clamp((int) Math.round(progress * max), 0, max);
        this.updateWidgets(layout);
    }

    private int maxEventScroll(EditorLayout layout) {
        int contentBottom = layout.slotY(Math.max(this.eventConditions.length, this.eventEffects.length) - 1) + SLOT_HEIGHT;
        return Math.max(0, contentBottom - layout.eventViewportBottom());
    }

    private void renderEventScrollbar(GuiGraphicsExtractor graphics, EditorLayout layout) {
        int max = this.maxEventScroll(layout);
        if (max <= 0) return;
        int x = layout.contentX() + layout.contentW() - 5;
        int top = layout.eventSlotsY();
        int height = layout.eventViewportH();
        graphics.fill(x, top, x + 4, top + height, 0x554F5668);
        int thumb = Math.max(16, height * height / (height + max));
        int y = top + (height - thumb) * this.eventScrollOffset / max;
        graphics.fill(x, y, x + 4, y + thumb, 0xFFE83CA8);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(null);
    }

    private void openExportFolderBrowser() {
        if (this.minecraft == null) return;
        Path start = this.exportRoot != null && Files.isDirectory(this.exportRoot) ? this.exportRoot : this.minecraft.gameDirectory.toPath();
        this.minecraft.setScreen(AstralFileBrowserScreen.folder(this, start, path -> this.exportRoot = path));
    }

    private void openAppearanceFileBrowser() {
        if (this.minecraft == null) return;
        Path start = this.appearanceSource == null ? this.minecraft.gameDirectory.toPath() : this.appearanceSource;
        if (this.appearanceSourceBox != null && !this.appearanceSourceBox.getValue().isBlank()) {
            try {
                start = Path.of(this.appearanceSourceBox.getValue()).toAbsolutePath().normalize();
            } catch (RuntimeException ignored) {}
        }
        this.minecraft.setScreen(AstralFileBrowserScreen.file(this, start, this.appearanceType.extensions(), path -> {
            this.appearanceSource = path;
            if (this.appearanceSourceBox != null) this.appearanceSourceBox.setValue(path.toString());
            if (this.appearanceNameBox != null && (this.appearanceNameBox.getValue().isBlank() || this.appearanceNameBox.getValue().equals("custom"))) {
                String filename = path.getFileName().toString();
                int dot = filename.lastIndexOf('.');
                this.appearanceNameBox.setValue(dot > 0 ? filename.substring(0, dot) : filename);
            }
        }));
    }

    private void exportCurrent() {
        try {
            this.status = Component.empty();
            this.statusError = false;
            String packName = this.safePackName(this.requireText(this.packNameBox, "gui.astral_craft.creator.error.pack_name"));
            if (this.exportRoot == null) throw new EditorException("gui.astral_craft.creator.error.export_root");
            Files.createDirectories(this.exportRoot);
            Path dataPack = this.exportRoot.resolve(packName + "_data");
            Path resourcePack = this.exportRoot.resolve(packName + "_resources");
            switch (this.tab) {
                case EVENT -> this.exportEvent(dataPack, resourcePack);
                case CHARACTER_SKIN -> this.exportSkin(dataPack, resourcePack);
                case SKIN_RARITY -> this.exportRarity(dataPack, resourcePack);
                case APPEARANCE -> this.exportAppearance(resourcePack);
            }
            this.status = Component.translatable("gui.astral_craft.creator.export_success", this.exportRoot.toString());
        } catch (EditorException exception) {
            this.statusError = true;
            this.status = Component.translatable(exception.translationKey, exception.args);
        } catch (IOException | SecurityException exception) {
            this.statusError = true;
            this.status = Component.translatable("gui.astral_craft.creator.error.io", exception.getClass().getSimpleName());
        }
    }

    private void exportEvent(Path dataPack, Path resourcePack) throws IOException, EditorException {
        Identifier id = this.requireIdentifier(this.eventIdBox, "gui.astral_craft.creator.error.event_id");
        Identifier texture = this.requireIdentifier(this.eventTextureBox, "gui.astral_craft.creator.error.texture");
        String displayName = this.requireText(this.eventNameBox, "gui.astral_craft.creator.error.display_name");
        String description = this.requireText(this.eventDescriptionBox, "gui.astral_craft.creator.error.description");
        int cooldown = this.requireInt(this.eventCooldownBox, 0, Integer.MAX_VALUE, "gui.astral_craft.creator.error.cooldown");
        double chance = this.requireDouble(this.eventChanceBox, 0.0D, 1.0D, "gui.astral_craft.creator.error.chance");
        double radius = this.requireDouble(this.eventRadiusBox, 1.0D, 1024.0D, "gui.astral_craft.creator.error.radius");
        JsonArray conditions = new JsonArray();
        for (ConditionDraft draft : this.eventConditions) {
            JsonObject value = draft.toJson(this);
            if (value != null) conditions.add(value);
        }
        JsonArray effects = new JsonArray();
        for (EffectDraft draft : this.eventEffects) {
            JsonObject value = draft.toJson(this);
            if (value != null) effects.add(value);
        }
        if (effects.size() == 0) throw new EditorException("gui.astral_craft.creator.error.effect_required");
        for (EffectDraft draft : this.eventEffects) {
            if (!draft.compatibleWith(this.eventTarget)) throw new EditorException("gui.astral_craft.creator.error.incompatible_target");
        }

        String nameKey = "event." + id.getNamespace() + "." + id.getPath().replace('/', '.') + ".name";
        String descriptionKey = "event." + id.getNamespace() + "." + id.getPath().replace('/', '.') + ".description";
        JsonObject target = new JsonObject();
        target.addProperty("scope", this.eventTarget.id.toString());
        target.addProperty("radius", radius);
        JsonObject root = new JsonObject();
        root.addProperty("id", id.toString());
        root.addProperty("name_key", nameKey);
        root.addProperty("description_key", descriptionKey);
        root.addProperty("texture", texture.toString());
        root.addProperty("triggers", this.eventTriggers);
        if (conditions.size() > 0) root.add("conditions", conditions);
        root.add("target", target);
        root.addProperty("cooldown_ticks", cooldown);
        root.addProperty("chance", chance);
        root.addProperty("broadcast", this.eventBroadcast);
        root.add("effects", effects);
        this.ensureDataPack(dataPack);
        Path eventFile = dataPack.resolve("data").resolve(id.getNamespace()).resolve("astral_craft/events").resolve(id.getPath() + ".json");
        this.writeJson(eventFile, root);
        this.ensureResourcePack(resourcePack);
        this.mergeLanguage(resourcePack, id.getNamespace(), "zh_cn", nameKey, displayName, descriptionKey, description);
        this.mergeLanguage(resourcePack, id.getNamespace(), "en_us", nameKey, displayName, descriptionKey, description);
    }

    private void exportSkin(Path dataPack, Path resourcePack) throws IOException, EditorException {
        Identifier entryId = this.requireIdentifier(this.skinEntryIdBox, "gui.astral_craft.creator.error.skin_entry_id");
        Identifier character = this.requireIdentifier(this.skinCharacterBox, "gui.astral_craft.creator.error.character_id");
        Identifier texture = this.requireIdentifier(this.skinTextureBox, "gui.astral_craft.creator.error.texture");
        String skinId = this.requireSimplePath(this.skinIdBox, "gui.astral_craft.creator.error.skin_id");
        String displayName = this.requireText(this.skinNameBox, "gui.astral_craft.creator.error.display_name");
        String rarity = this.requireText(this.skinRarityBox, "gui.astral_craft.creator.error.rarity");
        String nameKey = "character_skin." + entryId.getNamespace() + "." + entryId.getPath().replace('/', '.') + ".name";
        JsonObject root = new JsonObject();
        root.addProperty("character", character.toString());
        root.addProperty("id", skinId);
        root.addProperty("name_key", nameKey);
        root.addProperty("texture", texture.toString());
        root.addProperty("unlocked_by_default", this.skinUnlocked);
        root.addProperty("rarity", rarity);
        this.ensureDataPack(dataPack);
        Path file = dataPack.resolve("data").resolve(entryId.getNamespace()).resolve("astral_craft/character_skins").resolve(entryId.getPath() + ".json");
        this.writeJson(file, root);
        this.ensureResourcePack(resourcePack);
        this.mergeLanguage(resourcePack, entryId.getNamespace(), "zh_cn", nameKey, displayName);
        this.mergeLanguage(resourcePack, entryId.getNamespace(), "en_us", nameKey, displayName);
    }

    private void exportRarity(Path dataPack, Path resourcePack) throws IOException, EditorException {
        Identifier id = this.requireIdentifier(this.rarityIdBox, "gui.astral_craft.creator.error.rarity_id");
        String displayName = this.requireText(this.rarityNameBox, "gui.astral_craft.creator.error.display_name");
        int border = this.requireColor(this.rarityBorderBox, "gui.astral_craft.creator.error.color");
        int badge = this.requireColor(this.rarityBadgeBox, "gui.astral_craft.creator.error.color");
        int text = this.requireColor(this.rarityTextBox, "gui.astral_craft.creator.error.color");
        String nameKey = "skin_rarity." + id.getNamespace() + "." + id.getPath().replace('/', '.') + ".name";
        JsonObject root = new JsonObject();
        root.addProperty("name_key", nameKey);
        root.addProperty("border_color", border);
        root.addProperty("badge_color", badge);
        root.addProperty("text_color", text);
        this.ensureDataPack(dataPack);
        Path file = dataPack.resolve("data").resolve(id.getNamespace()).resolve("astral_craft/skin_rarities").resolve(id.getPath() + ".json");
        this.writeJson(file, root);
        this.ensureResourcePack(resourcePack);
        Path clientFile = resourcePack.resolve("assets").resolve(id.getNamespace()).resolve("astral_craft/skin_rarities").resolve(id.getPath() + ".json");
        this.writeJson(clientFile, root);
        this.mergeLanguage(resourcePack, id.getNamespace(), "zh_cn", nameKey, displayName);
        this.mergeLanguage(resourcePack, id.getNamespace(), "en_us", nameKey, displayName);
    }

    private void exportAppearance(Path resourcePack) throws IOException, EditorException {
        String namespace = this.requireNamespace(this.appearanceNamespaceBox);
        String name = this.requireSimplePath(this.appearanceNameBox, "gui.astral_craft.creator.error.appearance_name");
        Path source = this.requirePath(this.appearanceSourceBox, "gui.astral_craft.creator.error.appearance_source");
        if (!Files.isRegularFile(source)) throw new EditorException("gui.astral_craft.creator.error.appearance_source");
        this.appearanceSource = source;
        String sourceName = source.getFileName().toString();
        int dot = sourceName.lastIndexOf('.');
        String extension = dot >= 0 ? sourceName.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
        if (!this.appearanceType.accepts(extension)) throw new EditorException("gui.astral_craft.creator.error.appearance_source");
        this.ensureResourcePack(resourcePack);
        Path target = resourcePack.resolve("assets").resolve(namespace).resolve(this.appearanceType.directory).resolve(name + "." + extension);
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private void ensureDataPack(Path dataPack) throws IOException {
        Files.createDirectories(dataPack);
        Path metadata = dataPack.resolve("pack.mcmeta");
        if (Files.isRegularFile(metadata)) return;
        JsonObject pack = new JsonObject();
        pack.addProperty("description", "AstralCraft creator data pack");
        JsonArray minFormat = new JsonArray();
        minFormat.add(DATA_PACK_MAJOR);
        minFormat.add(DATA_PACK_MINOR);
        JsonArray maxFormat = new JsonArray();
        maxFormat.add(DATA_PACK_MAJOR);
        maxFormat.add(DATA_PACK_MINOR);
        pack.add("min_format", minFormat);
        pack.add("max_format", maxFormat);
        JsonObject root = new JsonObject();
        root.add("pack", pack);
        this.writeJson(metadata, root);
    }

    private void ensureResourcePack(Path resourcePack) throws IOException {
        Files.createDirectories(resourcePack);
        Path metadata = resourcePack.resolve("pack.mcmeta");
        if (Files.isRegularFile(metadata)) return;
        JsonObject pack = new JsonObject();
        pack.addProperty("description", "AstralCraft creator resource pack");
        JsonArray minFormat = new JsonArray();
        minFormat.add(RESOURCE_PACK_MAJOR);
        minFormat.add(RESOURCE_PACK_MINOR);
        JsonArray maxFormat = new JsonArray();
        maxFormat.add(RESOURCE_PACK_MAJOR);
        maxFormat.add(RESOURCE_PACK_MINOR);
        pack.add("min_format", minFormat);
        pack.add("max_format", maxFormat);
        JsonObject root = new JsonObject();
        root.add("pack", pack);
        this.writeJson(metadata, root);
    }

    private void mergeLanguage(Path resourcePack, String namespace, String language, String... entries) throws IOException {
        Path file = resourcePack.resolve("assets").resolve(namespace).resolve("lang").resolve(language + ".json");
        JsonObject root = new JsonObject();
        if (Files.isRegularFile(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                JsonObject loaded = GSON.fromJson(reader, JsonObject.class);
                if (loaded != null) root = loaded;
            } catch (RuntimeException ignored) {}
        }
        for (int index = 0; index + 1 < entries.length; index += 2) root.addProperty(entries[index], entries[index + 1]);
        this.writeJson(file, root);
    }

    private void writeJson(Path file, JsonObject root) throws IOException {
        Files.createDirectories(file.getParent());
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
            writer.write('\n');
        }
    }

    private String safePackName(String value) throws EditorException {
        String normalized = value.strip().replaceAll("[^A-Za-z0-9._-]+", "_");
        while (normalized.startsWith(".")) normalized = normalized.substring(1);
        if (normalized.isBlank() || normalized.equals(".") || normalized.equals("..")) throw new EditorException("gui.astral_craft.creator.error.pack_name");
        return normalized;
    }

    private Path requirePath(EditBox box, String key) throws EditorException {
        String value = this.requireText(box, key);
        try {
            return Path.of(value).toAbsolutePath().normalize();
        } catch (RuntimeException exception) {
            throw new EditorException(key);
        }
    }

    private String requireNamespace(EditBox box) throws EditorException {
        String value = this.requireText(box, "gui.astral_craft.creator.error.namespace").toLowerCase(Locale.ROOT);
        Identifier test = Identifier.tryParse(value + ":value");
        if (test == null || !test.getNamespace().equals(value)) throw new EditorException("gui.astral_craft.creator.error.namespace");
        return value;
    }

    private String requireSimplePath(EditBox box, String key) throws EditorException {
        String value = this.requireText(box, key).toLowerCase(Locale.ROOT);
        Identifier test = Identifier.tryParse("astral_craft:" + value);
        if (test == null || value.contains("..")) throw new EditorException(key);
        return value;
    }

    private Identifier requireIdentifier(EditBox box, String key) throws EditorException {
        Identifier value = Identifier.tryParse(this.requireText(box, key));
        if (value == null || value.getPath().contains("..")) throw new EditorException(key);
        return value;
    }

    private String requireText(EditBox box, String key) throws EditorException {
        if (box == null || box.getValue().isBlank()) throw new EditorException(key);
        return box.getValue().strip();
    }

    private int requireInt(EditBox box, int min, int max, String key) throws EditorException {
        try {
            int value = Integer.parseInt(this.requireText(box, key));
            if (value < min || value > max) throw new EditorException(key);
            return value;
        } catch (NumberFormatException exception) {
            throw new EditorException(key);
        }
    }

    private long requireLong(EditBox box, long min, long max, String key) throws EditorException {
        try {
            long value = Long.parseLong(this.requireText(box, key));
            if (value < min || value > max) throw new EditorException(key);
            return value;
        } catch (NumberFormatException exception) {
            throw new EditorException(key);
        }
    }

    private double requireDouble(EditBox box, double min, double max, String key) throws EditorException {
        try {
            double value = Double.parseDouble(this.requireText(box, key));
            if (!Double.isFinite(value) || value < min || value > max) throw new EditorException(key);
            return value;
        } catch (NumberFormatException exception) {
            throw new EditorException(key);
        }
    }

    private int requireColor(EditBox box, String key) throws EditorException {
        String value = this.requireText(box, key);
        if (value.startsWith("#")) value = value.substring(1);
        try {
            if (value.length() != 6 && value.length() != 8) throw new EditorException(key);
            long parsed = Long.parseUnsignedLong(value, 16);
            if (value.length() == 6) parsed |= 0xFF000000L;
            return (int) parsed;
        } catch (NumberFormatException exception) {
            throw new EditorException(key);
        }
    }

    private boolean requireBoolean(EditBox box, String key) throws EditorException {
        String value = this.requireText(box, key).toLowerCase(Locale.ROOT);
        if (!value.equals("true") && !value.equals("false")) throw new EditorException(key);
        return Boolean.parseBoolean(value);
    }

    private List<String> requireCsv(EditBox box, String key, boolean identifiers) throws EditorException {
        String raw = this.requireText(box, key);
        List<String> values = new ArrayList<>();
        for (String part : raw.split(",")) {
            String value = part.strip();
            if (value.isEmpty()) continue;
            if (identifiers && Identifier.tryParse(value) == null) throw new EditorException(key);
            values.add(value);
        }
        if (values.isEmpty()) throw new EditorException(key);
        return List.copyOf(values);
    }

    private void updateWidgets(EditorLayout layout) {
        if (this.packNameBox == null) return;
        this.position(this.packNameBox, layout.contentX(), layout.packBoxY(), layout.packNameW(), true);
        boolean event = this.tab == EditorTab.EVENT;
        this.position(this.eventIdBox, layout.leftX(), layout.rowBoxY(0), layout.halfW(), event);
        this.position(this.eventNameBox, layout.rightX(), layout.rowBoxY(0), layout.halfW(), event);
        this.position(this.eventDescriptionBox, layout.leftX(), layout.rowBoxY(1), layout.halfW(), event);
        this.position(this.eventTextureBox, layout.rightX(), layout.rowBoxY(1), layout.halfW(), event);
        this.position(this.eventCooldownBox, layout.eventSmallX(0), layout.rowBoxY(2), layout.eventSmallW(), event);
        this.position(this.eventChanceBox, layout.eventSmallX(1), layout.rowBoxY(2), layout.eventSmallW(), event);
        this.position(this.eventRadiusBox, layout.eventSmallX(2), layout.rowBoxY(2), layout.eventSmallW(), event);
        for (int index = 0; index < this.eventConditions.length; index++) this.eventConditions[index].position(layout.conditionX(), layout.slotY(index) - this.eventScrollOffset + BUTTON_HEIGHT + 13, layout.slotArgW(), event);
        for (int index = 0; index < this.eventEffects.length; index++) this.eventEffects[index].position(layout.effectX(), layout.slotY(index) - this.eventScrollOffset + BUTTON_HEIGHT + 13, layout.slotArgW(), event);

        boolean skin = this.tab == EditorTab.CHARACTER_SKIN;
        this.position(this.skinEntryIdBox, layout.leftX(), layout.rowBoxY(0), layout.halfW(), skin);
        this.position(this.skinNameBox, layout.rightX(), layout.rowBoxY(0), layout.halfW(), skin);
        this.position(this.skinCharacterBox, layout.leftX(), layout.rowBoxY(1), layout.halfW(), skin);
        this.position(this.skinIdBox, layout.rightX(), layout.rowBoxY(1), layout.halfW(), skin);
        this.position(this.skinTextureBox, layout.leftX(), layout.rowBoxY(2), layout.halfW(), skin);
        this.position(this.skinRarityBox, layout.rightX(), layout.rowBoxY(2), layout.halfW(), skin);

        boolean rarity = this.tab == EditorTab.SKIN_RARITY;
        this.position(this.rarityIdBox, layout.leftX(), layout.rowBoxY(0), layout.halfW(), rarity);
        this.position(this.rarityNameBox, layout.rightX(), layout.rowBoxY(0), layout.halfW(), rarity);
        this.position(this.rarityBorderBox, layout.rarityColorX(0), layout.rowBoxY(1), layout.rarityColorW(), rarity);
        this.position(this.rarityBadgeBox, layout.rarityColorX(1), layout.rowBoxY(1), layout.rarityColorW(), rarity);
        this.position(this.rarityTextBox, layout.rarityColorX(2), layout.rowBoxY(1), layout.rarityColorW(), rarity);

        boolean appearance = this.tab == EditorTab.APPEARANCE;
        this.position(this.appearanceNamespaceBox, layout.leftX(), layout.rowBoxY(0), layout.halfW(), appearance);
        this.position(this.appearanceNameBox, layout.rightX(), layout.rowBoxY(0), layout.halfW(), appearance);
        this.position(this.appearanceSourceBox, layout.leftX(), layout.rowBoxY(1), layout.halfW(), appearance);
    }

    private void position(EditBox box, int x, int y, int width, boolean visible) {
        if (box == null) return;
        box.setPosition(x, y);
        box.setWidth(width);
        box.setVisible(visible);
        box.active = visible;
    }

    private boolean anyVisibleBoxHovered(double mouseX, double mouseY) {
        for (EditBox box : this.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).toList()) {
            if (box.visible && box.isMouseOver(mouseX, mouseY)) return true;
        }
        return false;
    }

    private boolean hoveredManualControl(EditorLayout layout, double mouseX, double mouseY) {
        for (int index = 0; index < EditorTab.values().length; index++) if (this.isInside(mouseX, mouseY, layout.tabX(index), layout.tabY(), layout.tabW(), TAB_HEIGHT)) return true;
        if (this.isInside(mouseX, mouseY, layout.choosePathX(), layout.packBoxY(), layout.choosePathW(), FIELD_HEIGHT)
                || this.isInside(mouseX, mouseY, layout.exportX(), layout.actionY(), layout.actionButtonW(), 22)
                || this.isInside(mouseX, mouseY, layout.closeX(), layout.actionY(), layout.actionButtonW(), 22)) return true;
        if (this.tab == EditorTab.EVENT) {
            for (int index = 3; index < 6; index++) if (this.isInside(mouseX, mouseY, layout.eventSmallX(index), layout.rowBoxY(2), layout.eventSmallW(), FIELD_HEIGHT)) return true;
            for (int index = 0; index < this.eventConditions.length; index++) if (this.isInside(mouseX, mouseY, layout.conditionX(), layout.slotY(index) - this.eventScrollOffset, layout.slotTypeW(), BUTTON_HEIGHT)) return true;
            for (int index = 0; index < this.eventEffects.length; index++) if (this.isInside(mouseX, mouseY, layout.effectX(), layout.slotY(index) - this.eventScrollOffset, layout.slotTypeW(), BUTTON_HEIGHT)) return true;
        } else if (this.tab == EditorTab.CHARACTER_SKIN) {
            return this.isInside(mouseX, mouseY, layout.leftX(), layout.rowBoxY(3), layout.halfW(), FIELD_HEIGHT);
        } else if (this.tab == EditorTab.APPEARANCE) {
            return this.isInside(mouseX, mouseY, layout.rightX(), layout.rowBoxY(1), layout.halfW(), FIELD_HEIGHT)
                    || this.isInside(mouseX, mouseY, layout.leftX(), layout.rowBoxY(2), layout.halfW(), FIELD_HEIGHT);
        }
        return false;
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private EditorLayout layout() {
        int panelW = Math.min(770, Math.max(390, this.width - 20));
        int panelH = Math.min(480, Math.max(330, this.height - 16));
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;
        return new EditorLayout(panelX, panelY, panelW, panelH);
    }

    private enum EditorTab {
        EVENT("gui.astral_craft.creator.tab.event", 0xFFB05282),
        CHARACTER_SKIN("gui.astral_craft.creator.tab.skin", 0xFF5664B7),
        SKIN_RARITY("gui.astral_craft.creator.tab.rarity", 0xFF8B63B7),
        APPEARANCE("gui.astral_craft.creator.tab.appearance", 0xFF4F9D69);

        private final String translationKey;
        private final int color;

        EditorTab(String translationKey, int color) {
            this.translationKey = translationKey;
            this.color = color;
        }
    }

    private static JsonObject typed(Identifier type) {
        JsonObject result = new JsonObject();
        result.addProperty("type", type.toString());
        return result;
    }

    private enum EventEffectFilter {
        ALL("gui.astral_craft.creator.event.filter.all"),
        GENERAL("gui.astral_craft.creator.event.filter.general"),
        BOARD("gui.astral_craft.creator.event.filter.board");

        private final String translationKey;

        EventEffectFilter(String translationKey) { this.translationKey = translationKey; }

        private EventEffectFilter next() {
            EventEffectFilter[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }

        private boolean accepts(EffectType type) {
            return this == ALL || (this == BOARD) == type.boardOnly();
        }
    }

    private enum TargetScope {
        TRIGGER_PLAYER(AstralEventTargetScopes.TRIGGER_PLAYER, "gui.astral_craft.creator.target.trigger_player"),
        ALL_PLAYERS(AstralEventTargetScopes.ALL_PLAYERS, "gui.astral_craft.creator.target.all_players"),
        DIMENSION_PLAYERS(AstralEventTargetScopes.DIMENSION_PLAYERS, "gui.astral_craft.creator.target.dimension_players"),
        NEARBY_PLAYERS(AstralEventTargetScopes.NEARBY_PLAYERS, "gui.astral_craft.creator.target.nearby_players"),
        NEARBY_ENTITIES(AstralEventTargetScopes.NEARBY_ENTITIES, "gui.astral_craft.creator.target.nearby_entities"),
        NEARBY_MOBS(AstralEventTargetScopes.NEARBY_MOBS, "gui.astral_craft.creator.target.nearby_mobs"),
        NEARBY_ANIMALS(AstralEventTargetScopes.NEARBY_ANIMALS, "gui.astral_craft.creator.target.nearby_animals");

        private final Identifier id;
        private final String translationKey;

        TargetScope(Identifier id, String translationKey) {
            this.id = id;
            this.translationKey = translationKey;
        }

        private TargetScope next() {
            TargetScope[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }

        private boolean playersOnly() {
            return this == TRIGGER_PLAYER || this == ALL_PLAYERS || this == DIMENSION_PLAYERS || this == NEARBY_PLAYERS;
        }
    }

    @FunctionalInterface
    private interface DraftEncoder {
        JsonObject encode(AstralDataEditorScreen screen, EditBox arg1, EditBox arg2, EditBox arg3) throws EditorException;
    }

    private record DraftDefaults(String first, String second, String third) {}

    private enum ConditionType {
        NONE("gui.astral_craft.creator.condition.none", "", "", "", new DraftDefaults("", "", ""), (screen, a, b, c) -> null),
        DIMENSION("gui.astral_craft.creator.condition.dimension", "gui.astral_craft.creator.arg.dimensions", "gui.astral_craft.creator.arg.inverted", "",
                new DraftDefaults("minecraft:overworld", "false", ""), (screen, a, b, c) -> {
            JsonObject result = typed(AstralCraft.prefix("dimension"));
            JsonArray values = new JsonArray();
            for (String value : screen.requireCsv(a, "gui.astral_craft.creator.error.identifier_list", true)) values.add(value);
            result.add("dimensions", values);
            result.addProperty("inverted", screen.requireBoolean(b, "gui.astral_craft.creator.error.boolean"));
            return result;
        }),
        RANDOM_CHANCE("gui.astral_craft.creator.condition.random_chance", "gui.astral_craft.creator.arg.chance", "", "",
                new DraftDefaults("1.0", "", ""), (screen, a, b, c) -> {
            JsonObject result = typed(AstralCraft.prefix("random_chance"));
            result.addProperty("chance", screen.requireDouble(a, 0.0D, 1.0D, "gui.astral_craft.creator.error.chance"));
            return result;
        }),
        HEALTH("gui.astral_craft.creator.condition.health", "gui.astral_craft.creator.arg.min", "gui.astral_craft.creator.arg.max", "gui.astral_craft.creator.arg.max_percent",
                new DraftDefaults("0", "20", "1.0"), (screen, a, b, c) -> {
            JsonObject result = typed(AstralCraft.prefix("health"));
            result.addProperty("min", screen.requireDouble(a, 0.0D, Float.MAX_VALUE, "gui.astral_craft.creator.error.number"));
            result.addProperty("max", screen.requireDouble(b, 0.0D, Float.MAX_VALUE, "gui.astral_craft.creator.error.number"));
            result.addProperty("max_percent", screen.requireDouble(c, 0.0D, 1.0D, "gui.astral_craft.creator.error.number"));
            return result;
        }),
        HAS_ITEM("gui.astral_craft.creator.condition.has_item", "gui.astral_craft.creator.arg.item", "gui.astral_craft.creator.arg.count", "gui.astral_craft.creator.arg.inverted",
                new DraftDefaults("minecraft:apple", "1", "false"), (screen, a, b, c) -> {
            JsonObject result = typed(AstralCraft.prefix("has_item"));
            result.addProperty("id", screen.requireIdentifier(a, "gui.astral_craft.creator.error.identifier").toString());
            result.addProperty("count", screen.requireInt(b, 1, Integer.MAX_VALUE, "gui.astral_craft.creator.error.number"));
            result.addProperty("inverted", screen.requireBoolean(c, "gui.astral_craft.creator.error.boolean"));
            return result;
        }),
        HAS_EFFECT("gui.astral_craft.creator.condition.has_effect", "gui.astral_craft.creator.arg.effect", "gui.astral_craft.creator.arg.inverted", "",
                new DraftDefaults("minecraft:speed", "false", ""), (screen, a, b, c) -> {
            JsonObject result = typed(AstralCraft.prefix("has_effect"));
            result.addProperty("effect", screen.requireIdentifier(a, "gui.astral_craft.creator.error.identifier").toString());
            result.addProperty("inverted", screen.requireBoolean(b, "gui.astral_craft.creator.error.boolean"));
            return result;
        }),
        WEATHER("gui.astral_craft.creator.condition.weather", "gui.astral_craft.creator.arg.raining", "gui.astral_craft.creator.arg.thundering", "gui.astral_craft.creator.arg.require_exact",
                new DraftDefaults("true", "false", "false"), (screen, a, b, c) -> {
            JsonObject result = typed(AstralCraft.prefix("weather"));
            result.addProperty("raining", screen.requireBoolean(a, "gui.astral_craft.creator.error.boolean"));
            result.addProperty("thundering", screen.requireBoolean(b, "gui.astral_craft.creator.error.boolean"));
            result.addProperty("require_exact", screen.requireBoolean(c, "gui.astral_craft.creator.error.boolean"));
            return result;
        }),
        TIME_OF_DAY("gui.astral_craft.creator.condition.time", "gui.astral_craft.creator.arg.min", "gui.astral_craft.creator.arg.max", "",
                new DraftDefaults("0", "23999", ""), (screen, a, b, c) -> {
            JsonObject result = typed(AstralCraft.prefix("time_of_day"));
            result.addProperty("min", screen.requireLong(a, 0L, Long.MAX_VALUE, "gui.astral_craft.creator.error.number"));
            result.addProperty("max", screen.requireLong(b, 0L, Long.MAX_VALUE, "gui.astral_craft.creator.error.number"));
            return result;
        }),
        ENTITY_TYPE("gui.astral_craft.creator.condition.entity_type", "gui.astral_craft.creator.arg.entity_types", "gui.astral_craft.creator.arg.inverted", "",
                new DraftDefaults("minecraft:zombie", "false", ""), (screen, a, b, c) -> {
            JsonObject result = typed(AstralCraft.prefix("entity_type"));
            JsonArray values = new JsonArray();
            for (String value : screen.requireCsv(a, "gui.astral_craft.creator.error.identifier_list", true)) values.add(value);
            result.add("entity_types", values);
            result.addProperty("inverted", screen.requireBoolean(b, "gui.astral_craft.creator.error.boolean"));
            return result;
        }),
        ENTITY_CATEGORY("gui.astral_craft.creator.condition.entity_category", "gui.astral_craft.creator.arg.categories", "gui.astral_craft.creator.arg.inverted", "",
                new DraftDefaults("player", "false", ""), (screen, a, b, c) -> {
            JsonObject result = typed(AstralCraft.prefix("entity_category"));
            JsonArray values = new JsonArray();
            for (String value : screen.requireCsv(a, "gui.astral_craft.creator.error.categories", false)) values.add(value.toLowerCase(Locale.ROOT));
            result.add("categories", values);
            result.addProperty("inverted", screen.requireBoolean(b, "gui.astral_craft.creator.error.boolean"));
            return result;
        });

        private final String translationKey;
        private final String arg1;
        private final String arg2;
        private final String arg3;
        private final DraftDefaults defaults;
        private final DraftEncoder encoder;

        ConditionType(String translationKey, String arg1, String arg2, String arg3, DraftDefaults defaults, DraftEncoder encoder) {
            this.translationKey = translationKey;
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.arg3 = arg3;
            this.defaults = defaults;
            this.encoder = encoder;
        }

        private ConditionType next() {
            ConditionType[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }
    }

    private enum EffectType {
        NONE("gui.astral_craft.creator.effect.none", "", "", "", new DraftDefaults("", "", ""), target -> true, (screen, a, b, c) -> null),
        GIVE_ITEM("gui.astral_craft.creator.effect.give_item", "gui.astral_craft.creator.arg.item", "gui.astral_craft.creator.arg.count", "",
                new DraftDefaults("minecraft:apple", "1", ""), TargetScope::playersOnly, (screen, a, b, c) -> {
            JsonObject result = typed(AstralCraft.prefix("give_item"));
            result.addProperty("id", screen.requireIdentifier(a, "gui.astral_craft.creator.error.identifier").toString());
            result.addProperty("count", screen.requireInt(b, 1, Integer.MAX_VALUE, "gui.astral_craft.creator.error.number"));
            return result;
        }),
        DROP_ITEM("gui.astral_craft.creator.effect.drop_item", "gui.astral_craft.creator.arg.item", "gui.astral_craft.creator.arg.count", "",
                new DraftDefaults("minecraft:apple", "1", ""), target -> true, (screen, a, b, c) -> {
            JsonObject result = typed(AstralCraft.prefix("drop_item"));
            result.addProperty("id", screen.requireIdentifier(a, "gui.astral_craft.creator.error.identifier").toString());
            result.addProperty("count", screen.requireInt(b, 1, Integer.MAX_VALUE, "gui.astral_craft.creator.error.number"));
            return result;
        }),
        DAMAGE("gui.astral_craft.creator.effect.damage", "gui.astral_craft.creator.arg.amount", "", "",
                new DraftDefaults("1.0", "", ""), target -> true, (screen, a, b, c) -> {
            JsonObject result = typed(AstralCraft.prefix("damage"));
            result.addProperty("amount", screen.requireDouble(a, 0.0D, Float.MAX_VALUE, "gui.astral_craft.creator.error.number"));
            return result;
        }),
        HEAL("gui.astral_craft.creator.effect.heal", "gui.astral_craft.creator.arg.amount", "", "",
                new DraftDefaults("1.0", "", ""), target -> true, (screen, a, b, c) -> {
            JsonObject result = typed(AstralCraft.prefix("heal"));
            result.addProperty("amount", screen.requireDouble(a, 0.0D, Float.MAX_VALUE, "gui.astral_craft.creator.error.number"));
            return result;
        }),
        ADD_EXPERIENCE("gui.astral_craft.creator.effect.experience", "gui.astral_craft.creator.arg.amount", "", "",
                new DraftDefaults("5", "", ""), TargetScope::playersOnly, (screen, a, b, c) -> {
            JsonObject result = typed(AstralCraft.prefix("add_experience"));
            result.addProperty("amount", screen.requireInt(a, Integer.MIN_VALUE, Integer.MAX_VALUE, "gui.astral_craft.creator.error.number"));
            return result;
        }),
        ADD_HUNGER("gui.astral_craft.creator.effect.hunger", "gui.astral_craft.creator.arg.nutrition", "gui.astral_craft.creator.arg.saturation", "",
                new DraftDefaults("2", "0.2", ""), TargetScope::playersOnly, (screen, a, b, c) -> {
            JsonObject result = typed(AstralCraft.prefix("add_hunger"));
            result.addProperty("nutrition", screen.requireInt(a, Integer.MIN_VALUE, Integer.MAX_VALUE, "gui.astral_craft.creator.error.number"));
            result.addProperty("saturation", screen.requireDouble(b, -Float.MAX_VALUE, Float.MAX_VALUE, "gui.astral_craft.creator.error.number"));
            return result;
        }),
        SET_FIRE("gui.astral_craft.creator.effect.fire", "gui.astral_craft.creator.arg.seconds", "", "",
                new DraftDefaults("5", "", ""), target -> true, (screen, a, b, c) -> {
            JsonObject result = typed(AstralCraft.prefix("set_fire"));
            result.addProperty("seconds", screen.requireInt(a, 0, Integer.MAX_VALUE, "gui.astral_craft.creator.error.number"));
            return result;
        }),
        MOB_EFFECT("gui.astral_craft.creator.effect.mob_effect", "gui.astral_craft.creator.arg.effect", "gui.astral_craft.creator.arg.duration", "gui.astral_craft.creator.arg.amplifier",
                new DraftDefaults("minecraft:speed", "200", "0"), target -> true, (screen, a, b, c) -> {
            JsonObject result = typed(AstralCraft.prefix("effect"));
            result.addProperty("effect", screen.requireIdentifier(a, "gui.astral_craft.creator.error.identifier").toString());
            result.addProperty("duration_ticks", screen.requireInt(b, 1, Integer.MAX_VALUE, "gui.astral_craft.creator.error.number"));
            result.addProperty("amplifier", screen.requireInt(c, 0, 255, "gui.astral_craft.creator.error.number"));
            return result;
        }),
        CLEAR_EFFECT("gui.astral_craft.creator.effect.clear_effect", "gui.astral_craft.creator.arg.effect", "gui.astral_craft.creator.arg.all", "",
                new DraftDefaults("minecraft:speed", "false", ""), target -> true, (screen, a, b, c) -> {
            JsonObject result = typed(AstralCraft.prefix("clear_effect"));
            result.addProperty("effect", screen.requireIdentifier(a, "gui.astral_craft.creator.error.identifier").toString());
            result.addProperty("all", screen.requireBoolean(b, "gui.astral_craft.creator.error.boolean"));
            return result;
        }),
        BOARD_COINS("gui.astral_craft.creator.effect.board_coins", "gui.astral_craft.creator.arg.amount", "", "",
                new DraftDefaults("5", "", ""), target -> true, (screen, a, b, c) -> {
            JsonObject result = typed(AstralCraft.prefix("board_coins"));
            result.addProperty("amount", screen.requireInt(a, Integer.MIN_VALUE, Integer.MAX_VALUE, "gui.astral_craft.creator.error.number"));
            return result;
        }),
        BOARD_MOVE_DICE("gui.astral_craft.creator.effect.board_move_dice", "gui.astral_craft.creator.arg.count", "", "",
                new DraftDefaults("1", "", ""), target -> true, (screen, a, b, c) -> {
            JsonObject result = typed(AstralCraft.prefix("board_move_dice"));
            result.addProperty("extra_dice", screen.requireInt(a, 0, 64, "gui.astral_craft.creator.error.number"));
            return result;
        }),
        BOARD_SET_HEALTH("gui.astral_craft.creator.effect.board_set_health", "gui.astral_craft.creator.arg.amount", "", "",
                new DraftDefaults("1", "", ""), target -> true, (screen, a, b, c) -> {
            JsonObject result = typed(AstralCraft.prefix("board_set_health"));
            result.addProperty("health", screen.requireInt(a, 0, Integer.MAX_VALUE, "gui.astral_craft.creator.error.number"));
            return result;
        }),
        BOARD_TRAP("gui.astral_craft.creator.effect.board_trap", "gui.astral_craft.creator.arg.trap_type", "", "",
                new DraftDefaults("demolition", "", ""), target -> true, (screen, a, b, c) -> {
            JsonObject result = typed(AstralCraft.prefix("board_trap"));
            result.addProperty("trap_type", screen.requireText(a, "gui.astral_craft.creator.error.value").toLowerCase(Locale.ROOT));
            return result;
        });

        private final String translationKey;
        private final String arg1;
        private final String arg2;
        private final String arg3;
        private final DraftDefaults defaults;
        private final Predicate<TargetScope> targetPredicate;
        private final DraftEncoder encoder;

        EffectType(String translationKey, String arg1, String arg2, String arg3, DraftDefaults defaults,
                   Predicate<TargetScope> targetPredicate, DraftEncoder encoder) {
            this.translationKey = translationKey;
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.arg3 = arg3;
            this.defaults = defaults;
            this.targetPredicate = targetPredicate;
            this.encoder = encoder;
        }

        private EffectType next(EventEffectFilter filter) {
            EffectType[] values = values();
            EffectType candidate = this;
            do candidate = values[(candidate.ordinal() + 1) % values.length]; while (!filter.accepts(candidate));
            return candidate;
        }

        private boolean boardOnly() {
            return this.name().startsWith("BOARD_");
        }

        private boolean compatibleWith(TargetScope target) {
            return this.targetPredicate.test(target);
        }
    }

    private enum AppearanceType {
        CARD_BACK("gui.astral_craft.creator.appearance.card_back", "textures/gui/cards/back", IMAGE_EXTENSIONS),
        DICE("gui.astral_craft.creator.appearance.dice", "textures/entity/dice/skins", IMAGE_EXTENSIONS),
        CHARACTER_SKIN("gui.astral_craft.creator.appearance.character_skin", "textures/entity/character", Set.of("png"));

        private final String translationKey;
        private final String directory;
        private final Set<String> extensions;

        AppearanceType(String translationKey, String directory, Set<String> extensions) {
            this.translationKey = translationKey;
            this.directory = directory;
            this.extensions = extensions;
        }

        private AppearanceType next() {
            AppearanceType[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }

        private Set<String> extensions() {
            return this.extensions;
        }

        private boolean accepts(String extension) {
            return this.extensions.contains(extension);
        }
    }

    private static class ConditionDraft {
        private ConditionType type = ConditionType.NONE;
        private EditBox arg1;
        private EditBox arg2;
        private EditBox arg3;

        private void create(AstralDataEditorScreen screen) {
            boolean firstCreate = this.arg1 == null;
            String first = firstCreate ? this.type.defaults.first() : this.arg1.getValue();
            String second = firstCreate ? this.type.defaults.second() : this.arg2.getValue();
            String third = firstCreate ? this.type.defaults.third() : this.arg3.getValue();
            this.arg1 = screen.createBox("gui.astral_craft.creator.arg.value", 160);
            this.arg2 = screen.createBox("gui.astral_craft.creator.arg.value", 160);
            this.arg3 = screen.createBox("gui.astral_craft.creator.arg.value", 160);
            this.set(first, second, third);
        }

        private void nextType() {
            this.type = this.type.next();
            this.resetDefaults();
        }

        private void resetDefaults() {
            if (this.arg1 == null) return;
            this.set(this.type.defaults.first(), this.type.defaults.second(), this.type.defaults.third());
        }

        private void set(String first, String second, String third) {
            this.arg1.setValue(first);
            this.arg2.setValue(second);
            this.arg3.setValue(third);
        }

        private void position(int x, int y, int width, boolean tabVisible) {
            int argW = Math.max(35, (width * 3 - GAP * 2) / 3);
            this.positionBox(this.arg1, x, y, argW, tabVisible && !this.type.arg1.isEmpty());
            this.positionBox(this.arg2, x + argW + GAP, y, argW, tabVisible && !this.type.arg2.isEmpty());
            this.positionBox(this.arg3, x + (argW + GAP) * 2, y, argW, tabVisible && !this.type.arg3.isEmpty());
        }

        private void positionBox(EditBox box, int x, int y, int width, boolean visible) {
            box.setPosition(x, y);
            box.setWidth(width);
            box.setVisible(visible);
            box.active = visible;
        }

        private void renderLabels(GuiGraphicsExtractor graphics, AstralDataEditorScreen screen, int x, int y, int width) {
            int argW = Math.max(35, (width * 3 - GAP * 2) / 3);
            if (!this.type.arg1.isEmpty()) screen.label(graphics, this.type.arg1, x, y);
            if (!this.type.arg2.isEmpty()) screen.label(graphics, this.type.arg2, x + argW + GAP, y);
            if (!this.type.arg3.isEmpty()) screen.label(graphics, this.type.arg3, x + (argW + GAP) * 2, y);
        }

        private JsonObject toJson(AstralDataEditorScreen screen) throws EditorException {
            return this.type.encoder.encode(screen, this.arg1, this.arg2, this.arg3);
        }
    }

    private static class EffectDraft {
        private EffectType type = EffectType.NONE;
        private EditBox arg1;
        private EditBox arg2;
        private EditBox arg3;

        private void create(AstralDataEditorScreen screen) {
            boolean firstCreate = this.arg1 == null;
            String first = firstCreate ? this.type.defaults.first() : this.arg1.getValue();
            String second = firstCreate ? this.type.defaults.second() : this.arg2.getValue();
            String third = firstCreate ? this.type.defaults.third() : this.arg3.getValue();
            this.arg1 = screen.createBox("gui.astral_craft.creator.arg.value", 160);
            this.arg2 = screen.createBox("gui.astral_craft.creator.arg.value", 160);
            this.arg3 = screen.createBox("gui.astral_craft.creator.arg.value", 160);
            this.set(first, second, third);
        }

        private void nextType(EventEffectFilter filter) {
            this.type = this.type.next(filter);
            this.resetDefaults();
        }

        private void resetDefaults() {
            if (this.arg1 == null) return;
            this.set(this.type.defaults.first(), this.type.defaults.second(), this.type.defaults.third());
        }

        private void set(String first, String second, String third) {
            this.arg1.setValue(first);
            this.arg2.setValue(second);
            this.arg3.setValue(third);
        }

        private void position(int x, int y, int width, boolean tabVisible) {
            int argW = Math.max(35, (width * 3 - GAP * 2) / 3);
            this.positionBox(this.arg1, x, y, argW, tabVisible && !this.type.arg1.isEmpty());
            this.positionBox(this.arg2, x + argW + GAP, y, argW, tabVisible && !this.type.arg2.isEmpty());
            this.positionBox(this.arg3, x + (argW + GAP) * 2, y, argW, tabVisible && !this.type.arg3.isEmpty());
        }

        private void positionBox(EditBox box, int x, int y, int width, boolean visible) {
            box.setPosition(x, y);
            box.setWidth(width);
            box.setVisible(visible);
            box.active = visible;
        }

        private void renderLabels(GuiGraphicsExtractor graphics, AstralDataEditorScreen screen, int x, int y, int width) {
            int argW = Math.max(35, (width * 3 - GAP * 2) / 3);
            if (!this.type.arg1.isEmpty()) screen.label(graphics, this.type.arg1, x, y);
            if (!this.type.arg2.isEmpty()) screen.label(graphics, this.type.arg2, x + argW + GAP, y);
            if (!this.type.arg3.isEmpty()) screen.label(graphics, this.type.arg3, x + (argW + GAP) * 2, y);
        }

        private JsonObject toJson(AstralDataEditorScreen screen) throws EditorException {
            return this.type.encoder.encode(screen, this.arg1, this.arg2, this.arg3);
        }

        private boolean compatibleWith(TargetScope target) {
            return this.type.compatibleWith(target);
        }
    }

    private static class EditorException extends Exception {
        private final String translationKey;
        private final Object[] args;

        private EditorException(String translationKey, Object... args) {
            this.translationKey = translationKey;
            this.args = args;
        }
    }

    private record EditorLayout(int panelX, int panelY, int panelW, int panelH) {
        private int panelRight() { return this.panelX + this.panelW; }
        private int contentX() { return this.panelX + 10; }
        private int contentW() { return this.panelW - 20; }
        private int tabY() { return this.panelY + 27; }
        private int tabW() { return Math.max(72, (this.contentW() - GAP * 3) / 4); }
        private int tabX(int index) { return this.contentX() + index * (this.tabW() + GAP); }
        private int packLabelY() { return this.tabY() + TAB_HEIGHT + 10; }
        private int packBoxY() { return this.packLabelY() + 10; }
        private int packNameW() { return Math.max(100, this.contentW() / 4); }
        private int pathX() { return this.contentX() + this.packNameW() + GAP; }
        private int choosePathW() { return Math.clamp(this.contentW() / 6, 72, 110); }
        private int choosePathX() { return this.contentX() + this.contentW() - this.choosePathW(); }
        private int pathW() { return this.choosePathX() - GAP - this.pathX(); }
        private int rowsTop() { return this.packBoxY() + FIELD_HEIGHT + 19; }
        private int rowLabelY(int row) { return this.rowsTop() + row * 34; }
        private int rowBoxY(int row) { return this.rowLabelY(row) + 10; }
        private int halfW() { return (this.contentW() - GAP) / 2; }
        private int leftX() { return this.contentX(); }
        private int rightX() { return this.contentX() + this.halfW() + GAP; }
        private int eventSmallW() { return Math.max(48, (this.contentW() - GAP * 5) / 6); }
        private int eventSmallX(int index) { return this.contentX() + index * (this.eventSmallW() + GAP); }
        private int eventSlotsY() { return this.rowBoxY(2) + FIELD_HEIGHT + 24; }
        private int conditionX() { return this.contentX(); }
        private int effectX() { return this.contentX() + (this.contentW() - GAP) / 2 + GAP; }
        private int slotTypeW() { return (this.contentW() - GAP) / 2; }
        private int slotArgW() { return this.slotTypeW(); }
        private int slotY(int index) { return this.eventSlotsY() + index * SLOT_HEIGHT; }
        private int eventViewportBottom() { return this.statusY() - 4; }
        private int eventViewportH() { return Math.max(24, this.eventViewportBottom() - this.eventSlotsY()); }
        private int effectFilterW() { return Math.min(120, this.slotTypeW()); }
        private int effectFilterX() { return this.effectX() + this.slotTypeW() - this.effectFilterW(); }
        private int rarityColorW() { return Math.max(72, (this.contentW() - GAP * 2) / 3); }
        private int rarityColorX(int index) { return this.contentX() + index * (this.rarityColorW() + GAP); }
        private int actionY() { return this.panelY + this.panelH - 31; }
        private int statusY() { return this.actionY() - 15; }
        private int actionButtonW() { return Math.max(82, (this.contentW() - GAP) / 2); }
        private int exportX() { return this.contentX(); }
        private int closeX() { return this.contentX() + this.actionButtonW() + GAP; }
    }
}
