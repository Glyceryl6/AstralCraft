package com.astral_craft.client.gui.editor;

import com.astral_craft.AstralCraft;
import com.astral_craft.client.gui.components.AstralFancyButton;
import com.astral_craft.common.gameplay.event.type.AstralEventTargetScopes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

@SuppressWarnings({"unused", "SameParameterValue"})
public class AstralDataEditorScreen extends Screen {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int GAP = 5;
    private static final int FIELD_HEIGHT = 20;
    private static final int TAB_HEIGHT = 22;
    private static final int CHECKBOX_GAP = 4;
    private static final int ARGUMENT_ROW_HEIGHT = 34;
    private static final int EVENT_SECTION_GAP = 8;
    private static final int SUGGESTION_ROW_HEIGHT = 22;
    private static final int SUGGESTION_ITEM_CELL = 20;
    private static final int SUGGESTION_VISIBLE_ROWS = 6;
    private static final int SUGGESTION_SCROLLBAR_WIDTH = 6;
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
    private final Map<EditBox, Component> boxHints = new IdentityHashMap<>();
    private final Map<EditBox, RegistrySuggestionKind> registrySuggestionKinds = new IdentityHashMap<>();
    private final Map<RegistrySuggestionKind, List<Identifier>> registrySuggestionCache = new EnumMap<>(RegistrySuggestionKind.class);
    private EditBox registrySuggestionBox;
    private RegistrySuggestionKind registrySuggestionKind;
    private List<Identifier> registrySuggestions = List.of();
    private int registrySuggestionIndex;
    private String registrySuggestionQuery = "";
    private int registrySuggestionScrollRow;
    private boolean registrySuggestionScrollbarDragging;

    private EditBox packNameBox;

    private EditBox eventIdBox;
    private EditBox eventNameBox;
    private EditBox eventNameKeyBox;
    private EditBox eventDescriptionBox;
    private EditBox eventDescriptionKeyBox;
    private EditBox eventTextureBox;
    private EditBox eventCooldownBox;
    private EditBox eventChanceBox;
    private EditBox eventRadiusBox;
    private TargetScope eventTarget = TargetScope.TRIGGER_PLAYER;
    private boolean eventTriggers = true;
    private boolean eventBroadcast;
    private final int[] tabScrollOffsets = new int[EditorTab.values().length];
    private boolean tabScrollbarDragging;
    private Checkbox eventTriggersCheckbox;
    private Checkbox eventBroadcastCheckbox;
    private final ConditionDraft[] eventConditions = Arrays.stream(ConditionType.values()).filter(type -> type != ConditionType.NONE)
            .map(ConditionDraft::new).toArray(ConditionDraft[]::new);
    private final EffectDraft[] eventEffects = Arrays.stream(EffectType.values()).filter(type -> type != EffectType.NONE)
            .map(EffectDraft::new).toArray(EffectDraft[]::new);

    private EditBox skinEntryIdBox;
    private EditBox skinNameBox;
    private EditBox skinNameKeyBox;
    private EditBox skinCharacterBox;
    private EditBox skinIdBox;
    private EditBox skinTextureBox;
    private EditBox skinRarityBox;
    private EditBox skinModelBox;
    private EditBox skinAnimationSetBox;
    private boolean skinUnlocked;
    private Checkbox skinUnlockedCheckbox;

    private EditBox rarityIdBox;
    private EditBox rarityNameBox;
    private EditBox rarityNameKeyBox;
    private EditBox rarityBorderBox;
    private EditBox rarityBadgeBox;
    private EditBox rarityTextBox;
    private RgbColorControl rarityBorderColor;
    private RgbColorControl rarityBadgeColor;
    private RgbColorControl rarityTextColor;

    private EditBox appearanceNamespaceBox;
    private EditBox appearanceNameBox;
    private EditBox appearanceSourceBox;
    private AppearanceType appearanceType = AppearanceType.CARD_BACK;

    private boolean exportEvent = true;
    private boolean exportSkin;
    private boolean exportRarity;
    private boolean exportAppearance;
    private boolean exportSeparate;
    private Checkbox exportEventCheckbox;
    private Checkbox exportSkinCheckbox;
    private Checkbox exportRarityCheckbox;
    private Checkbox exportAppearanceCheckbox;
    private Checkbox exportSeparateCheckbox;

    public AstralDataEditorScreen() {
        super(Component.translatable("gui.astral_craft.creator.title"));
        Path gameDirectory = Minecraft.getInstance().gameDirectory.toPath().toAbsolutePath().normalize();
        this.exportRoot = gameDirectory.resolve("astral_creator_exports");
    }

    @Override
    protected void init() {
        EditorLayout layout = this.layout();
        this.boxHints.clear();
        this.registrySuggestionKinds.clear();
        this.registrySuggestionCache.clear();
        this.packNameBox = this.recreateBox(this.packNameBox, "gui.astral_craft.creator.pack_name_hint", 64, "astral_creator_pack");
        this.eventIdBox = this.recreateBox(this.eventIdBox, "gui.astral_craft.creator.event.id_hint", 160, "example:welcome_event");
        this.eventNameBox = this.recreateBox(this.eventNameBox, "gui.astral_craft.creator.event.name_hint", 160, "");
        this.eventNameKeyBox = this.recreateBox(this.eventNameKeyBox, "gui.astral_craft.creator.localization_key_hint", 256, "");
        this.eventDescriptionBox = this.recreateBox(this.eventDescriptionBox, "gui.astral_craft.creator.event.description_hint", 256, "");
        this.eventDescriptionKeyBox = this.recreateBox(this.eventDescriptionKeyBox, "gui.astral_craft.creator.localization_key_hint", 256, "");
        this.eventTextureBox = this.recreateBox(this.eventTextureBox, "gui.astral_craft.creator.event.texture_hint", 256, "astral_craft:textures/gui/cards/event.png");
        this.eventCooldownBox = this.recreateBox(this.eventCooldownBox, "gui.astral_craft.creator.event.cooldown_hint", 12, "600");
        this.eventChanceBox = this.recreateBox(this.eventChanceBox, "gui.astral_craft.creator.event.chance_hint", 12, "1.0");
        this.eventRadiusBox = this.recreateBox(this.eventRadiusBox, "gui.astral_craft.creator.event.radius_hint", 12, "16.0");
        int eventToggleW = Math.max(1, (layout.tabContentW() - GAP) / 2);
        this.eventTriggersCheckbox = this.createCheckbox("gui.astral_craft.creator.event.auto_trigger", this.eventTriggers, value -> this.eventTriggers = value, eventToggleW);
        this.eventBroadcastCheckbox = this.createCheckbox("gui.astral_craft.creator.event.broadcast", this.eventBroadcast, value -> this.eventBroadcast = value, eventToggleW);
        for (ConditionDraft condition : this.eventConditions) condition.create(this, layout.tabContentW());
        for (EffectDraft effect : this.eventEffects) effect.create(this, layout.tabContentW());
        this.configureRegistrySuggestions();
        this.skinEntryIdBox = this.recreateBox(this.skinEntryIdBox, "gui.astral_craft.creator.skin.entry_id_hint", 160, "example:skins/character/default");
        this.skinNameBox = this.recreateBox(this.skinNameBox, "gui.astral_craft.creator.skin.name_hint", 160, "");
        this.skinNameKeyBox = this.recreateBox(this.skinNameKeyBox, "gui.astral_craft.creator.localization_key_hint", 256, "");
        this.skinCharacterBox = this.recreateBox(this.skinCharacterBox, "gui.astral_craft.creator.skin.character_hint", 160, "astral_craft:default");
        this.skinIdBox = this.recreateBox(this.skinIdBox, "gui.astral_craft.creator.skin.skin_id_hint", 96, "default");
        this.skinTextureBox = this.recreateBox(this.skinTextureBox, "gui.astral_craft.creator.skin.texture_hint", 256, "example:entity/character/skin_custom");
        this.skinRarityBox = this.recreateBox(this.skinRarityBox, "gui.astral_craft.creator.skin.rarity_hint", 160, "none");
        this.skinModelBox = this.recreateBox(this.skinModelBox, "gui.astral_craft.creator.skin.model_hint", 160, "");
        this.skinAnimationSetBox = this.recreateBox(this.skinAnimationSetBox, "gui.astral_craft.creator.skin.animation_set_hint", 160, "");
        this.skinUnlockedCheckbox = this.createCheckbox("gui.astral_craft.creator.skin.unlocked", this.skinUnlocked, value -> this.skinUnlocked = value, layout.halfW());
        this.rarityIdBox = this.recreateBox(this.rarityIdBox, "gui.astral_craft.creator.rarity.id_hint", 160, "example:rare");
        this.rarityNameBox = this.recreateBox(this.rarityNameBox, "gui.astral_craft.creator.rarity.name_hint", 160, "");
        this.rarityNameKeyBox = this.recreateBox(this.rarityNameKeyBox, "gui.astral_craft.creator.localization_key_hint", 256, "");
        this.rarityBorderBox = this.recreateBox(this.rarityBorderBox, "gui.astral_craft.creator.rarity.border_hint", 10, "#FF8ACB");
        this.rarityBadgeBox = this.recreateBox(this.rarityBadgeBox, "gui.astral_craft.creator.rarity.badge_hint", 10, "#F05BAE");
        this.rarityTextBox = this.recreateBox(this.rarityTextBox, "gui.astral_craft.creator.rarity.text_hint", 10, "#FFFFFF");
        this.rarityBorderColor = new RgbColorControl(this.rarityBorderBox);
        this.rarityBadgeColor = new RgbColorControl(this.rarityBadgeBox);
        this.rarityTextColor = new RgbColorControl(this.rarityTextBox);
        this.appearanceNamespaceBox = this.recreateBox(this.appearanceNamespaceBox, "gui.astral_craft.creator.appearance.namespace_hint", 64, "example");
        this.appearanceNameBox = this.recreateBox(this.appearanceNameBox, "gui.astral_craft.creator.appearance.name_hint", 96, "custom");
        this.appearanceSourceBox = this.recreateBox(this.appearanceSourceBox, "gui.astral_craft.creator.appearance.source_hint", 1024, "");
        this.exportEventCheckbox = this.createCheckbox("gui.astral_craft.creator.export.content.event", this.exportEvent, value -> this.exportEvent = value, layout.tabContentW());
        this.exportSkinCheckbox = this.createCheckbox("gui.astral_craft.creator.export.content.skin", this.exportSkin, value -> this.exportSkin = value, layout.tabContentW());
        this.exportRarityCheckbox = this.createCheckbox("gui.astral_craft.creator.export.content.rarity", this.exportRarity, value -> this.exportRarity = value, layout.tabContentW());
        this.exportAppearanceCheckbox = this.createCheckbox("gui.astral_craft.creator.export.content.appearance", this.exportAppearance, value -> this.exportAppearance = value, layout.tabContentW());
        this.exportSeparateCheckbox = this.createCheckbox("gui.astral_craft.creator.export.separate", this.exportSeparate, value -> this.exportSeparate = value, layout.tabContentW());
        this.updateWidgets(layout);
    }

    private EditBox recreateBox(EditBox previous, String hintKey, int maxLength, String fallback) {
        String value = previous == null ? fallback : previous.getValue();
        EditBox box = this.createBox(hintKey, maxLength);
        box.setValue(value);
        return box;
    }

    private EditBox createBox(String hintKey, int maxLength) {
        Component hint = Component.translatable(hintKey);
        EditBox box = this.addRenderableWidget(new EditBox(this.font, 0, 0, 100, FIELD_HEIGHT, hint));
        box.setMaxLength(maxLength);
        box.setHint(hint);
        this.boxHints.put(box, hint);
        return box;
    }

    private void configureRegistrySuggestions() {
        for (ConditionDraft draft : this.eventConditions) {
            RegistrySuggestionKind kind = switch (draft.type) {
                case DIMENSION -> RegistrySuggestionKind.DIMENSION;
                case HAS_ITEM -> RegistrySuggestionKind.ITEM;
                case HAS_EFFECT -> RegistrySuggestionKind.MOB_EFFECT;
                case ENTITY_TYPE -> RegistrySuggestionKind.ENTITY_TYPE;
                case BLOCK_AT -> RegistrySuggestionKind.BLOCK;
                default -> null;
            };
            if (kind != null) this.registrySuggestionKinds.put(draft.arg1, kind);
        }

        for (EffectDraft draft : this.eventEffects) {
            RegistrySuggestionKind kind = switch (draft.type) {
                case GIVE_ITEM, DROP_ITEM -> RegistrySuggestionKind.ITEM;
                case MOB_EFFECT, CLEAR_EFFECT -> RegistrySuggestionKind.MOB_EFFECT;
                default -> null;
            };
            if (kind != null) this.registrySuggestionKinds.put(draft.arg1, kind);
        }
    }


    private Checkbox createCheckbox(String key, boolean selected, Consumer<Boolean> changed, int maxWidth) {
        return this.addRenderableWidget(Checkbox.builder(Component.translatable(key), this.font)
                .selected(selected)
                .onValueChange((checkbox, value) -> {
                    changed.accept(value);
                    this.onDynamicLayoutChanged();
                })
                .maxWidth(Math.max(40, maxWidth))
                .build());
    }

    private void onDynamicLayoutChanged() {
        EditorLayout layout = this.layout();
        this.setTabScrollOffset(layout, this.tabScrollOffset());
        this.updateWidgets(layout);
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        EditorLayout layout = this.layout();
        this.setTabScrollOffset(layout, this.tabScrollOffset());
        this.updateWidgets(layout);
        AstralFancyButton.renderOutlinedBox(graphics, layout.panelX(), layout.panelY(), layout.panelW(), layout.panelH(),
                0xEE151723, 0xE8545B70, 0xD0101018, 1, 2);
        graphics.fill(layout.panelX(), layout.panelY(), layout.panelRight(), layout.panelY() + 3, 0xFFE83CA8);
        graphics.centeredText(this.font, this.title, this.width / 2, layout.panelY() + 8, 0xFFFFFFFF);
        this.renderButton(graphics, layout.topCloseX(), layout.topCloseY(), layout.topCloseW(), 16,
                "gui.astral_craft.creator.close_short", mouseX, mouseY, 0xFF646477, false);
        this.renderTabs(graphics, layout, mouseX, mouseY);
        graphics.enableScissor(layout.tabContentX(), layout.tabViewportTop(), layout.tabContentRight(), layout.tabViewportBottom());
        switch (this.tab) {
            case EVENT -> this.renderEventTab(graphics, layout, mouseX, mouseY);
            case CHARACTER_SKIN -> this.renderSkinTab(graphics, layout);
            case SKIN_RARITY -> this.renderRarityTab(graphics, layout);
            case APPEARANCE -> this.renderAppearanceTab(graphics, layout, mouseX, mouseY);
            case EXPORT -> this.renderExportTab(graphics, layout, mouseX, mouseY);
        }

        graphics.disableScissor();
        this.renderTabScrollbar(graphics, layout);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        this.updateRegistrySuggestions();
        if (!this.registrySuggestions.isEmpty()) {
            graphics.nextStratum();
            this.renderRegistrySuggestions(graphics, mouseX, mouseY);
        } else {
            this.renderClippedHintTooltip(graphics, mouseX, mouseY);
        }
        if (this.registrySuggestionHovered(mouseX, mouseY)) graphics.requestCursor(CursorTypes.POINTING_HAND);
        else if (this.anyVisibleBoxHovered(mouseX, mouseY)) graphics.requestCursor(CursorTypes.IBEAM);
        else if (this.anyVisibleCheckboxHovered(mouseX, mouseY) || this.hoveredManualControl(layout, mouseX, mouseY)) graphics.requestCursor(CursorTypes.POINTING_HAND);
        else graphics.requestCursor(CursorTypes.ARROW);
    }

    private void renderClippedHintTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        for (Map.Entry<EditBox, Component> entry : this.boxHints.entrySet()) {
            EditBox box = entry.getKey();
            Component hint = entry.getValue();
            if (!box.visible || !box.getValue().isEmpty() || !box.isMouseOver(mouseX, mouseY)) continue;
            if (this.font.width(hint) <= box.getInnerWidth()) return;
            graphics.setTooltipForNextFrame(this.font, hint, mouseX, mouseY);
            return;
        }
    }

    private void updateRegistrySuggestions() {
        EditBox focused = null;
        RegistrySuggestionKind kind = null;
        for (Map.Entry<EditBox, RegistrySuggestionKind> entry : this.registrySuggestionKinds.entrySet()) {
            if (entry.getKey().visible && entry.getKey().isFocused()) {
                focused = entry.getKey();
                kind = entry.getValue();
                break;
            }
        }

        if (focused == null || kind == null) {
            this.closeRegistrySuggestions(false);
            return;
        }

        String value = focused.getValue();
        int comma = value.lastIndexOf(',');
        String token = value.substring(comma + 1).strip().toLowerCase(Locale.ROOT);
        boolean changedSource = focused != this.registrySuggestionBox || kind != this.registrySuggestionKind;
        if (!changedSource && token.equals(this.registrySuggestionQuery)) return;
        List<Identifier> matches = this.registrySuggestionCache.computeIfAbsent(kind, suggestionKind -> suggestionKind.entries(this).stream()
                        .sorted(Comparator.comparing(Identifier::toString)).toList()).stream()
                .filter(id -> !id.toString().equalsIgnoreCase(token))
                .filter(id -> token.isEmpty() || id.toString().toLowerCase(Locale.ROOT).startsWith(token)
                        || id.getPath().toLowerCase(Locale.ROOT).startsWith(token))
                .toList();
        if (changedSource) {
            this.registrySuggestionIndex = 0;
            this.registrySuggestionScrollRow = 0;
        }
        this.registrySuggestionBox = focused;
        this.registrySuggestionKind = kind;
        this.registrySuggestionQuery = token;
        this.registrySuggestions = matches;
        this.registrySuggestionIndex = Math.clamp(this.registrySuggestionIndex, 0, Math.max(0, matches.size() - 1));
        this.ensureSelectedRegistrySuggestionVisible();
    }

    private void renderRegistrySuggestions(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.registrySuggestionBox == null || this.registrySuggestionKind == null || this.registrySuggestions.isEmpty()) return;
        SuggestionBounds bounds = this.registrySuggestionBounds();
        AstralFancyButton.renderOutlinedBox(graphics, bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                0xF0181B26, 0xFF6E7588, 0x00000000, 1, 0);
        graphics.enableScissor(bounds.x() + 1, bounds.y() + 1, bounds.x() + bounds.width() - SUGGESTION_SCROLLBAR_WIDTH - 1, bounds.y() + bounds.height() - 1);
        if (this.registrySuggestionKind == RegistrySuggestionKind.ITEM) this.renderItemRegistrySuggestions(graphics, bounds, mouseX, mouseY);
        else this.renderListRegistrySuggestions(graphics, bounds, mouseX, mouseY);
        graphics.disableScissor();
        this.renderRegistrySuggestionScrollbar(graphics, bounds);
    }

    private void renderItemRegistrySuggestions(GuiGraphicsExtractor graphics, SuggestionBounds bounds, int mouseX, int mouseY) {
        int columns = this.registrySuggestionColumns(bounds);
        int start = this.registrySuggestionScrollRow * columns;
        int end = Math.min(this.registrySuggestions.size(), start + columns * SUGGESTION_VISIBLE_ROWS);
        for (int index = start; index < end; index++) {
            int visibleIndex = index - start;
            int x = bounds.x() + 3 + visibleIndex % columns * SUGGESTION_ITEM_CELL;
            int y = bounds.y() + 3 + visibleIndex / columns * SUGGESTION_ITEM_CELL;
            boolean hovered = this.isInside(mouseX, mouseY, x, y, SUGGESTION_ITEM_CELL, SUGGESTION_ITEM_CELL);
            graphics.fill(x, y, x + SUGGESTION_ITEM_CELL, y + SUGGESTION_ITEM_CELL, 0xFF252A35);
            graphics.outline(x, y, SUGGESTION_ITEM_CELL, SUGGESTION_ITEM_CELL, 0xFF4B5362);
            if (index == this.registrySuggestionIndex || hovered) {
                graphics.fill(x + 1, y + 1, x + SUGGESTION_ITEM_CELL - 1, y + SUGGESTION_ITEM_CELL - 1, 0x66566DB3);
                graphics.outline(x, y, SUGGESTION_ITEM_CELL, SUGGESTION_ITEM_CELL, 0xFFA9B9E8);
            }
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(this.registrySuggestions.get(index)));
            graphics.item(stack, x + 2, y + 2);
            if (hovered) graphics.setTooltipForNextFrame(this.font, stack, mouseX, mouseY);
        }
    }

    private void renderListRegistrySuggestions(GuiGraphicsExtractor graphics, SuggestionBounds bounds, int mouseX, int mouseY) {
        int start = this.registrySuggestionScrollRow;
        int end = Math.min(this.registrySuggestions.size(), start + SUGGESTION_VISIBLE_ROWS);
        for (int index = start; index < end; index++) {
            Identifier id = this.registrySuggestions.get(index);
            int rowY = bounds.y() + (index - start) * SUGGESTION_ROW_HEIGHT;
            boolean hovered = this.isInside(mouseX, mouseY, bounds.x() + 1, rowY, bounds.width() - SUGGESTION_SCROLLBAR_WIDTH - 2, SUGGESTION_ROW_HEIGHT);
            if (index == this.registrySuggestionIndex || hovered) graphics.fill(bounds.x() + 1, rowY + 1, bounds.x() + bounds.width() - SUGGESTION_SCROLLBAR_WIDTH - 1, rowY + SUGGESTION_ROW_HEIGHT - 1, 0x554F6A9B);
            int textX = bounds.x() + 5;
            if (this.registrySuggestionKind == RegistrySuggestionKind.MOB_EFFECT) {
                var effect = BuiltInRegistries.MOB_EFFECT.getValue(id);
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Gui.getMobEffectSprite(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect)), bounds.x() + 3, rowY + 2, 18, 18);
                textX += 20;
                Component name = effect.getDisplayName();
                int nameWidth = Math.min(this.font.width(name), Math.max(0, (bounds.width() - SUGGESTION_SCROLLBAR_WIDTH - (textX - bounds.x()) - 10) * 3 / 5));
                String shownName = this.font.plainSubstrByWidth(name.getString(), nameWidth);
                graphics.text(this.font, shownName, textX, rowY + 3, 0xFFF0F4FA);
                int idX = textX + nameWidth + 6;
                String shownId = this.font.plainSubstrByWidth(id.toString(), Math.max(1, bounds.width() - SUGGESTION_SCROLLBAR_WIDTH - (idX - bounds.x()) - 5));
                graphics.text(this.font, shownId, idX, rowY + 3, 0xFF98A2B3);
                if (hovered) graphics.setComponentTooltipForNextFrame(this.font, List.of(name, Component.literal(id.toString())), mouseX, mouseY);
            } else {
                String shown = this.font.plainSubstrByWidth(id.toString(), Math.max(1, bounds.width() - SUGGESTION_SCROLLBAR_WIDTH - (textX - bounds.x()) - 5));
                graphics.text(this.font, shown, textX, rowY + 6, 0xFFE6EDF7);
            }
        }
    }

    private SuggestionBounds registrySuggestionBounds() {
        if (this.registrySuggestionBox == null || this.registrySuggestionKind == null) return new SuggestionBounds(0, 0, 0, 0);
        int width = this.registrySuggestionKind == RegistrySuggestionKind.ITEM
                ? Math.clamp(Math.max(this.registrySuggestionBox.getWidth(), 188), 108, Math.max(108, this.width - 8))
                : Math.clamp(Math.max(this.registrySuggestionBox.getWidth(), this.registrySuggestionKind == RegistrySuggestionKind.MOB_EFFECT ? 280 : 180), 80, Math.max(80, this.width - 8));
        int rows = Math.min(SUGGESTION_VISIBLE_ROWS, this.registrySuggestionTotalRows(width));
        int height = this.registrySuggestionKind == RegistrySuggestionKind.ITEM ? rows * SUGGESTION_ITEM_CELL + 6 : rows * SUGGESTION_ROW_HEIGHT;
        int x = Math.clamp(this.registrySuggestionBox.getX(), 4, Math.max(4, this.width - width - 4));
        int below = this.registrySuggestionBox.getY() + this.registrySuggestionBox.getHeight() + 1;
        int y = below + height <= this.height - 4 ? below : this.registrySuggestionBox.getY() - height - 1;
        return new SuggestionBounds(x, Math.max(4, y), width, height);
    }

    private int registrySuggestionColumns(SuggestionBounds bounds) {
        return Math.max(1, (bounds.width() - SUGGESTION_SCROLLBAR_WIDTH - 6) / SUGGESTION_ITEM_CELL);
    }

    private int registrySuggestionTotalRows(int width) {
        if (this.registrySuggestionKind != RegistrySuggestionKind.ITEM) return this.registrySuggestions.size();
        int columns = Math.max(1, (width - SUGGESTION_SCROLLBAR_WIDTH - 6) / SUGGESTION_ITEM_CELL);
        return (this.registrySuggestions.size() + columns - 1) / columns;
    }

    private int maxRegistrySuggestionScrollRow() {
        if (this.registrySuggestions.isEmpty()) return 0;
        SuggestionBounds bounds = this.registrySuggestionBounds();
        return Math.max(0, this.registrySuggestionTotalRows(bounds.width()) - SUGGESTION_VISIBLE_ROWS);
    }

    private boolean registrySuggestionHovered(double mouseX, double mouseY) {
        if (this.registrySuggestions.isEmpty()) return false;
        SuggestionBounds bounds = this.registrySuggestionBounds();
        return this.isInside(mouseX, mouseY, bounds.x(), bounds.y(), bounds.width(), bounds.height());
    }

    private boolean handleRegistrySuggestionClick(double mouseX, double mouseY) {
        if (!this.registrySuggestionHovered(mouseX, mouseY)) return false;
        SuggestionBounds bounds = this.registrySuggestionBounds();
        if (this.isInside(mouseX, mouseY, bounds.x() + bounds.width() - SUGGESTION_SCROLLBAR_WIDTH, bounds.y(), SUGGESTION_SCROLLBAR_WIDTH, bounds.height()) && this.maxRegistrySuggestionScrollRow() > 0) {
            this.registrySuggestionScrollbarDragging = true;
            this.updateRegistrySuggestionScrollFromMouse(mouseY);
            return true;
        }
        int index;
        if (this.registrySuggestionKind == RegistrySuggestionKind.ITEM) {
            int columns = this.registrySuggestionColumns(bounds);
            int column = (int) ((mouseX - bounds.x() - 3) / SUGGESTION_ITEM_CELL);
            int row = (int) ((mouseY - bounds.y() - 3) / SUGGESTION_ITEM_CELL);
            if (column < 0 || column >= columns || row < 0 || row >= SUGGESTION_VISIBLE_ROWS) return true;
            index = (this.registrySuggestionScrollRow + row) * columns + column;
        } else {
            int row = (int) ((mouseY - bounds.y()) / SUGGESTION_ROW_HEIGHT);
            index = this.registrySuggestionScrollRow + row;
        }
        if (index < 0 || index >= this.registrySuggestions.size()) return true;
        this.registrySuggestionIndex = index;
        this.applyRegistrySuggestion(this.registrySuggestions.get(index));
        return true;
    }

    private void renderRegistrySuggestionScrollbar(GuiGraphicsExtractor graphics, SuggestionBounds bounds) {
        int max = this.maxRegistrySuggestionScrollRow();
        if (max <= 0) return;
        int x = bounds.x() + bounds.width() - SUGGESTION_SCROLLBAR_WIDTH;
        graphics.fill(x, bounds.y(), x + SUGGESTION_SCROLLBAR_WIDTH, bounds.y() + bounds.height(), 0x66505A68);
        int totalRows = max + SUGGESTION_VISIBLE_ROWS;
        int thumb = Math.max(14, bounds.height() * SUGGESTION_VISIBLE_ROWS / totalRows);
        int y = bounds.y() + (bounds.height() - thumb) * this.registrySuggestionScrollRow / max;
        graphics.fill(x + 1, y, x + SUGGESTION_SCROLLBAR_WIDTH - 1, y + thumb, 0xFFE83CA8);
    }

    private void updateRegistrySuggestionScrollFromMouse(double mouseY) {
        int max = this.maxRegistrySuggestionScrollRow();
        if (max <= 0) {
            this.registrySuggestionScrollRow = 0;
            return;
        }
        SuggestionBounds bounds = this.registrySuggestionBounds();
        int totalRows = max + SUGGESTION_VISIBLE_ROWS;
        int thumb = Math.max(14, bounds.height() * SUGGESTION_VISIBLE_ROWS / totalRows);
        double progress = Math.clamp((mouseY - bounds.y() - thumb * 0.5D) / Math.max(1.0D, bounds.height() - thumb), 0.0D, 1.0D);
        this.registrySuggestionScrollRow = (int) Math.round(progress * max);
    }

    private void ensureSelectedRegistrySuggestionVisible() {
        if (this.registrySuggestions.isEmpty() || this.registrySuggestionKind == null) return;
        SuggestionBounds bounds = this.registrySuggestionBounds();
        int row = this.registrySuggestionKind == RegistrySuggestionKind.ITEM
                ? this.registrySuggestionIndex / this.registrySuggestionColumns(bounds)
                : this.registrySuggestionIndex;
        if (row < this.registrySuggestionScrollRow) this.registrySuggestionScrollRow = row;
        else if (row >= this.registrySuggestionScrollRow + SUGGESTION_VISIBLE_ROWS) this.registrySuggestionScrollRow = row - SUGGESTION_VISIBLE_ROWS + 1;
        this.registrySuggestionScrollRow = Math.clamp(this.registrySuggestionScrollRow, 0, this.maxRegistrySuggestionScrollRow());
    }

    private void applyRegistrySuggestion(Identifier suggestion) {
        if (this.registrySuggestionBox == null) return;
        String value = this.registrySuggestionBox.getValue();
        int comma = value.lastIndexOf(',');
        String prefix = comma < 0 ? "" : value.substring(0, comma + 1) + " ";
        this.registrySuggestionBox.setValue(prefix + suggestion);
        this.registrySuggestionBox.setCursorPosition(this.registrySuggestionBox.getValue().length());
        this.closeRegistrySuggestions(false);
    }

    private void closeRegistrySuggestions(boolean clearFocus) {
        if (clearFocus && this.registrySuggestionBox != null) this.registrySuggestionBox.setFocused(false);
        this.registrySuggestionBox = null;
        this.registrySuggestionKind = null;
        this.registrySuggestions = List.of();
        this.registrySuggestionIndex = 0;
        this.registrySuggestionQuery = "";
        this.registrySuggestionScrollRow = 0;
        this.registrySuggestionScrollbarDragging = false;
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

    private void renderEventTab(GuiGraphicsExtractor graphics, EditorLayout layout, int mouseX, int mouseY) {
        int y = this.tabStartY(layout);
        this.label(graphics, "gui.astral_craft.creator.event.id", layout.leftX(), y);
        this.label(graphics, "gui.astral_craft.creator.event.name", layout.rightX(), y);
        y += 34;
        this.label(graphics, "gui.astral_craft.creator.event.description", layout.leftX(), y);
        this.label(graphics, "gui.astral_craft.creator.event.texture", layout.rightX(), y);
        y += 34;
        this.label(graphics, "gui.astral_craft.creator.localization_name_key", layout.leftX(), y);
        this.label(graphics, "gui.astral_craft.creator.localization_description_key", layout.rightX(), y);
        int readW = Math.max(54, layout.halfW() / 3);
        this.renderButton(graphics, layout.leftX() + layout.halfW() - readW, y + 10, readW, FIELD_HEIGHT,
                "gui.astral_craft.creator.localization_read_short", mouseX, mouseY, 0xFF5664B7, false);
        this.renderButton(graphics, layout.rightX() + layout.halfW() - readW, y + 10, readW, FIELD_HEIGHT,
                "gui.astral_craft.creator.localization_read_short", mouseX, mouseY, 0xFF5664B7, false);
        y += 34;
        this.label(graphics, "gui.astral_craft.creator.event.cooldown", layout.eventMetaX(0), y);
        this.label(graphics, "gui.astral_craft.creator.event.chance", layout.eventMetaX(1), y);
        this.label(graphics, "gui.astral_craft.creator.event.radius", layout.eventMetaX(2), y);
        this.renderButton(graphics, layout.eventMetaX(3), y + 10, layout.eventMetaW(), FIELD_HEIGHT,
                this.eventTarget.translationKey, mouseX, mouseY, 0xFF5664B7, false);
        y += 36 + this.eventToggleHeight() + EVENT_SECTION_GAP;
        y = this.renderConditionSection(graphics, layout, y);
        y += EVENT_SECTION_GAP;
        y = this.renderEffectSection(graphics, layout, y, false, mouseX, mouseY);
        y += EVENT_SECTION_GAP;
        y = this.renderEffectSection(graphics, layout, y, true, mouseX, mouseY);
        if (this.status != null && !this.status.getString().isBlank()) {
            int color = this.statusError ? 0xFFFF8F9E : 0xFF8FE2A9;
            this.renderWrappedText(graphics, this.status, layout.tabContentX(), y + 6, layout.tabContentW(), color);
        }
    }

    private int renderConditionSection(GuiGraphicsExtractor graphics, EditorLayout layout, int y) {
        graphics.text(this.font, Component.translatable("gui.astral_craft.creator.event.conditions"), layout.tabContentX(), y, 0xFFD7E4F2);
        y += 14;
        y = this.conditionCheckboxFlowEnd(layout, y);
        for (ConditionDraft draft : this.eventConditions) {
            if (!draft.hasArguments()) continue;
            y += 4;
            draft.renderArgumentLabels(graphics, this, layout.tabContentX(), y, layout.tabContentW());
            y += draft.argumentBlockHeight();
        }
        return y;
    }

    private int renderEffectSection(GuiGraphicsExtractor graphics, EditorLayout layout, int y, boolean boardOnly, int mouseX, int mouseY) {
        String key = boardOnly ? "gui.astral_craft.creator.event.board_effects" : "gui.astral_craft.creator.event.effects";
        graphics.text(this.font, Component.translatable(key), layout.tabContentX(), y, 0xFFD7E4F2);
        y += 14;
        y = this.effectCheckboxFlowEnd(layout, y, boardOnly);
        for (EffectDraft draft : this.eventEffects) {
            if (draft.type.boardOnly() != boardOnly || !draft.hasArguments()) continue;
            y += 4;
            draft.renderArgumentLabels(graphics, this, layout.tabContentX(), y, layout.tabContentW(), mouseX, mouseY);
            y += draft.argumentBlockHeight();
        }
        return y;
    }

    private int eventToggleHeight() {
        int trigger = this.eventTriggersCheckbox == null ? FIELD_HEIGHT : this.eventTriggersCheckbox.getHeight();
        int broadcast = this.eventBroadcastCheckbox == null ? FIELD_HEIGHT : this.eventBroadcastCheckbox.getHeight();
        return Math.max(trigger, broadcast);
    }

    private void renderSkinTab(GuiGraphicsExtractor graphics, EditorLayout layout) {
        int y = this.tabStartY(layout);
        this.label(graphics, "gui.astral_craft.creator.skin.entry_id", layout.leftX(), y);
        this.label(graphics, "gui.astral_craft.creator.skin.name", layout.rightX(), y);
        y += 34;
        this.label(graphics, "gui.astral_craft.creator.localization_key", layout.leftX(), y);
        this.renderButton(graphics, layout.rightX(), y + 10, layout.halfW(), FIELD_HEIGHT,
                "gui.astral_craft.creator.localization_read", 0, 0, 0xFF5664B7, false);
        y += 34;
        this.label(graphics, "gui.astral_craft.creator.skin.character", layout.leftX(), y);
        this.label(graphics, "gui.astral_craft.creator.skin.skin_id", layout.rightX(), y);
        y += 34;
        this.label(graphics, "gui.astral_craft.creator.skin.texture", layout.leftX(), y);
        this.label(graphics, "gui.astral_craft.creator.skin.rarity", layout.rightX(), y);
        y += 34;
        this.label(graphics, "gui.astral_craft.creator.skin.model", layout.leftX(), y);
        this.label(graphics, "gui.astral_craft.creator.skin.animation_set", layout.rightX(), y);
        y += 34 + Math.max(FIELD_HEIGHT, this.skinUnlockedCheckbox == null ? FIELD_HEIGHT : this.skinUnlockedCheckbox.getHeight()) + 8;
        this.renderWrappedText(graphics, Component.translatable("gui.astral_craft.creator.skin.help"), layout.tabContentX(), y, layout.tabContentW(), 0xFF9AA8BA);
    }

    private void renderRarityTab(GuiGraphicsExtractor graphics, EditorLayout layout) {
        int y = this.tabStartY(layout);
        this.label(graphics, "gui.astral_craft.creator.rarity.id", layout.leftX(), y);
        this.label(graphics, "gui.astral_craft.creator.rarity.name", layout.rightX(), y);
        y += 34;
        this.label(graphics, "gui.astral_craft.creator.localization_key", layout.leftX(), y);
        this.renderButton(graphics, layout.rightX(), y + 10, layout.halfW(), FIELD_HEIGHT,
                "gui.astral_craft.creator.localization_read", 0, 0, 0xFF5664B7, false);
        y += 38;
        y = this.renderColorControl(graphics, layout, y, "gui.astral_craft.creator.rarity.border", this.rarityBorderColor);
        y = this.renderColorControl(graphics, layout, y, "gui.astral_craft.creator.rarity.badge", this.rarityBadgeColor);
        y = this.renderColorControl(graphics, layout, y, "gui.astral_craft.creator.rarity.text", this.rarityTextColor);
        this.renderWrappedText(graphics, Component.translatable("gui.astral_craft.creator.rarity.help"), layout.tabContentX(), y + 4, layout.tabContentW(), 0xFF9AA8BA);
    }

    private int renderColorControl(GuiGraphicsExtractor graphics, EditorLayout layout, int y, String labelKey, RgbColorControl control) {
        this.label(graphics, labelKey, layout.tabContentX(), y);
        int previewX = layout.tabContentRight() - 44;
        graphics.fill(previewX, y + 10, previewX + 44, y + 30, 0xFF000000 | control.rgb());
        AstralFancyButton.renderOutlinedBox(graphics, previewX, y + 10, 44, 20, 0x00000000, 0xFFD7E4F2, 0x00000000, 1, 0);
        int sliderY = y + 36;
        for (int channel = 0; channel < 3; channel++) {
            String key = switch (channel) {
                case 0 -> "gui.astral_craft.creator.color.red";
                case 1 -> "gui.astral_craft.creator.color.green";
                default -> "gui.astral_craft.creator.color.blue";
            };
            graphics.text(this.font, Component.translatable(key), layout.tabContentX(), sliderY + channel * 16 + 3, 0xFFBBC7D7);
            int barX = layout.tabContentX() + 18;
            int barW = Math.max(40, layout.tabContentW() - 74);
            int barY = sliderY + channel * 16 + 3;
            graphics.fill(barX, barY, barX + barW, barY + 8, 0xFF252936);
            int value = control.channel(channel);
            int fillColor = switch (channel) { case 0 -> 0xFFE35454; case 1 -> 0xFF54C96A; default -> 0xFF5A75E8; };
            int fillW = Math.round(barW * value / 255.0F);
            graphics.fill(barX, barY, barX + fillW, barY + 8, fillColor);
            int knobX = barX + Math.round((barW - 2) * value / 255.0F);
            graphics.fill(knobX, barY - 2, knobX + 2, barY + 10, 0xFFFFFFFF);
            graphics.text(this.font, Component.literal(Integer.toString(value)), barX + barW + 5, barY, 0xFFD7E4F2);
        }
        return y + 88;
    }

    private void renderAppearanceTab(GuiGraphicsExtractor graphics, EditorLayout layout, int mouseX, int mouseY) {
        int y = this.tabStartY(layout);
        this.label(graphics, "gui.astral_craft.creator.appearance.namespace", layout.leftX(), y);
        this.label(graphics, "gui.astral_craft.creator.appearance.name", layout.rightX(), y);
        y += 34;
        this.label(graphics, "gui.astral_craft.creator.appearance.source", layout.leftX(), y);
        this.renderButton(graphics, layout.rightX(), y + 10, layout.halfW(), FIELD_HEIGHT,
                "gui.astral_craft.creator.appearance.choose_source", mouseX, mouseY, 0xFF5664B7, false);
        y += 34;
        this.renderButton(graphics, layout.leftX(), y, layout.halfW(), FIELD_HEIGHT,
                this.appearanceType.translationKey, mouseX, mouseY, 0xFFB05282, false);
        y += 32;
        this.renderWrappedText(graphics, Component.translatable("gui.astral_craft.creator.appearance.help"), layout.tabContentX(), y, layout.tabContentW(), 0xFF9AA8BA);
    }

    private void renderExportTab(GuiGraphicsExtractor graphics, EditorLayout layout, int mouseX, int mouseY) {
        int y = this.tabStartY(layout);
        this.label(graphics, "gui.astral_craft.creator.pack_name", layout.tabContentX(), y);
        y += 34;
        this.label(graphics, "gui.astral_craft.creator.export_root", layout.tabContentX(), y);
        String root = this.exportRoot == null ? "" : this.exportRoot.toString();
        int pathW = Math.max(1, layout.tabContentW() - layout.choosePathW() - GAP);
        graphics.text(this.font, this.font.plainSubstrByWidth(root, pathW - 4), layout.tabContentX(), y + 16, 0xFFBBC7D7);
        this.renderButton(graphics, layout.tabContentRight() - layout.choosePathW(), y + 10, layout.choosePathW(), FIELD_HEIGHT,
                "gui.astral_craft.creator.choose_folder", mouseX, mouseY, 0xFF5664B7, false);
        y += 34;

        this.label(graphics, "gui.astral_craft.creator.export.contents", layout.tabContentX(), y);
        y += 14 + this.exportCheckboxFlowHeight(layout);
        y += 8 + (this.exportSeparateCheckbox == null ? FIELD_HEIGHT : this.exportSeparateCheckbox.getHeight()) + 8;
        y = this.renderWrappedText(graphics, Component.translatable("gui.astral_craft.creator.export_packaging_note"),
                layout.tabContentX(), y, layout.tabContentW(), 0xFF9AA8BA);
        if (this.status != null && !this.status.getString().isBlank()) {
            y += 6;
            int color = this.statusError ? 0xFFFF8F9E : 0xFF8FE2A9;
            y = this.renderWrappedText(graphics, this.status, layout.tabContentX(), y, layout.tabContentW(), color);
        }
        y += 8;
        this.renderButton(graphics, layout.tabContentX(), y, layout.tabContentW(), 22,
                "gui.astral_craft.creator.export", mouseX, mouseY, 0xFF4F9D69, false);
    }

    private void label(GuiGraphicsExtractor graphics, String key, int x, int y) {
        graphics.text(this.font, Component.translatable(key), x, y, 0xFFD7E4F2);
    }

    private int renderWrappedText(GuiGraphicsExtractor graphics, Component text, int x, int y, int width, int color) {
        List<FormattedCharSequence> lines = this.font.split(text, Math.max(1, width));
        int lineY = y;
        for (FormattedCharSequence line : lines) {
            graphics.text(this.font, line, x, lineY, color);
            lineY += this.font.lineHeight + 2;
        }
        return lineY;
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
        if (this.handleRegistrySuggestionClick(mouseX, mouseY)) return true;
        if (!this.registrySuggestions.isEmpty() && (this.registrySuggestionBox == null || !this.registrySuggestionBox.isMouseOver(mouseX, mouseY))) this.closeRegistrySuggestions(true);
        if (this.isInside(mouseX, mouseY, layout.topCloseX(), layout.topCloseY(), layout.topCloseW(), 16)) {
            this.onClose();
            return true;
        }
        for (int index = 0; index < EditorTab.values().length; index++) {
            if (this.isInside(mouseX, mouseY, layout.tabX(index), layout.tabY(), layout.tabW(), TAB_HEIGHT)) {
                this.tab = EditorTab.values()[index];
                this.setTabScrollOffset(layout, this.tabScrollOffset());
                this.updateWidgets(layout);
                return true;
            }
        }
        if (this.handleTabScrollbarClick(layout, mouseX, mouseY)) return true;
        if (this.handleTabClick(layout, mouseX, mouseY)) return true;
        return super.mouseClicked(event, doubleClick);
    }

    private boolean handleTabClick(EditorLayout layout, double mouseX, double mouseY) {
        if (this.tab == EditorTab.EVENT) {
            for (EffectDraft draft : this.eventEffects) if (draft.handleSpecialClick(this, mouseX, mouseY)) return true;
            int start = this.tabStartY(layout);
            int keyY = start + 78;
            int readW = Math.max(54, layout.halfW() / 3);
            if (this.isTabControlVisible(layout, keyY, FIELD_HEIGHT) && this.isInside(mouseX, mouseY, layout.leftX() + layout.halfW() - readW, keyY, readW, FIELD_HEIGHT)) {
                this.readLocalization(this.eventNameKeyBox, this.eventNameBox);
                return true;
            }
            if (this.isTabControlVisible(layout, keyY, FIELD_HEIGHT) && this.isInside(mouseX, mouseY, layout.rightX() + layout.halfW() - readW, keyY, readW, FIELD_HEIGHT)) {
                this.readLocalization(this.eventDescriptionKeyBox, this.eventDescriptionBox);
                return true;
            }
            int targetY = start + 112;
            if (this.isTabControlVisible(layout, targetY, FIELD_HEIGHT)
                    && this.isInside(mouseX, mouseY, layout.eventMetaX(3), targetY, layout.eventMetaW(), FIELD_HEIGHT)) {
                this.eventTarget = this.eventTarget.next();
                return true;
            }
        } else if (this.tab == EditorTab.CHARACTER_SKIN) {
            int start = this.tabStartY(layout);
            int readY = start + 44;
            if (this.isTabControlVisible(layout, readY, FIELD_HEIGHT) && this.isInside(mouseX, mouseY, layout.rightX(), readY, layout.halfW(), FIELD_HEIGHT)) {
                this.readLocalization(this.skinNameKeyBox, this.skinNameBox);
                return true;
            }
        } else if (this.tab == EditorTab.SKIN_RARITY) {
            int start = this.tabStartY(layout);
            int readY = start + 44;
            if (this.isTabControlVisible(layout, readY, FIELD_HEIGHT) && this.isInside(mouseX, mouseY, layout.rightX(), readY, layout.halfW(), FIELD_HEIGHT)) {
                this.readLocalization(this.rarityNameKeyBox, this.rarityNameBox);
                return true;
            }
            return this.handleColorClick(layout, mouseX, mouseY);
        } else if (this.tab == EditorTab.APPEARANCE) {
            int start = this.tabStartY(layout);
            if (this.isTabControlVisible(layout, start + 44, FIELD_HEIGHT)
                    && this.isInside(mouseX, mouseY, layout.rightX(), start + 44, layout.halfW(), FIELD_HEIGHT)) {
                this.openAppearanceFileBrowser();
                return true;
            }
            if (this.isTabControlVisible(layout, start + 68, FIELD_HEIGHT)
                    && this.isInside(mouseX, mouseY, layout.leftX(), start + 68, layout.halfW(), FIELD_HEIGHT)) {
                this.appearanceType = this.appearanceType.next();
                return true;
            }
        } else if (this.tab == EditorTab.EXPORT) {
            int start = this.tabStartY(layout);
            int chooseY = start + 44;
            if (this.isTabControlVisible(layout, chooseY, FIELD_HEIGHT)
                    && this.isInside(mouseX, mouseY, layout.tabContentRight() - layout.choosePathW(), chooseY, layout.choosePathW(), FIELD_HEIGHT)) {
                this.openExportFolderBrowser();
                return true;
            }
            int exportY = this.exportActionY(layout);
            if (this.isTabControlVisible(layout, exportY, 22)
                    && this.isInside(mouseX, mouseY, layout.tabContentX(), exportY, layout.tabContentW(), 22)) {
                this.exportSelected();
                return true;
            }
        }

        return false;
    }

    private boolean isTabControlVisible(EditorLayout layout, int y, int height) {
        return y >= layout.tabViewportTop() && y + height <= layout.tabViewportBottom();
    }

    @Override
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dragX, double dragY) {
        if (event.button() == 0 && this.registrySuggestionScrollbarDragging) {
            this.updateRegistrySuggestionScrollFromMouse(event.y());
            return true;
        }
        if (event.button() == 0 && this.activeColorControl() != null) {
            this.updateActiveColor(this.layout(), event.x());
            return true;
        }
        if (event.button() == 0 && this.tabScrollbarDragging) {
            this.updateTabScrollFromMouse(this.layout(), event.y());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && this.registrySuggestionScrollbarDragging) {
            this.registrySuggestionScrollbarDragging = false;
            return true;
        }
        if (event.button() == 0 && this.activeColorControl() != null) {
            this.clearColorDragging();
            return true;
        }
        if (event.button() == 0 && this.tabScrollbarDragging) {
            this.tabScrollbarDragging = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (this.registrySuggestionHovered(mouseX, mouseY)) {
            int max = this.maxRegistrySuggestionScrollRow();
            if (max > 0) this.registrySuggestionScrollRow = Math.clamp(this.registrySuggestionScrollRow - (int) Math.signum(deltaY), 0, max);
            return true;
        }
        EditorLayout layout = this.layout();
        if (this.isInside(mouseX, mouseY, layout.tabContentX(), layout.tabViewportTop(), layout.contentRight() - layout.tabContentX(), layout.tabViewportH())) {
            this.setTabScrollOffset(layout, this.tabScrollOffset() - (int) Math.signum(deltaY) * 24);
            this.updateWidgets(layout);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    private boolean handleTabScrollbarClick(EditorLayout layout, double mouseX, double mouseY) {
        int max = this.maxTabScroll(layout);
        if (max <= 0 || !this.isInside(mouseX, mouseY, layout.tabScrollbarX(), layout.tabViewportTop(), layout.tabScrollbarW(), layout.tabViewportH())) return false;
        this.tabScrollbarDragging = true;
        this.updateTabScrollFromMouse(layout, mouseY);
        return true;
    }

    private void updateTabScrollFromMouse(EditorLayout layout, double mouseY) {
        int max = this.maxTabScroll(layout);
        if (max <= 0) {
            this.setTabScrollOffset(layout, 0);
            return;
        }
        int height = layout.tabViewportH();
        int thumb = Math.max(18, height * height / (height + max));
        double track = Math.max(1.0D, height - thumb);
        double progress = Math.clamp((mouseY - layout.tabViewportTop() - thumb * 0.5D) / track, 0.0D, 1.0D);
        this.setTabScrollOffset(layout, (int) Math.round(progress * max));
        this.updateWidgets(layout);
    }

    private void renderTabScrollbar(GuiGraphicsExtractor graphics, EditorLayout layout) {
        int max = this.maxTabScroll(layout);
        if (max <= 0) return;
        int x = layout.tabScrollbarX();
        int top = layout.tabViewportTop();
        int height = layout.tabViewportH();
        graphics.fill(x, top, x + layout.tabScrollbarW(), top + height, 0x554F5668);
        int thumb = Math.max(18, height * height / (height + max));
        int y = top + (height - thumb) * this.tabScrollOffset() / max;
        graphics.fill(x, y, x + layout.tabScrollbarW(), y + thumb, 0xFFE83CA8);
    }

    private int tabScrollOffset() {
        return this.tabScrollOffsets[this.tab.ordinal()];
    }

    private void setTabScrollOffset(EditorLayout layout, int value) {
        this.tabScrollOffsets[this.tab.ordinal()] = Math.clamp(value, 0, this.maxTabScroll(layout));
    }

    private int tabStartY(EditorLayout layout) {
        return layout.tabViewportTop() + 3 - this.tabScrollOffset();
    }

    private int maxTabScroll(EditorLayout layout) {
        return Math.max(0, this.tabContentHeight(layout) - layout.tabViewportH());
    }

    private int tabContentHeight(EditorLayout layout) {
        return switch (this.tab) {
            case EVENT -> this.eventContentHeight(layout);
            case CHARACTER_SKIN -> this.skinContentHeight(layout);
            case SKIN_RARITY -> this.rarityContentHeight(layout);
            case APPEARANCE -> this.appearanceContentHeight(layout);
            case EXPORT -> this.exportContentHeight(layout);
        };
    }

    private int eventContentHeight(EditorLayout layout) {
        int y = 3 + 34 + 34 + 34 + 36 + this.eventToggleHeight() + EVENT_SECTION_GAP;
        y = this.conditionSectionEnd(layout, y);
        y += EVENT_SECTION_GAP;
        y = this.effectSectionEnd(layout, y, false);
        y += EVENT_SECTION_GAP;
        y = this.effectSectionEnd(layout, y, true);
        if (this.status != null && !this.status.getString().isBlank()) {
            y += 6 + this.wrappedTextHeight(this.status, layout.tabContentW());
        }
        return y + 4;
    }

    private int skinContentHeight(EditorLayout layout) {
        int checkboxHeight = this.skinUnlockedCheckbox == null ? FIELD_HEIGHT : this.skinUnlockedCheckbox.getHeight();
        return 3 + 34 * 5 + Math.max(FIELD_HEIGHT, checkboxHeight) + 8
                + this.wrappedTextHeight(Component.translatable("gui.astral_craft.creator.skin.help"), layout.tabContentW()) + 4;
    }

    private int rarityContentHeight(EditorLayout layout) {
        return 3 + 34 + 38 + 88 * 3 + this.wrappedTextHeight(Component.translatable("gui.astral_craft.creator.rarity.help"), layout.tabContentW()) + 8;
    }

    private int appearanceContentHeight(EditorLayout layout) {
        return 3 + 34 * 2 + 32 + this.wrappedTextHeight(Component.translatable("gui.astral_craft.creator.appearance.help"), layout.tabContentW()) + 4;
    }

    private int exportContentHeight(EditorLayout layout) {
        int y = 3 + 34 + 34 + 14 + this.exportCheckboxFlowHeight(layout);
        y += 8 + (this.exportSeparateCheckbox == null ? FIELD_HEIGHT : this.exportSeparateCheckbox.getHeight()) + 8;
        y += this.wrappedTextHeight(Component.translatable("gui.astral_craft.creator.export_packaging_note"), layout.tabContentW());
        if (this.status != null && !this.status.getString().isBlank()) {
            y += 6 + this.wrappedTextHeight(this.status, layout.tabContentW());
        }
        return y + 8 + 22 + 4;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!this.registrySuggestions.isEmpty()) {
            if (event.key() == GLFW.GLFW_KEY_DOWN) {
                this.registrySuggestionIndex = (this.registrySuggestionIndex + 1) % this.registrySuggestions.size();
                this.ensureSelectedRegistrySuggestionVisible();
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_UP) {
                this.registrySuggestionIndex = (this.registrySuggestionIndex - 1 + this.registrySuggestions.size()) % this.registrySuggestions.size();
                this.ensureSelectedRegistrySuggestionVisible();
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_TAB || event.key() == GLFW.GLFW_KEY_ENTER) {
                this.applyRegistrySuggestion(this.registrySuggestions.get(this.registrySuggestionIndex));
                return true;
            }
        }
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
        this.minecraft.setScreen(null);
    }

    private void openExportFolderBrowser() {
        Path start = this.exportRoot != null && Files.isDirectory(this.exportRoot) ? this.exportRoot : this.minecraft.gameDirectory.toPath();
        this.minecraft.setScreen(AstralFileBrowserScreen.folder(this, start, path -> this.exportRoot = path));
    }

    private void openAppearanceFileBrowser() {
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
                String rawName = dot > 0 ? filename.substring(0, dot) : filename;
                String safeName = this.safeResourceName(rawName);
                this.appearanceNameBox.setValue(safeName);
                if (!safeName.equals(rawName)) {
                    this.status = Component.translatable("gui.astral_craft.creator.appearance.filename_normalized", rawName, safeName);
                    this.statusError = false;
                }
            }
        }));
    }

    private void openInventoryItemPicker(EffectDraft draft) {
        this.minecraft.setScreen(new AstralInventoryItemPickerScreen(this, stack -> {
            draft.addInventoryStack(stack);
            this.status = Component.translatable("gui.astral_craft.creator.item.added");
            this.statusError = false;
            this.onDynamicLayoutChanged();
        }));
    }

    private JsonObject encodeGiveItemEffect(EffectDraft draft) throws EditorException {
        JsonArray items = new JsonArray();
        for (ItemStack stack : draft.exportStacks(this)) {
            ItemStackTemplate template = ItemStackTemplate.fromNonEmptyStack(stack);
            var encoded = this.minecraft.getConnection() != null
                    ? ItemStackTemplate.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE, this.minecraft.getConnection().registryAccess()), template).result()
                    : ItemStackTemplate.CODEC.encodeStart(JsonOps.INSTANCE, template).result();
            if (encoded.isEmpty()) throw new EditorException("gui.astral_craft.creator.error.item_stack_encode");
            items.add(encoded.get());
        }
        JsonObject result = typed(AstralCraft.prefix("give_item"));
        result.add("items", items);
        return result;
    }

    private Component fieldContext(EditorTab tab, String fieldKey) {
        return Component.translatable(tab.translationKey).append(" / ").append(Component.translatable(fieldKey));
    }

    private Component eventContext(String itemKey, String sectionKey) {
        return Component.translatable(EditorTab.EVENT.translationKey).append(" / ")
                .append(Component.translatable(sectionKey)).append(" / ").append(Component.translatable(itemKey));
    }

    private <T> T validateField(EditorTab tab, String fieldKey, EditorSupplier<T> supplier) throws EditorException {
        try {
            return supplier.get();
        } catch (EditorException exception) {
            throw exception.withContext(this.fieldContext(tab, fieldKey));
        }
    }

    private void setEditorError(EditorException exception) {
        this.statusError = true;
        Component detail = Component.translatable(exception.translationKey, exception.args);
        this.status = exception.context == null ? detail
                : Component.translatable("gui.astral_craft.creator.error.field_context", exception.context, detail);
    }

    private void validateSelectedContent() throws EditorException, IOException {
        if (this.exportEvent) this.validateEventForm();
        if (this.exportSkin) this.validateSkinForm();
        if (this.exportRarity) this.validateRarityForm();
        if (this.exportAppearance) this.validateAppearanceForm();
    }

    private void validateEventForm() throws EditorException {
        this.validateField(EditorTab.EVENT, "gui.astral_craft.creator.event.id",
                () -> this.requireIdentifier(this.eventIdBox, "gui.astral_craft.creator.error.event_id"));
        this.validateField(EditorTab.EVENT, "gui.astral_craft.creator.event.name",
                () -> this.requireText(this.eventNameBox, "gui.astral_craft.creator.error.display_name"));
        this.validateField(EditorTab.EVENT, "gui.astral_craft.creator.event.description",
                () -> this.requireText(this.eventDescriptionBox, "gui.astral_craft.creator.error.description"));
        this.validateField(EditorTab.EVENT, "gui.astral_craft.creator.event.texture",
                () -> this.requireIdentifier(this.eventTextureBox, "gui.astral_craft.creator.error.texture"));
        this.validateField(EditorTab.EVENT, "gui.astral_craft.creator.event.cooldown",
                () -> this.requireInt(this.eventCooldownBox, 0, Integer.MAX_VALUE, "gui.astral_craft.creator.error.cooldown"));
        this.validateField(EditorTab.EVENT, "gui.astral_craft.creator.event.chance",
                () -> this.requireDouble(this.eventChanceBox, 0.0D, 1.0D, "gui.astral_craft.creator.error.chance"));
        this.validateField(EditorTab.EVENT, "gui.astral_craft.creator.event.radius",
                () -> this.requireDouble(this.eventRadiusBox, 1.0D, 1024.0D, "gui.astral_craft.creator.error.radius"));
        if (!this.eventNameKeyBox.getValue().isBlank()) this.validateField(EditorTab.EVENT, "gui.astral_craft.creator.localization_name_key",
                () -> this.localizationKeyOrDefault(this.eventNameKeyBox, "unused"));
        if (!this.eventDescriptionKeyBox.getValue().isBlank()) this.validateField(EditorTab.EVENT, "gui.astral_craft.creator.localization_description_key",
                () -> this.localizationKeyOrDefault(this.eventDescriptionKeyBox, "unused"));

        for (ConditionDraft draft : this.eventConditions) draft.toJson(this);
        int selectedEffects = 0;
        for (EffectDraft draft : this.eventEffects) {
            if (!draft.selected) continue;
            selectedEffects++;
            draft.toJson(this);
            if (!draft.compatibleWith(this.eventTarget)) {
                throw new EditorException("gui.astral_craft.creator.error.incompatible_target")
                        .withContext(this.eventContext(draft.type.translationKey,
                                draft.type.boardOnly() ? "gui.astral_craft.creator.event.board_effects" : "gui.astral_craft.creator.event.effects"));
            }
        }
        if (selectedEffects == 0) {
            throw new EditorException("gui.astral_craft.creator.error.effect_required")
                    .withContext(this.fieldContext(EditorTab.EVENT, "gui.astral_craft.creator.event.effects"));
        }
    }

    private void validateSkinForm() throws EditorException {
        this.validateField(EditorTab.CHARACTER_SKIN, "gui.astral_craft.creator.skin.entry_id",
                () -> this.requireIdentifier(this.skinEntryIdBox, "gui.astral_craft.creator.error.skin_entry_id"));
        this.validateField(EditorTab.CHARACTER_SKIN, "gui.astral_craft.creator.skin.name",
                () -> this.requireText(this.skinNameBox, "gui.astral_craft.creator.error.display_name"));
        this.validateField(EditorTab.CHARACTER_SKIN, "gui.astral_craft.creator.skin.character",
                () -> this.requireIdentifier(this.skinCharacterBox, "gui.astral_craft.creator.error.character_id"));
        this.validateField(EditorTab.CHARACTER_SKIN, "gui.astral_craft.creator.skin.skin_id",
                () -> this.requireSimplePath(this.skinIdBox, "gui.astral_craft.creator.error.skin_id"));
        this.validateField(EditorTab.CHARACTER_SKIN, "gui.astral_craft.creator.skin.texture",
                () -> this.requireIdentifier(this.skinTextureBox, "gui.astral_craft.creator.error.texture"));
        this.validateField(EditorTab.CHARACTER_SKIN, "gui.astral_craft.creator.skin.rarity",
                () -> this.requireText(this.skinRarityBox, "gui.astral_craft.creator.error.rarity"));
        if (!this.skinNameKeyBox.getValue().isBlank()) this.validateField(EditorTab.CHARACTER_SKIN, "gui.astral_craft.creator.localization_key",
                () -> this.localizationKeyOrDefault(this.skinNameKeyBox, "unused"));
        if (!this.skinModelBox.getValue().isBlank()) this.validateField(EditorTab.CHARACTER_SKIN, "gui.astral_craft.creator.skin.model",
                () -> this.requireIdentifier(this.skinModelBox, "gui.astral_craft.creator.error.model_id"));
        if (!this.skinAnimationSetBox.getValue().isBlank()) this.validateField(EditorTab.CHARACTER_SKIN, "gui.astral_craft.creator.skin.animation_set",
                () -> this.requireIdentifier(this.skinAnimationSetBox, "gui.astral_craft.creator.error.animation_set_id"));
    }

    private void validateRarityForm() throws EditorException {
        this.validateField(EditorTab.SKIN_RARITY, "gui.astral_craft.creator.rarity.id",
                () -> this.requireIdentifier(this.rarityIdBox, "gui.astral_craft.creator.error.rarity_id"));
        this.validateField(EditorTab.SKIN_RARITY, "gui.astral_craft.creator.rarity.name",
                () -> this.requireText(this.rarityNameBox, "gui.astral_craft.creator.error.display_name"));
        if (!this.rarityNameKeyBox.getValue().isBlank()) this.validateField(EditorTab.SKIN_RARITY, "gui.astral_craft.creator.localization_key",
                () -> this.localizationKeyOrDefault(this.rarityNameKeyBox, "unused"));
        this.validateField(EditorTab.SKIN_RARITY, "gui.astral_craft.creator.rarity.border",
                () -> this.requireColor(this.rarityBorderBox, "gui.astral_craft.creator.error.color"));
        this.validateField(EditorTab.SKIN_RARITY, "gui.astral_craft.creator.rarity.badge",
                () -> this.requireColor(this.rarityBadgeBox, "gui.astral_craft.creator.error.color"));
        this.validateField(EditorTab.SKIN_RARITY, "gui.astral_craft.creator.rarity.text",
                () -> this.requireColor(this.rarityTextBox, "gui.astral_craft.creator.error.color"));
    }

    private void validateAppearanceForm() throws EditorException, IOException {
        this.validateField(EditorTab.APPEARANCE, "gui.astral_craft.creator.appearance.namespace", () -> this.requireNamespace(this.appearanceNamespaceBox));
        this.validateField(EditorTab.APPEARANCE, "gui.astral_craft.creator.appearance.name",
                () -> this.requireSimplePath(this.appearanceNameBox, "gui.astral_craft.creator.error.appearance_name"));
        Path source = this.validateField(EditorTab.APPEARANCE, "gui.astral_craft.creator.appearance.source",
                () -> this.requirePath(this.appearanceSourceBox, "gui.astral_craft.creator.error.appearance_source"));
        if (!Files.isRegularFile(source)) {
            throw new EditorException("gui.astral_craft.creator.error.appearance_source")
                    .withContext(this.fieldContext(EditorTab.APPEARANCE, "gui.astral_craft.creator.appearance.source"));
        }
        String sourceName = source.getFileName().toString();
        int dot = sourceName.lastIndexOf('.');
        String extension = dot >= 0 ? sourceName.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
        if (!this.appearanceType.accepts(extension)) {
            throw new EditorException("gui.astral_craft.creator.error.appearance_source")
                    .withContext(this.fieldContext(EditorTab.APPEARANCE, "gui.astral_craft.creator.appearance.source"));
        }
        try {
            this.validateImageSource(source, extension);
        } catch (EditorException exception) {
            throw exception.withContext(this.fieldContext(EditorTab.APPEARANCE, "gui.astral_craft.creator.appearance.source"));
        }
    }

    private void exportSelected() {
        try {
            this.status = Component.empty();
            this.statusError = false;
            if (!this.exportEvent && !this.exportSkin && !this.exportRarity && !this.exportAppearance) {
                throw new EditorException("gui.astral_craft.creator.error.export_selection");
            }
            String packName = this.validateField(EditorTab.EXPORT, "gui.astral_craft.creator.pack_name",
                    () -> this.safePackName(this.requireText(this.packNameBox, "gui.astral_craft.creator.error.pack_name")));
            if (this.exportRoot == null) {
                throw new EditorException("gui.astral_craft.creator.error.export_root")
                        .withContext(this.fieldContext(EditorTab.EXPORT, "gui.astral_craft.creator.export_path"));
            }
            this.validateSelectedContent();
            Files.createDirectories(this.exportRoot);
            if (this.exportSeparate) this.exportSeparately(packName);
            else this.exportCombined(packName);
            this.status = Component.translatable("gui.astral_craft.creator.export_success", this.exportRoot.toString());
        } catch (EditorException exception) {
            this.setEditorError(exception);
        } catch (IOException | SecurityException exception) {
            this.statusError = true;
            this.status = Component.translatable("gui.astral_craft.creator.error.io", exception.getClass().getSimpleName());
        }
        this.onDynamicLayoutChanged();
    }

    private void exportCombined(String packName) throws IOException, EditorException {
        Path dataPack = this.exportRoot.resolve(packName + "_data");
        Path resourcePack = this.exportRoot.resolve(packName + "_resources");
        if (this.exportEvent) this.exportEvent(dataPack, resourcePack);
        if (this.exportSkin) this.exportSkin(dataPack, resourcePack);
        if (this.exportRarity) this.exportRarity(dataPack, resourcePack);
        if (this.exportAppearance) this.exportAppearance(resourcePack);
    }

    private void exportSeparately(String packName) throws IOException, EditorException {
        if (this.exportEvent) this.exportEvent(this.exportRoot.resolve(packName + "_event_data"), this.exportRoot.resolve(packName + "_event_resources"));
        if (this.exportSkin) this.exportSkin(this.exportRoot.resolve(packName + "_skin_data"), this.exportRoot.resolve(packName + "_skin_resources"));
        if (this.exportRarity) this.exportRarity(this.exportRoot.resolve(packName + "_rarity_data"), this.exportRoot.resolve(packName + "_rarity_resources"));
        if (this.exportAppearance) this.exportAppearance(this.exportRoot.resolve(packName + "_appearance_resources"));
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
        if (effects.isEmpty()) throw new EditorException("gui.astral_craft.creator.error.effect_required");
        for (EffectDraft draft : this.eventEffects) {
            if (!draft.compatibleWith(this.eventTarget)) throw new EditorException("gui.astral_craft.creator.error.incompatible_target");
        }

        String nameKey = this.localizationKeyOrDefault(this.eventNameKeyBox, "event." + id.getNamespace() + "." + id.getPath().replace('/', '.') + ".name");
        String descriptionKey = this.localizationKeyOrDefault(this.eventDescriptionKeyBox, "event." + id.getNamespace() + "." + id.getPath().replace('/', '.') + ".description");
        JsonObject target = new JsonObject();
        target.addProperty("scope", this.eventTarget.id.toString());
        target.addProperty("radius", radius);
        JsonObject root = new JsonObject();
        root.addProperty("id", id.toString());
        root.addProperty("name_key", nameKey);
        root.addProperty("description_key", descriptionKey);
        root.addProperty("texture", texture.toString());
        root.addProperty("triggers", this.eventTriggers);
        if (!conditions.isEmpty()) root.add("conditions", conditions);
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
        String nameKey = this.localizationKeyOrDefault(this.skinNameKeyBox, "character_skin." + entryId.getNamespace() + "." + entryId.getPath().replace('/', '.') + ".name");
        JsonObject root = new JsonObject();
        root.addProperty("character", character.toString());
        root.addProperty("id", skinId);
        root.addProperty("name_key", nameKey);
        root.addProperty("texture", texture.toString());
        root.addProperty("unlocked_by_default", this.skinUnlocked);
        root.addProperty("rarity", rarity);
        if (!this.skinModelBox.getValue().isBlank()) root.addProperty("model", this.requireIdentifier(this.skinModelBox, "gui.astral_craft.creator.error.model_id").toString());
        if (!this.skinAnimationSetBox.getValue().isBlank()) root.addProperty("animation_set", this.requireIdentifier(this.skinAnimationSetBox, "gui.astral_craft.creator.error.animation_set_id").toString());
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
        String nameKey = this.localizationKeyOrDefault(this.rarityNameKeyBox, "skin_rarity." + id.getNamespace() + "." + id.getPath().replace('/', '.') + ".name");
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
        this.validateImageSource(source, extension);
        this.ensureResourcePack(resourcePack);
        Path target = resourcePack.resolve("assets").resolve(namespace).resolve(this.appearanceType.directory).resolve(name + "." + extension);
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private void readLocalization(EditBox keyBox, EditBox valueBox) {
        String key = keyBox == null ? "" : keyBox.getValue().trim();
        if (key.isEmpty() || !Language.getInstance().has(key)) {
            this.status = Component.translatable("gui.astral_craft.creator.localization_missing", key);
            this.statusError = true;
            return;
        }
        valueBox.setValue(Component.translatable(key).getString());
        this.status = Component.translatable("gui.astral_craft.creator.localization_loaded", key);
        this.statusError = false;
    }

    private String localizationKeyOrDefault(EditBox box, String fallback) throws EditorException {
        if (box == null || box.getValue().isBlank()) return fallback;
        String key = box.getValue().trim();
        if (!key.matches("[a-z0-9_.\\-/]+")) throw new EditorException("gui.astral_craft.creator.error.localization_key");
        return key;
    }

    private String safeResourceName(String value) {
        String normalized = value == null ? "custom" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]+", "_")
                .replaceAll("_+", "_").replaceAll("^[._-]+|[._-]+$", "");
        return normalized.isBlank() ? "custom" : normalized;
    }

    private void validateImageSource(Path source, String extension) throws IOException, EditorException {
        if (Files.size(source) > 32L * 1024L * 1024L) throw new EditorException("gui.astral_craft.creator.error.image_file");
        byte[] signature = new byte[8];
        int read;
        try (var input = Files.newInputStream(source)) {
            read = input.read(signature);
        }
        if (read < 8) throw new EditorException("gui.astral_craft.creator.error.image_file");
        boolean png = signature[0] == (byte) 0x89 && signature[1] == 0x50 && signature[2] == 0x4E && signature[3] == 0x47
                && signature[4] == 0x0D && signature[5] == 0x0A && signature[6] == 0x1A && signature[7] == 0x0A;
        boolean jpeg = signature[0] == (byte) 0xFF && signature[1] == (byte) 0xD8 && signature[2] == (byte) 0xFF;
        if (("png".equals(extension) && !png) || (("jpg".equals(extension) || "jpeg".equals(extension)) && !jpeg)) {
            throw new EditorException("gui.astral_craft.creator.error.image_file");
        }
    }

    private boolean handleColorClick(EditorLayout layout, double mouseX, double mouseY) {
        int y = this.tabStartY(layout) + 34 + 38;
        RgbColorControl[] controls = {this.rarityBorderColor, this.rarityBadgeColor, this.rarityTextColor};
        for (RgbColorControl control : controls) {
            int sliderY = y + 36;
            int barX = layout.tabContentX() + 18;
            int barW = Math.max(40, layout.tabContentW() - 74);
            for (int channel = 0; channel < 3; channel++) {
                int barY = sliderY + channel * 16;
                if (this.isTabControlVisible(layout, barY, 14) && this.isInside(mouseX, mouseY, barX, barY, barW, 14)) {
                    control.draggingChannel = channel;
                    this.updateActiveColor(layout, mouseX);
                    return true;
                }
            }
            y += 88;
        }
        return false;
    }

    private RgbColorControl activeColorControl() {
        if (this.tab != EditorTab.SKIN_RARITY) return null;
        for (RgbColorControl control : new RgbColorControl[]{this.rarityBorderColor, this.rarityBadgeColor, this.rarityTextColor}) {
            if (control != null && control.draggingChannel >= 0) return control;
        }
        return null;
    }

    private void updateActiveColor(EditorLayout layout, double mouseX) {
        RgbColorControl control = this.activeColorControl();
        if (control == null) return;
        int barX = layout.tabContentX() + 18;
        int barW = Math.max(40, layout.tabContentW() - 74);
        int value = Math.clamp((int) Math.round((mouseX - barX) * 255.0D / Math.max(1, barW)), 0, 255);
        control.setChannel(control.draggingChannel, value);
    }

    private void clearColorDragging() {
        for (RgbColorControl control : new RgbColorControl[]{this.rarityBorderColor, this.rarityBadgeColor, this.rarityTextColor}) {
            if (control != null) control.draggingChannel = -1;
        }
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
        if (normalized.isBlank() || normalized.equals("..")) throw new EditorException("gui.astral_craft.creator.error.pack_name");
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

    private Identifier requireRegistryIdentifier(EditBox box, RegistrySuggestionKind kind, String key) throws EditorException {
        Identifier value = this.requireIdentifier(box, key);
        if (!kind.contains(this, value)) throw new EditorException("gui.astral_craft.creator.error.unknown_registry_entry", value.toString());
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

    private List<Identifier> requireRegistryCsv(EditBox box, RegistrySuggestionKind kind, String key) throws EditorException {
        List<Identifier> values = new ArrayList<>();
        for (String raw : this.requireCsv(box, key, true)) {
            Identifier value = Identifier.tryParse(raw);
            if (value == null || !kind.contains(this, value)) throw new EditorException("gui.astral_craft.creator.error.unknown_registry_entry", raw);
            values.add(value);
        }
        return List.copyOf(values);
    }

    private int[] requireIntTriple(EditBox box, String key) throws EditorException {
        String[] parts = this.requireText(box, key).trim().split("[ ,]+", -1);
        if (parts.length != 3) throw new EditorException(key);
        int[] values = new int[3];
        try {
            for (int index = 0; index < values.length; index++) values[index] = Integer.parseInt(parts[index]);
        } catch (NumberFormatException exception) {
            throw new EditorException(key);
        }
        return values;
    }

    private void updateWidgets(EditorLayout layout) {
        if (this.packNameBox == null) return;
        this.positionEventWidgets(layout);
        this.positionSkinWidgets(layout);
        this.positionRarityWidgets(layout);
        this.positionAppearanceWidgets(layout);
        this.positionExportWidgets(layout);
    }

    private void positionEventWidgets(EditorLayout layout) {
        boolean event = this.tab == EditorTab.EVENT;
        int y = this.tabStartY(layout);
        this.positionTabBox(this.eventIdBox, layout.leftX(), y + 10, layout.halfW(), event, layout);
        this.positionTabBox(this.eventNameBox, layout.rightX(), y + 10, layout.halfW(), event, layout);
        y += 34;
        this.positionTabBox(this.eventDescriptionBox, layout.leftX(), y + 10, layout.halfW(), event, layout);
        this.positionTabBox(this.eventTextureBox, layout.rightX(), y + 10, layout.halfW(), event, layout);
        y += 34;
        int readW = Math.max(54, layout.halfW() / 3);
        this.positionTabBox(this.eventNameKeyBox, layout.leftX(), y + 10, Math.max(1, layout.halfW() - readW - GAP), event, layout);
        this.positionTabBox(this.eventDescriptionKeyBox, layout.rightX(), y + 10, Math.max(1, layout.halfW() - readW - GAP), event, layout);
        y += 34;
        this.positionTabBox(this.eventCooldownBox, layout.eventMetaX(0), y + 10, layout.eventMetaW(), event, layout);
        this.positionTabBox(this.eventChanceBox, layout.eventMetaX(1), y + 10, layout.eventMetaW(), event, layout);
        this.positionTabBox(this.eventRadiusBox, layout.eventMetaX(2), y + 10, layout.eventMetaW(), event, layout);
        y += 36;

        int toggleW = Math.max(1, (layout.tabContentW() - GAP) / 2);
        this.positionTabCheckbox(this.eventTriggersCheckbox, layout.tabContentX(), y, toggleW, event, layout);
        this.positionTabCheckbox(this.eventBroadcastCheckbox, layout.tabContentX() + toggleW + GAP, y, toggleW, event, layout);
        y += this.eventToggleHeight() + EVENT_SECTION_GAP;

        y = this.positionConditionSection(layout, y, event);
        y += EVENT_SECTION_GAP;
        y = this.positionEffectSection(layout, y, false, event);
        y += EVENT_SECTION_GAP;
        this.positionEffectSection(layout, y, true, event);
    }

    private int positionConditionSection(EditorLayout layout, int y, boolean visible) {
        y += 14;
        y = this.positionConditionCheckboxFlow(layout, y, visible);
        for (ConditionDraft draft : this.eventConditions) {
            if (!draft.hasArguments()) {
                draft.hideArguments(this);
                continue;
            }
            y += 4;
            draft.positionArguments(this, layout.tabContentX(), y, layout.tabContentW(), visible, layout);
            y += draft.argumentBlockHeight();
        }
        return y;
    }

    private int positionEffectSection(EditorLayout layout, int y, boolean boardOnly, boolean visible) {
        y += 14;
        y = this.positionEffectCheckboxFlow(layout, y, boardOnly, visible);
        for (EffectDraft draft : this.eventEffects) {
            if (draft.type.boardOnly() != boardOnly) continue;
            if (!draft.hasArguments()) {
                draft.hideArguments(this);
                continue;
            }
            y += 4;
            draft.positionArguments(this, layout.tabContentX(), y, layout.tabContentW(), visible, layout);
            y += draft.argumentBlockHeight();
        }
        return y;
    }

    private void positionSkinWidgets(EditorLayout layout) {
        boolean visible = this.tab == EditorTab.CHARACTER_SKIN;
        int y = this.tabStartY(layout);
        this.positionTabBox(this.skinEntryIdBox, layout.leftX(), y + 10, layout.halfW(), visible, layout);
        this.positionTabBox(this.skinNameBox, layout.rightX(), y + 10, layout.halfW(), visible, layout);
        y += 34;
        this.positionTabBox(this.skinNameKeyBox, layout.leftX(), y + 10, layout.halfW(), visible, layout);
        y += 34;
        this.positionTabBox(this.skinCharacterBox, layout.leftX(), y + 10, layout.halfW(), visible, layout);
        this.positionTabBox(this.skinIdBox, layout.rightX(), y + 10, layout.halfW(), visible, layout);
        y += 34;
        this.positionTabBox(this.skinTextureBox, layout.leftX(), y + 10, layout.halfW(), visible, layout);
        this.positionTabBox(this.skinRarityBox, layout.rightX(), y + 10, layout.halfW(), visible, layout);
        y += 34;
        this.positionTabBox(this.skinModelBox, layout.leftX(), y + 10, layout.halfW(), visible, layout);
        this.positionTabBox(this.skinAnimationSetBox, layout.rightX(), y + 10, layout.halfW(), visible, layout);
        y += 34;
        this.positionTabCheckbox(this.skinUnlockedCheckbox, layout.tabContentX(), y, layout.halfW(), visible, layout);
    }

    private void positionRarityWidgets(EditorLayout layout) {
        boolean visible = this.tab == EditorTab.SKIN_RARITY;
        int y = this.tabStartY(layout);
        this.positionTabBox(this.rarityIdBox, layout.leftX(), y + 10, layout.halfW(), visible, layout);
        this.positionTabBox(this.rarityNameBox, layout.rightX(), y + 10, layout.halfW(), visible, layout);
        y += 34;
        this.positionTabBox(this.rarityNameKeyBox, layout.leftX(), y + 10, layout.halfW(), visible, layout);
        y += 38;
        this.positionTabBox(this.rarityBorderBox, layout.tabContentX(), y + 10, Math.max(80, layout.tabContentW() - 54), visible, layout);
        y += 88;
        this.positionTabBox(this.rarityBadgeBox, layout.tabContentX(), y + 10, Math.max(80, layout.tabContentW() - 54), visible, layout);
        y += 88;
        this.positionTabBox(this.rarityTextBox, layout.tabContentX(), y + 10, Math.max(80, layout.tabContentW() - 54), visible, layout);
    }

    private void positionAppearanceWidgets(EditorLayout layout) {
        boolean visible = this.tab == EditorTab.APPEARANCE;
        int y = this.tabStartY(layout);
        this.positionTabBox(this.appearanceNamespaceBox, layout.leftX(), y + 10, layout.halfW(), visible, layout);
        this.positionTabBox(this.appearanceNameBox, layout.rightX(), y + 10, layout.halfW(), visible, layout);
        y += 34;
        this.positionTabBox(this.appearanceSourceBox, layout.leftX(), y + 10, layout.halfW(), visible, layout);
    }

    private void positionExportWidgets(EditorLayout layout) {
        boolean visible = this.tab == EditorTab.EXPORT;
        int y = this.tabStartY(layout);
        this.positionTabBox(this.packNameBox, layout.tabContentX(), y + 10, layout.tabContentW(), visible, layout);
        y += 34 + 34 + 14;
        y = this.positionExportCheckboxFlow(layout, y, visible);
        y += 8;
        this.positionTabCheckbox(this.exportSeparateCheckbox, layout.tabContentX(), y, layout.tabContentW(), visible, layout);
    }

    private int conditionSectionEnd(EditorLayout layout, int y) {
        y += 14;
        y = this.conditionCheckboxFlowEnd(layout, y);
        for (ConditionDraft draft : this.eventConditions) if (draft.hasArguments()) y += 4 + draft.argumentBlockHeight();
        return y;
    }

    private int effectSectionEnd(EditorLayout layout, int y, boolean boardOnly) {
        y += 14;
        y = this.effectCheckboxFlowEnd(layout, y, boardOnly);
        for (EffectDraft draft : this.eventEffects) {
            if (draft.type.boardOnly() == boardOnly && draft.hasArguments()) y += 4 + draft.argumentBlockHeight();
        }
        return y;
    }

    private int conditionCheckboxFlowEnd(EditorLayout layout, int y) {
        int rowWidth = 0;
        int rowHeight = 0;
        for (ConditionDraft draft : this.eventConditions) {
            int width = this.preferredCheckboxWidth(draft.type.translationKey, layout.tabContentW());
            int height = draft.checkboxHeight();
            if (rowWidth > 0 && rowWidth + width > layout.tabContentW()) {
                y += rowHeight + CHECKBOX_GAP;
                rowWidth = 0;
                rowHeight = 0;
            }
            rowWidth += width + CHECKBOX_GAP;
            rowHeight = Math.max(rowHeight, height);
        }
        return y + rowHeight;
    }

    private int effectCheckboxFlowEnd(EditorLayout layout, int y, boolean boardOnly) {
        int rowWidth = 0;
        int rowHeight = 0;
        for (EffectDraft draft : this.eventEffects) {
            if (draft.type.boardOnly() != boardOnly) continue;
            int width = this.preferredCheckboxWidth(draft.type.translationKey, layout.tabContentW());
            int height = draft.checkboxHeight();
            if (rowWidth > 0 && rowWidth + width > layout.tabContentW()) {
                y += rowHeight + CHECKBOX_GAP;
                rowWidth = 0;
                rowHeight = 0;
            }
            rowWidth += width + CHECKBOX_GAP;
            rowHeight = Math.max(rowHeight, height);
        }
        return y + rowHeight;
    }

    private int positionConditionCheckboxFlow(EditorLayout layout, int y, boolean visible) {
        int x = layout.tabContentX();
        int rowHeight = 0;
        for (ConditionDraft draft : this.eventConditions) {
            int width = this.preferredCheckboxWidth(draft.type.translationKey, layout.tabContentW());
            int height = draft.checkboxHeight();
            if (x > layout.tabContentX() && x + width > layout.tabContentRight()) {
                y += rowHeight + CHECKBOX_GAP;
                x = layout.tabContentX();
                rowHeight = 0;
            }
            this.positionTabCheckbox(draft.checkbox, x, y, width, visible, layout);
            x += width + CHECKBOX_GAP;
            rowHeight = Math.max(rowHeight, height);
        }
        return y + rowHeight;
    }

    private int positionEffectCheckboxFlow(EditorLayout layout, int y, boolean boardOnly, boolean visible) {
        int x = layout.tabContentX();
        int rowHeight = 0;
        for (EffectDraft draft : this.eventEffects) {
            if (draft.type.boardOnly() != boardOnly) continue;
            int width = this.preferredCheckboxWidth(draft.type.translationKey, layout.tabContentW());
            int height = draft.checkboxHeight();
            if (x > layout.tabContentX() && x + width > layout.tabContentRight()) {
                y += rowHeight + CHECKBOX_GAP;
                x = layout.tabContentX();
                rowHeight = 0;
            }
            this.positionTabCheckbox(draft.checkbox, x, y, width, visible, layout);
            x += width + CHECKBOX_GAP;
            rowHeight = Math.max(rowHeight, height);
        }
        return y + rowHeight;
    }

    private int exportCheckboxFlowHeight(EditorLayout layout) {
        String[] keys = {
                "gui.astral_craft.creator.export.content.event",
                "gui.astral_craft.creator.export.content.skin",
                "gui.astral_craft.creator.export.content.rarity",
                "gui.astral_craft.creator.export.content.appearance"
        };
        Checkbox[] checkboxes = {this.exportEventCheckbox, this.exportSkinCheckbox, this.exportRarityCheckbox, this.exportAppearanceCheckbox};
        int widthUsed = 0;
        int rowHeight = 0;
        int height = 0;
        for (int index = 0; index < keys.length; index++) {
            int width = this.preferredCheckboxWidth(keys[index], layout.tabContentW());
            int checkboxHeight = checkboxes[index] == null ? FIELD_HEIGHT : checkboxes[index].getHeight();
            if (widthUsed > 0 && widthUsed + width > layout.tabContentW()) {
                height += rowHeight + CHECKBOX_GAP;
                widthUsed = 0;
                rowHeight = 0;
            }
            widthUsed += width + CHECKBOX_GAP;
            rowHeight = Math.max(rowHeight, checkboxHeight);
        }
        return height + rowHeight;
    }

    private int positionExportCheckboxFlow(EditorLayout layout, int y, boolean visible) {
        String[] keys = {
                "gui.astral_craft.creator.export.content.event",
                "gui.astral_craft.creator.export.content.skin",
                "gui.astral_craft.creator.export.content.rarity",
                "gui.astral_craft.creator.export.content.appearance"
        };
        Checkbox[] checkboxes = {this.exportEventCheckbox, this.exportSkinCheckbox, this.exportRarityCheckbox, this.exportAppearanceCheckbox};
        int x = layout.tabContentX();
        int rowHeight = 0;
        for (int index = 0; index < keys.length; index++) {
            int width = this.preferredCheckboxWidth(keys[index], layout.tabContentW());
            int height = checkboxes[index] == null ? FIELD_HEIGHT : checkboxes[index].getHeight();
            if (x > layout.tabContentX() && x + width > layout.tabContentRight()) {
                y += rowHeight + CHECKBOX_GAP;
                x = layout.tabContentX();
                rowHeight = 0;
            }
            this.positionTabCheckbox(checkboxes[index], x, y, width, visible, layout);
            x += width + CHECKBOX_GAP;
            rowHeight = Math.max(rowHeight, height);
        }
        return y + rowHeight;
    }

    private int preferredCheckboxWidth(String key, int availableWidth) {
        return Math.clamp(this.font.width(Component.translatable(key)) + 24, 64, availableWidth);
    }

    private int wrappedTextHeight(Component text, int width) {
        int lines = Math.max(1, this.font.split(text, Math.max(1, width)).size());
        return lines * (this.font.lineHeight + 2);
    }

    private int exportActionY(EditorLayout layout) {
        int y = this.tabStartY(layout) + 34 + 34 + 14 + this.exportCheckboxFlowHeight(layout);
        y += 8 + (this.exportSeparateCheckbox == null ? FIELD_HEIGHT : this.exportSeparateCheckbox.getHeight()) + 8;
        y += this.wrappedTextHeight(Component.translatable("gui.astral_craft.creator.export_packaging_note"), layout.tabContentW());
        if (this.status != null && !this.status.getString().isBlank()) y += 6 + this.wrappedTextHeight(this.status, layout.tabContentW());
        return y + 8;
    }

    private void positionTabBox(EditBox box, int x, int y, int width, boolean tabVisible, EditorLayout layout) {
        this.position(box, x, y, width, tabVisible && this.isTabControlVisible(layout, y, FIELD_HEIGHT));
    }

    private void positionTabCheckbox(Checkbox checkbox, int x, int y, int width, boolean tabVisible, EditorLayout layout) {
        int height = checkbox == null ? FIELD_HEIGHT : checkbox.getHeight();
        this.positionCheckbox(checkbox, x, y, width, tabVisible && this.isTabControlVisible(layout, y, height));
    }

    private void position(EditBox box, int x, int y, int width, boolean visible) {
        if (box == null) return;
        box.setPosition(x, y);
        box.setWidth(Math.max(1, width));
        this.updateBoxHint(box);
        box.setVisible(visible);
        box.active = visible;
    }

    private void updateBoxHint(EditBox box) {
        Component hint = this.boxHints.get(box);
        if (hint == null) return;
        int width = Math.max(1, box.getInnerWidth());
        String text = hint.getString();
        if (this.font.width(text) <= width) {
            box.setHint(hint);
            return;
        }
        String suffix = "...";
        int textWidth = Math.max(1, width - this.font.width(suffix));
        String clipped = this.font.plainSubstrByWidth(text, textWidth);
        box.setHint(Component.literal(clipped + suffix).withStyle(hint.getStyle()));
    }

    private void positionCheckbox(Checkbox checkbox, int x, int y, int width, boolean visible) {
        if (checkbox == null) return;
        checkbox.setPosition(x, y);
        checkbox.setWidth(Math.max(1, width));
        checkbox.visible = visible;
        checkbox.active = visible;
    }

    private boolean anyVisibleBoxHovered(double mouseX, double mouseY) {
        for (EditBox box : this.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).toList()) {
            if (box.visible && box.isMouseOver(mouseX, mouseY)) return true;
        }
        return false;
    }

    private boolean anyVisibleCheckboxHovered(double mouseX, double mouseY) {
        for (Checkbox checkbox : this.children().stream().filter(Checkbox.class::isInstance).map(Checkbox.class::cast).toList()) {
            if (checkbox.visible && checkbox.isMouseOver(mouseX, mouseY)) return true;
        }
        return false;
    }

    private boolean hoveredManualControl(EditorLayout layout, double mouseX, double mouseY) {
        for (int index = 0; index < EditorTab.values().length; index++) {
            if (this.isInside(mouseX, mouseY, layout.tabX(index), layout.tabY(), layout.tabW(), TAB_HEIGHT)) return true;
        }
        if (this.isInside(mouseX, mouseY, layout.topCloseX(), layout.topCloseY(), layout.topCloseW(), 16)) return true;
        if (this.maxTabScroll(layout) > 0
                && this.isInside(mouseX, mouseY, layout.tabScrollbarX(), layout.tabViewportTop(), layout.tabScrollbarW(), layout.tabViewportH())) return true;
        if (this.tab == EditorTab.EVENT) {
            for (EffectDraft draft : this.eventEffects) if (draft.specialHovered(this, mouseX, mouseY)) return true;
            int start = this.tabStartY(layout);
            int keyY = start + 78;
            int readW = Math.max(54, layout.halfW() / 3);
            int targetY = start + 112;
            return this.isTabControlVisible(layout, keyY, FIELD_HEIGHT)
                    && (this.isInside(mouseX, mouseY, layout.leftX() + layout.halfW() - readW, keyY, readW, FIELD_HEIGHT)
                    || this.isInside(mouseX, mouseY, layout.rightX() + layout.halfW() - readW, keyY, readW, FIELD_HEIGHT))
                    || this.isTabControlVisible(layout, targetY, FIELD_HEIGHT)
                    && this.isInside(mouseX, mouseY, layout.eventMetaX(3), targetY, layout.eventMetaW(), FIELD_HEIGHT);
        }

        if (this.tab == EditorTab.CHARACTER_SKIN || this.tab == EditorTab.SKIN_RARITY) {
            int readY = this.tabStartY(layout) + 44;
            if (this.isTabControlVisible(layout, readY, FIELD_HEIGHT)
                    && this.isInside(mouseX, mouseY, layout.rightX(), readY, layout.halfW(), FIELD_HEIGHT)) return true;
            if (this.tab == EditorTab.SKIN_RARITY) {
                int y = this.tabStartY(layout) + 34 + 38;
                int barX = layout.tabContentX() + 18;
                int barW = Math.max(40, layout.tabContentW() - 74);
                for (int control = 0; control < 3; control++) {
                    int sliderY = y + control * 88 + 36;
                    for (int channel = 0; channel < 3; channel++) {
                        if (this.isInside(mouseX, mouseY, barX, sliderY + channel * 16, barW, 14)) return true;
                    }
                }
            }
        }

        if (this.tab == EditorTab.APPEARANCE) {
            int start = this.tabStartY(layout);
            return this.isTabControlVisible(layout, start + 44, FIELD_HEIGHT)
                    && this.isInside(mouseX, mouseY, layout.rightX(), start + 44, layout.halfW(), FIELD_HEIGHT)
                    || this.isTabControlVisible(layout, start + 68, FIELD_HEIGHT)
                    && this.isInside(mouseX, mouseY, layout.leftX(), start + 68, layout.halfW(), FIELD_HEIGHT);
        }

        if (this.tab == EditorTab.EXPORT) {
            int start = this.tabStartY(layout);
            int chooseY = start + 44;
            int exportY = this.exportActionY(layout);
            return this.isTabControlVisible(layout, chooseY, FIELD_HEIGHT)
                    && this.isInside(mouseX, mouseY, layout.tabContentRight() - layout.choosePathW(), chooseY, layout.choosePathW(), FIELD_HEIGHT)
                    || this.isTabControlVisible(layout, exportY, 22)
                    && this.isInside(mouseX, mouseY, layout.tabContentX(), exportY, layout.tabContentW(), 22);
        }
        return false;
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private EditorLayout layout() {
        int panelW = Math.clamp(this.width - 12, 1, 770);
        int panelH = Math.clamp(this.height - 12, 1, 560);
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;
        return new EditorLayout(panelX, panelY, panelW, panelH);
    }

    private enum EditorTab {
        EVENT("gui.astral_craft.creator.tab.event", 0xFFB05282),
        CHARACTER_SKIN("gui.astral_craft.creator.tab.skin", 0xFF5664B7),
        SKIN_RARITY("gui.astral_craft.creator.tab.rarity", 0xFF8B63B7),
        APPEARANCE("gui.astral_craft.creator.tab.appearance", 0xFF4F9D69),
        EXPORT("gui.astral_craft.creator.tab.export", 0xFF4F9D69);

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
            for (Identifier value : screen.requireRegistryCsv(a, RegistrySuggestionKind.DIMENSION, "gui.astral_craft.creator.error.identifier_list")) values.add(value.toString());
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
            result.addProperty("id", screen.requireRegistryIdentifier(a, RegistrySuggestionKind.ITEM, "gui.astral_craft.creator.error.identifier").toString());
            result.addProperty("count", screen.requireInt(b, 1, Integer.MAX_VALUE, "gui.astral_craft.creator.error.number"));
            result.addProperty("inverted", screen.requireBoolean(c, "gui.astral_craft.creator.error.boolean"));
            return result;
        }),
        HAS_EFFECT("gui.astral_craft.creator.condition.has_effect", "gui.astral_craft.creator.arg.effect", "gui.astral_craft.creator.arg.inverted", "",
                new DraftDefaults("minecraft:speed", "false", ""), (screen, a, b, c) -> {
            JsonObject result = typed(AstralCraft.prefix("has_effect"));
            result.addProperty("effect", screen.requireRegistryIdentifier(a, RegistrySuggestionKind.MOB_EFFECT, "gui.astral_craft.creator.error.identifier").toString());
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
            for (Identifier value : screen.requireRegistryCsv(a, RegistrySuggestionKind.ENTITY_TYPE, "gui.astral_craft.creator.error.identifier_list")) values.add(value.toString());
            result.add("entity_types", values);
            result.addProperty("inverted", screen.requireBoolean(b, "gui.astral_craft.creator.error.boolean"));
            return result;
        }),
        BLOCK_AT("gui.astral_craft.creator.condition.block_at", "gui.astral_craft.creator.arg.blocks", "gui.astral_craft.creator.arg.offset_xyz", "gui.astral_craft.creator.arg.inverted",
                new DraftDefaults("minecraft:stone", "0 0 0", "false"), (screen, a, b, c) -> {
            JsonObject result = typed(AstralCraft.prefix("block_at"));
            JsonArray blocks = new JsonArray();
            for (Identifier value : screen.requireRegistryCsv(a, RegistrySuggestionKind.BLOCK, "gui.astral_craft.creator.error.identifier_list")) blocks.add(value.toString());
            int[] offset = screen.requireIntTriple(b, "gui.astral_craft.creator.error.offset_xyz");
            result.add("blocks", blocks);
            result.addProperty("offset_x", offset[0]);
            result.addProperty("offset_y", offset[1]);
            result.addProperty("offset_z", offset[2]);
            result.addProperty("inverted", screen.requireBoolean(c, "gui.astral_craft.creator.error.boolean"));
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

    }

    private enum EffectType {
        NONE("gui.astral_craft.creator.effect.none", "", "", "", new DraftDefaults("", "", ""), target -> true, (screen, a, b, c) -> null),
        GIVE_ITEM("gui.astral_craft.creator.effect.give_item", "gui.astral_craft.creator.arg.item", "gui.astral_craft.creator.arg.count", "",
                new DraftDefaults("", "1", ""), TargetScope::playersOnly, (screen, a, b, c) -> {
            JsonObject result = typed(AstralCraft.prefix("give_item"));
            result.addProperty("id", screen.requireRegistryIdentifier(a, RegistrySuggestionKind.ITEM, "gui.astral_craft.creator.error.identifier").toString());
            result.addProperty("count", screen.requireInt(b, 1, Integer.MAX_VALUE, "gui.astral_craft.creator.error.number"));
            return result;
        }),
        DROP_ITEM("gui.astral_craft.creator.effect.drop_item", "gui.astral_craft.creator.arg.item", "gui.astral_craft.creator.arg.count", "",
                new DraftDefaults("minecraft:apple", "1", ""), target -> true, (screen, a, b, c) -> {
            JsonObject result = typed(AstralCraft.prefix("drop_item"));
            result.addProperty("id", screen.requireRegistryIdentifier(a, RegistrySuggestionKind.ITEM, "gui.astral_craft.creator.error.identifier").toString());
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
            result.addProperty("effect", screen.requireRegistryIdentifier(a, RegistrySuggestionKind.MOB_EFFECT, "gui.astral_craft.creator.error.identifier").toString());
            result.addProperty("duration_ticks", screen.requireInt(b, 1, Integer.MAX_VALUE, "gui.astral_craft.creator.error.number"));
            result.addProperty("amplifier", screen.requireInt(c, 0, 255, "gui.astral_craft.creator.error.number"));
            return result;
        }),
        CLEAR_EFFECT("gui.astral_craft.creator.effect.clear_effect", "gui.astral_craft.creator.arg.effect", "gui.astral_craft.creator.arg.all", "",
                new DraftDefaults("minecraft:speed", "false", ""), target -> true, (screen, a, b, c) -> {
            JsonObject result = typed(AstralCraft.prefix("clear_effect"));
            result.addProperty("effect", screen.requireRegistryIdentifier(a, RegistrySuggestionKind.MOB_EFFECT, "gui.astral_craft.creator.error.identifier").toString());
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

        private boolean boardOnly() {
            return this.name().startsWith("BOARD_");
        }

        private boolean compatibleWith(TargetScope target) {
            return this.targetPredicate.test(target);
        }
    }

    private enum RegistrySuggestionKind {
        ITEM, BLOCK, ENTITY_TYPE, MOB_EFFECT, DIMENSION;

        private List<Identifier> entries(AstralDataEditorScreen screen) {
            return switch (this) {
                case ITEM -> List.copyOf(BuiltInRegistries.ITEM.keySet());
                case BLOCK -> List.copyOf(BuiltInRegistries.BLOCK.keySet());
                case ENTITY_TYPE -> List.copyOf(BuiltInRegistries.ENTITY_TYPE.keySet());
                case MOB_EFFECT -> List.copyOf(BuiltInRegistries.MOB_EFFECT.keySet());
                case DIMENSION -> screen.minecraft == null || screen.minecraft.getConnection() == null
                        ? List.of(Identifier.parse("minecraft:overworld"), Identifier.parse("minecraft:the_nether"), Identifier.parse("minecraft:the_end"))
                        : screen.minecraft.getConnection().levels().stream().map(key -> key.identifier()).toList();
            };
        }

        private boolean contains(AstralDataEditorScreen screen, Identifier id) {
            return switch (this) {
                case ITEM -> BuiltInRegistries.ITEM.containsKey(id);
                case BLOCK -> BuiltInRegistries.BLOCK.containsKey(id);
                case ENTITY_TYPE -> BuiltInRegistries.ENTITY_TYPE.containsKey(id);
                case MOB_EFFECT -> BuiltInRegistries.MOB_EFFECT.containsKey(id);
                case DIMENSION -> screen.minecraft == null || screen.minecraft.getConnection() == null
                        || screen.minecraft.getConnection().levels().stream().anyMatch(key -> key.identifier().equals(id));
            };
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

    private static class RgbColorControl {
        private final EditBox box;
        private int red;
        private int green;
        private int blue;
        private int draggingChannel = -1;
        private boolean updatingBox;

        private RgbColorControl(EditBox box) {
            this.box = box;
            this.readBox(box.getValue());
            box.setResponder(this::readBox);
        }

        private void readBox(String value) {
            if (this.updatingBox || value == null || !value.matches("#[0-9A-Fa-f]{6}")) return;
            int rgb = Integer.parseInt(value.substring(1), 16);
            this.red = rgb >> 16 & 255;
            this.green = rgb >> 8 & 255;
            this.blue = rgb & 255;
        }

        private int channel(int channel) {
            return switch (channel) { case 0 -> this.red; case 1 -> this.green; default -> this.blue; };
        }

        private void setChannel(int channel, int value) {
            value = Math.clamp(value, 0, 255);
            if (channel == 0) this.red = value;
            else if (channel == 1) this.green = value;
            else this.blue = value;
            this.updatingBox = true;
            this.box.setValue(String.format(Locale.ROOT, "#%02X%02X%02X", this.red, this.green, this.blue));
            this.updatingBox = false;
        }

        private int rgb() {
            return this.red << 16 | this.green << 8 | this.blue;
        }
    }

    private static class ConditionDraft {
        private final ConditionType type;
        private boolean selected;
        private Checkbox checkbox;
        private EditBox arg1;
        private EditBox arg2;
        private EditBox arg3;

        private ConditionDraft(ConditionType type) {
            this.type = type;
        }

        private void create(AstralDataEditorScreen screen, int width) {
            boolean firstCreate = this.arg1 == null;
            String first = firstCreate ? this.type.defaults.first() : this.arg1.getValue();
            String second = firstCreate ? this.type.defaults.second() : this.arg2.getValue();
            String third = firstCreate ? this.type.defaults.third() : this.arg3.getValue();
            this.checkbox = screen.createCheckbox(this.type.translationKey, this.selected, value -> this.selected = value, width);
            this.arg1 = screen.createBox("gui.astral_craft.creator.arg.value", 160);
            this.arg2 = screen.createBox("gui.astral_craft.creator.arg.value", 160);
            this.arg3 = screen.createBox("gui.astral_craft.creator.arg.value", 160);
            this.set(first, second, third);
        }

        private void set(String first, String second, String third) {
            this.arg1.setValue(first);
            this.arg2.setValue(second);
            this.arg3.setValue(third);
        }

        private int checkboxHeight() {
            return this.checkbox == null ? FIELD_HEIGHT : this.checkbox.getHeight();
        }

        private int argumentCount() {
            int count = 0;
            if (!this.type.arg1.isEmpty()) count++;
            if (!this.type.arg2.isEmpty()) count++;
            if (!this.type.arg3.isEmpty()) count++;
            return count;
        }

        private boolean hasArguments() {
            return this.selected && this.argumentCount() > 0;
        }

        private int argumentBlockHeight() {
            return this.hasArguments() ? 14 + this.argumentCount() * ARGUMENT_ROW_HEIGHT : 0;
        }

        private void hideArguments(AstralDataEditorScreen screen) {
            screen.position(this.arg1, 0, 0, 1, false);
            screen.position(this.arg2, 0, 0, 1, false);
            screen.position(this.arg3, 0, 0, 1, false);
        }

        private void positionArguments(AstralDataEditorScreen screen, int x, int y, int width, boolean tabVisible, EditorLayout layout) {
            if (!this.hasArguments()) {
                this.hideArguments(screen);
                return;
            }
            int argX = x + 18;
            int argW = Math.max(1, width - 18);
            int argY = y + 14;
            argY = this.positionArgument(screen, this.arg1, this.type.arg1, argX, argY, argW, tabVisible, layout);
            argY = this.positionArgument(screen, this.arg2, this.type.arg2, argX, argY, argW, tabVisible, layout);
            this.positionArgument(screen, this.arg3, this.type.arg3, argX, argY, argW, tabVisible, layout);
        }

        private int positionArgument(AstralDataEditorScreen screen, EditBox box, String key, int x, int y, int width,
                                     boolean tabVisible, EditorLayout layout) {
            if (key.isEmpty()) {
                screen.position(box, x, y, width, false);
                return y;
            }
            screen.positionTabBox(box, x, y + 10, width, tabVisible, layout);
            return y + ARGUMENT_ROW_HEIGHT;
        }

        private void renderArgumentLabels(GuiGraphicsExtractor graphics, AstralDataEditorScreen screen, int x, int y, int width) {
            if (!this.hasArguments()) return;
            graphics.text(screen.font, Component.translatable(this.type.translationKey), x + 4, y, 0xFFB9C6D8);
            int argX = x + 18;
            int argY = y + 14;
            if (!this.type.arg1.isEmpty()) {
                screen.label(graphics, this.type.arg1, argX, argY);
                argY += ARGUMENT_ROW_HEIGHT;
            }
            if (!this.type.arg2.isEmpty()) {
                screen.label(graphics, this.type.arg2, argX, argY);
                argY += ARGUMENT_ROW_HEIGHT;
            }
            if (!this.type.arg3.isEmpty()) screen.label(graphics, this.type.arg3, argX, argY);
        }

        private JsonObject toJson(AstralDataEditorScreen screen) throws EditorException {
            if (!this.selected) return null;
            try {
                return this.type.encoder.encode(screen, this.arg1, this.arg2, this.arg3);
            } catch (EditorException exception) {
                throw exception.withContext(screen.eventContext(this.type.translationKey, "gui.astral_craft.creator.event.conditions"));
            }
        }
    }

    private static class EffectDraft {
        private final EffectType type;
        private final List<ItemStack> itemStacks = new ArrayList<>();
        private boolean selected;
        private Checkbox checkbox;
        private EditBox arg1;
        private EditBox arg2;
        private EditBox arg3;

        private EffectDraft(EffectType type) {
            this.type = type;
        }

        private void create(AstralDataEditorScreen screen, int width) {
            boolean firstCreate = this.arg1 == null;
            String first = firstCreate ? this.type.defaults.first() : this.arg1.getValue();
            String second = firstCreate ? this.type.defaults.second() : this.arg2.getValue();
            String third = firstCreate ? this.type.defaults.third() : this.arg3.getValue();
            this.checkbox = screen.createCheckbox(this.type.translationKey, this.selected, value -> this.selected = value, width);
            this.arg1 = screen.createBox("gui.astral_craft.creator.arg.value", 160);
            this.arg2 = screen.createBox("gui.astral_craft.creator.arg.value", 160);
            this.arg3 = screen.createBox("gui.astral_craft.creator.arg.value", 160);
            this.set(first, second, third);
        }

        private void set(String first, String second, String third) {
            this.arg1.setValue(first);
            this.arg2.setValue(second);
            this.arg3.setValue(third);
        }

        private int checkboxHeight() {
            return this.checkbox == null ? FIELD_HEIGHT : this.checkbox.getHeight();
        }

        private int argumentCount() {
            int count = 0;
            if (!this.type.arg1.isEmpty()) count++;
            if (!this.type.arg2.isEmpty()) count++;
            if (!this.type.arg3.isEmpty()) count++;
            return count;
        }

        private boolean hasArguments() {
            return this.selected && this.argumentCount() > 0;
        }

        private int argumentBlockHeight() {
            if (!this.hasArguments()) return 0;
            int height = 14 + this.argumentCount() * ARGUMENT_ROW_HEIGHT;
            if (this.type == EffectType.GIVE_ITEM) height += 26 + this.itemStacks.size() * 24;
            return height;
        }

        private void hideArguments(AstralDataEditorScreen screen) {
            screen.position(this.arg1, 0, 0, 1, false);
            screen.position(this.arg2, 0, 0, 1, false);
            screen.position(this.arg3, 0, 0, 1, false);
        }

        private void positionArguments(AstralDataEditorScreen screen, int x, int y, int width, boolean tabVisible, EditorLayout layout) {
            if (!this.hasArguments()) {
                this.hideArguments(screen);
                return;
            }
            int argX = x + 18;
            int argW = Math.max(1, width - 18);
            int argY = y + 14;
            argY = this.positionArgument(screen, this.arg1, this.type.arg1, argX, argY, argW, tabVisible, layout);
            argY = this.positionArgument(screen, this.arg2, this.type.arg2, argX, argY, argW, tabVisible, layout);
            this.positionArgument(screen, this.arg3, this.type.arg3, argX, argY, argW, tabVisible, layout);
        }

        private int positionArgument(AstralDataEditorScreen screen, EditBox box, String key, int x, int y, int width,
                                     boolean tabVisible, EditorLayout layout) {
            if (key.isEmpty()) {
                screen.position(box, x, y, width, false);
                return y;
            }
            screen.positionTabBox(box, x, y + 10, width, tabVisible, layout);
            return y + ARGUMENT_ROW_HEIGHT;
        }

        private void renderArgumentLabels(GuiGraphicsExtractor graphics, AstralDataEditorScreen screen, int x, int y, int width, int mouseX, int mouseY) {
            if (!this.hasArguments()) return;
            graphics.text(screen.font, Component.translatable(this.type.translationKey), x + 4, y, 0xFFB9C6D8);
            int argX = x + 18;
            int argY = y + 14;
            if (!this.type.arg1.isEmpty()) {
                screen.label(graphics, this.type.arg1, argX, argY);
                argY += ARGUMENT_ROW_HEIGHT;
            }
            if (!this.type.arg2.isEmpty()) {
                screen.label(graphics, this.type.arg2, argX, argY);
                argY += ARGUMENT_ROW_HEIGHT;
            }
            if (!this.type.arg3.isEmpty()) {
                screen.label(graphics, this.type.arg3, argX, argY);
                argY += ARGUMENT_ROW_HEIGHT;
            }
            if (this.type == EffectType.GIVE_ITEM) this.renderItemControls(graphics, screen, argX, argY, Math.max(1, width - 18), mouseX, mouseY);
        }

        private void renderItemControls(GuiGraphicsExtractor graphics, AstralDataEditorScreen screen, int x, int y, int width, int mouseX, int mouseY) {
            int leftW = Math.max(1, (width - GAP) / 2);
            int rightW = Math.max(1, width - leftW - GAP);
            screen.renderButton(graphics, x, y, leftW, 22, "gui.astral_craft.creator.item.add_typed", mouseX, mouseY, 0xFF5664B7, false);
            screen.renderButton(graphics, x + leftW + GAP, y, rightW, 22, "gui.astral_craft.creator.item.add_inventory", mouseX, mouseY, 0xFF5664B7, false);
            int rowY = y + 26;
            for (int index = 0; index < this.itemStacks.size(); index++) {
                ItemStack stack = this.itemStacks.get(index);
                graphics.fill(x, rowY, x + width, rowY + 21, 0x443B4250);
                graphics.item(stack, x + 2, rowY + 2);
                Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                String label = stack.getHoverName().getString() + "  " + id + " x" + stack.getCount();
                String shown = screen.font.plainSubstrByWidth(label, Math.max(1, width - 48));
                graphics.text(screen.font, shown, x + 22, rowY + 7, 0xFFE6EDF7);
                screen.renderButton(graphics, x + width - 22, rowY + 1, 21, 19, "gui.astral_craft.creator.item.remove_short", mouseX, mouseY, 0xFF8A4F5D, false);
                if (screen.isInside(mouseX, mouseY, x, rowY, width - 24, 21)) {
                    Component tooltip = Component.literal(stack.getHoverName().getString() + "\n" + id)
                            .append("\n").append(Component.translatable("gui.astral_craft.creator.inventory_picker.data_kept"));
                    graphics.setTooltipForNextFrame(screen.font, screen.font.split(tooltip, Math.min(260, Math.max(120, screen.width - 32))), mouseX, mouseY);
                }
                rowY += 24;
            }
        }

        private boolean handleSpecialClick(AstralDataEditorScreen screen, double mouseX, double mouseY) {
            if (!this.selected || this.type != EffectType.GIVE_ITEM || !this.arg2.visible) return false;
            int x = this.arg2.getX();
            int width = this.arg2.getWidth();
            int y = this.arg2.getY() + this.arg2.getHeight() + 4;
            int leftW = Math.max(1, (width - GAP) / 2);
            int rightW = Math.max(1, width - leftW - GAP);
            if (screen.isInside(mouseX, mouseY, x, y, leftW, 22)) {
                try {
                    this.addTypedStack(screen);
                    screen.status = Component.translatable("gui.astral_craft.creator.item.added");
                    screen.statusError = false;
                    screen.onDynamicLayoutChanged();
                } catch (EditorException exception) {
                    screen.setEditorError(exception.withContext(screen.eventContext(this.type.translationKey, "gui.astral_craft.creator.event.effects")));
                }
                return true;
            }
            if (screen.isInside(mouseX, mouseY, x + leftW + GAP, y, rightW, 22)) {
                screen.openInventoryItemPicker(this);
                return true;
            }
            int rowY = y + 26;
            for (int index = 0; index < this.itemStacks.size(); index++) {
                if (!screen.isInside(mouseX, mouseY, x + width - 22, rowY + 1, 21, 19)) {
                    rowY += 24;
                    continue;
                }
                this.itemStacks.remove(index);
                screen.onDynamicLayoutChanged();
                return true;
            }
            return false;
        }

        private boolean specialHovered(AstralDataEditorScreen screen, double mouseX, double mouseY) {
            if (!this.selected || this.type != EffectType.GIVE_ITEM || !this.arg2.visible) return false;
            int x = this.arg2.getX();
            int width = this.arg2.getWidth();
            int y = this.arg2.getY() + this.arg2.getHeight() + 4;
            if (screen.isInside(mouseX, mouseY, x, y, width, 22)) return true;
            int rowY = y + 26;
            for (int index = 0; index < this.itemStacks.size(); index++) {
                if (screen.isInside(mouseX, mouseY, x + width - 22, rowY + 1, 21, 19)) return true;
                rowY += 24;
            }
            return false;
        }

        private void addTypedStack(AstralDataEditorScreen screen) throws EditorException {
            Identifier id = screen.requireRegistryIdentifier(this.arg1, RegistrySuggestionKind.ITEM, "gui.astral_craft.creator.error.identifier");
            int count = screen.requireInt(this.arg2, 1, 99, "gui.astral_craft.creator.error.number");
            this.itemStacks.add(new ItemStack(BuiltInRegistries.ITEM.getValue(id), count));
            this.arg1.setValue("");
            this.arg2.setValue("1");
        }

        private void addInventoryStack(ItemStack stack) {
            if (!stack.isEmpty()) this.itemStacks.add(stack.copy());
        }

        private List<ItemStack> exportStacks(AstralDataEditorScreen screen) throws EditorException {
            List<ItemStack> values = new ArrayList<>();
            for (ItemStack stack : this.itemStacks) if (!stack.isEmpty()) values.add(stack.copy());
            if (!this.arg1.getValue().isBlank()) {
                Identifier id = screen.requireRegistryIdentifier(this.arg1, RegistrySuggestionKind.ITEM, "gui.astral_craft.creator.error.identifier");
                int count = screen.requireInt(this.arg2, 1, 99, "gui.astral_craft.creator.error.number");
                values.add(new ItemStack(BuiltInRegistries.ITEM.getValue(id), count));
            }
            if (values.isEmpty()) throw new EditorException("gui.astral_craft.creator.error.item_stack_required");
            return values;
        }

        private JsonObject toJson(AstralDataEditorScreen screen) throws EditorException {
            if (!this.selected) return null;
            try {
                if (this.type == EffectType.GIVE_ITEM) return screen.encodeGiveItemEffect(this);
                return this.type.encoder.encode(screen, this.arg1, this.arg2, this.arg3);
            } catch (EditorException exception) {
                throw exception.withContext(screen.eventContext(this.type.translationKey,
                        this.type.boardOnly() ? "gui.astral_craft.creator.event.board_effects" : "gui.astral_craft.creator.event.effects"));
            }
        }

        private boolean compatibleWith(TargetScope target) {
            return !this.selected || this.type.compatibleWith(target);
        }
    }

    private static class EditorException extends Exception {
        private final String translationKey;
        private final Object[] args;
        private final Component context;

        private EditorException(String translationKey, Object... args) {
            this(translationKey, null, args);
        }

        private EditorException(String translationKey, Component context, Object[] args) {
            this.translationKey = translationKey;
            this.args = args;
            this.context = context;
        }

        private EditorException withContext(Component context) {
            return this.context == null ? new EditorException(this.translationKey, context, this.args) : this;
        }
    }

    @FunctionalInterface
    private interface EditorSupplier<T> {
        T get() throws EditorException;
    }

    private record SuggestionBounds(int x, int y, int width, int height) {}

    private record EditorLayout(int panelX, int panelY, int panelW, int panelH) {
        private int panelRight() { return this.panelX + this.panelW; }
        private int contentX() { return this.panelX + 10; }
        private int contentW() { return Math.max(1, this.panelW - 20); }
        private int contentRight() { return this.contentX() + this.contentW(); }
        private int topCloseW() { return 20; }
        private int topCloseX() { return this.panelRight() - this.topCloseW() - 5; }
        private int topCloseY() { return this.panelY + 5; }
        private int tabY() { return this.panelY + 27; }
        private int tabW() { return Math.max(1, (this.contentW() - GAP * (EditorTab.values().length - 1)) / EditorTab.values().length); }
        private int tabX(int index) { return this.contentX() + index * (this.tabW() + GAP); }
        private int tabViewportTop() { return this.tabY() + TAB_HEIGHT + 8; }
        private int tabViewportBottom() { return this.panelY + this.panelH - 10; }
        private int tabViewportH() { return Math.max(24, this.tabViewportBottom() - this.tabViewportTop()); }
        private int tabScrollbarW() { return 5; }
        private int tabScrollbarX() { return this.contentRight() - this.tabScrollbarW(); }
        private int tabContentX() { return this.contentX() + 2; }
        private int tabContentRight() { return this.tabScrollbarX() - GAP; }
        private int tabContentW() { return Math.max(1, this.tabContentRight() - this.tabContentX()); }
        private int halfW() { return Math.max(1, (this.tabContentW() - GAP) / 2); }
        private int leftX() { return this.tabContentX(); }
        private int rightX() { return this.tabContentX() + this.halfW() + GAP; }
        private int eventMetaW() { return Math.max(1, (this.tabContentW() - GAP * 3) / 4); }
        private int eventMetaX(int index) { return this.tabContentX() + index * (this.eventMetaW() + GAP); }
        private int rarityColorW() { return Math.max(1, (this.tabContentW() - GAP * 2) / 3); }
        private int rarityColorX(int index) { return this.tabContentX() + index * (this.rarityColorW() + GAP); }
        private int choosePathW() { return Math.clamp(this.tabContentW() / 5, 70, 120); }
    }

}