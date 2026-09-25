package com.astral_craft.client.gui.editor;

import com.astral_craft.client.gui.components.AstralFancyButton;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AstralFileBrowserScreen extends Screen {

    private static final int ROW_HEIGHT = 19;
    private static final int GAP = 4;
    private final Screen parent;
    private final BrowserMode mode;
    private final Set<String> extensions;
    private final Consumer<Path> selected;
    private Path directory;
    private Path selectedFile;
    private List<Path> entries = List.of();
    private int scrollRows;
    private Component status = Component.empty();
    private EditBox pathBox;
    private EditBox folderNameBox;
    private boolean pathMenuOpen;

    public AstralFileBrowserScreen(Screen parent, Path start, BrowserMode mode, Set<String> extensions, Consumer<Path> selected) {
        super(Component.translatable(mode == BrowserMode.FOLDER
                ? "gui.astral_craft.creator.file_browser.title_folder" : "gui.astral_craft.creator.file_browser.title_file"));
        this.parent = parent;
        this.mode = mode;
        this.extensions = extensions == null ? Set.of() : extensions.stream().map(value -> value.toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
        this.selected = selected;
        this.directory = this.initialDirectory(start);
    }

    public static AstralFileBrowserScreen folder(Screen parent, Path start, Consumer<Path> selected) {
        return new AstralFileBrowserScreen(parent, start, BrowserMode.FOLDER, Set.of(), selected);
    }

    public static AstralFileBrowserScreen file(Screen parent, Path start, Set<String> extensions, Consumer<Path> selected) {
        return new AstralFileBrowserScreen(parent, start, BrowserMode.FILE, extensions, selected);
    }

    @Override
    protected void init() {
        BrowserLayout layout = this.layout();
        this.pathBox = this.addRenderableWidget(new EditBox(this.font, layout.pathX(), layout.pathY(), layout.pathW(), 20,
                Component.translatable("gui.astral_craft.creator.file_browser.current_path")));
        this.pathBox.setMaxLength(1024);
        this.pathBox.setValue(this.directory.toString());
        if (this.mode == BrowserMode.FOLDER) {
            this.folderNameBox = this.addRenderableWidget(new EditBox(this.font, layout.listX(), layout.newFolderY(), layout.newFolderBoxW(), 20,
                    Component.translatable("gui.astral_craft.creator.file_browser.new_folder")));
            this.folderNameBox.setMaxLength(96);
            this.folderNameBox.setHint(Component.translatable("gui.astral_craft.creator.file_browser.new_folder_hint"));
        }
        this.refreshEntries();
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        BrowserLayout layout = this.layout();
        AstralFancyButton.renderOutlinedBox(graphics, layout.panelX(), layout.panelY(), layout.panelW(), layout.panelH(),
                0xE9151723, 0xE8545B70, 0xD0101018, 1, 2);
        graphics.fill(layout.panelX(), layout.panelY(), layout.panelRight(), layout.panelY() + 3, 0xFFE83CA8);
        graphics.centeredText(this.font, this.title, this.width / 2, layout.panelY() + 9, 0xFFFFFFFF);
        graphics.text(this.font, Component.translatable("gui.astral_craft.creator.file_browser.current_path"), layout.pathX(), layout.pathY() - 11, 0xFFD7E4F2);
        this.renderButton(graphics, layout.upX(), layout.pathY(), layout.smallButtonW(), 20, "gui.astral_craft.creator.file_browser.up", mouseX, mouseY, 0xFF5664B7);
        this.renderButton(graphics, layout.goX(), layout.pathY(), layout.smallButtonW(), 20, "gui.astral_craft.creator.file_browser.go", mouseX, mouseY, 0xFF5664B7);
        this.renderButton(graphics, layout.refreshX(), layout.pathY(), layout.smallButtonW(), 20, "gui.astral_craft.creator.file_browser.refresh", mouseX, mouseY, 0xFF5664B7);
        this.renderPathMenu(graphics, layout, mouseX, mouseY);
        AstralFancyButton.renderOutlinedBox(graphics, layout.listX(), layout.listY(), layout.listW(), layout.listH(),
                0x9E0F111A, 0xA6545B70, 0x66101018, 1, 1);
        this.renderEntries(graphics, layout, mouseX, mouseY);
        if (this.mode == BrowserMode.FOLDER) {
            this.renderButton(graphics, layout.createFolderX(), layout.newFolderY(), layout.createFolderW(), 20,
                    "gui.astral_craft.creator.file_browser.create_folder", mouseX, mouseY, 0xFF5664B7);
        }
        if (this.status != null && !this.status.getString().isBlank()) {
            graphics.text(this.font, this.font.plainSubstrByWidth(this.status.getString(), layout.panelW() - 18),
                    layout.listX(), layout.statusY(), 0xFFFFA5B5);
        }
        String selectKey = this.mode == BrowserMode.FOLDER ? "gui.astral_craft.creator.file_browser.select_folder" : "gui.astral_craft.creator.file_browser.select_file";
        boolean canSelect = this.mode == BrowserMode.FOLDER || this.selectedFile != null;
        this.renderButton(graphics, layout.selectX(), layout.actionY(), layout.actionButtonW(), 22, selectKey, mouseX, mouseY,
                canSelect ? 0xFF4F9D69 : 0xFF5B5B66);
        this.renderButton(graphics, layout.cancelX(), layout.actionY(), layout.actionButtonW(), 22,
                "gui.astral_craft.cancel", mouseX, mouseY, 0xFF646477);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if ((this.pathBox != null && this.pathBox.isMouseOver(mouseX, mouseY))
                || (this.folderNameBox != null && this.folderNameBox.isMouseOver(mouseX, mouseY))) graphics.requestCursor(CursorTypes.IBEAM);
        else if (this.hoveredControl(layout, mouseX, mouseY)) graphics.requestCursor(CursorTypes.POINTING_HAND);
        else graphics.requestCursor(CursorTypes.ARROW);
    }

    private List<Path> pathAncestors() {
        List<Path> values = new ArrayList<>();
        Path current = this.directory;
        while (current != null && values.size() < 8) {
            values.add(current);
            current = current.getParent();
        }
        return values;
    }

    private void renderPathMenu(GuiGraphicsExtractor graphics, BrowserLayout layout, int mouseX, int mouseY) {
        if (!this.pathMenuOpen) return;
        List<Path> ancestors = this.pathAncestors();
        int x = layout.pathX();
        int y = layout.pathY() + 21;
        int width = Math.min(layout.innerW(), Math.max(layout.pathW(), 180));
        int height = ancestors.size() * ROW_HEIGHT + 4;
        AstralFancyButton.renderOutlinedBox(graphics, x, y, width, height, 0xF2151723, 0xE8545B70, 0xD0101018, 1, 1);
        for (int index = 0; index < ancestors.size(); index++) {
            Path path = ancestors.get(index);
            int rowY = y + 2 + index * ROW_HEIGHT;
            boolean hovered = this.isInside(mouseX, mouseY, x + 2, rowY, width - 4, ROW_HEIGHT - 1);
            if (hovered) graphics.fill(x + 2, rowY, x + width - 2, rowY + ROW_HEIGHT - 1, 0x553B4052);
            graphics.text(this.font, this.font.plainSubstrByWidth(path.toString(), width - 10), x + 5, rowY + 5, 0xFFD7E4F2);
        }
    }

    private boolean handlePathMenuClick(BrowserLayout layout, double mouseX, double mouseY) {
        List<Path> ancestors = this.pathAncestors();
        int x = layout.pathX();
        int y = layout.pathY() + 21;
        int width = Math.min(layout.innerW(), Math.max(layout.pathW(), 180));
        if (!this.isInside(mouseX, mouseY, x, y, width, ancestors.size() * ROW_HEIGHT + 4)) {
            this.pathMenuOpen = false;
            return false;
        }
        int index = (int) ((mouseY - y - 2) / ROW_HEIGHT);
        if (index >= 0 && index < ancestors.size()) this.navigate(ancestors.get(index));
        return true;
    }

    private void renderEntries(GuiGraphicsExtractor graphics, BrowserLayout layout, int mouseX, int mouseY) {
        int visibleRows = Math.max(1, layout.listH() / ROW_HEIGHT);
        int start = Math.clamp(this.entries.size() - visibleRows, 0, this.scrollRows);
        int end = Math.min(this.entries.size(), start + visibleRows);
        if (this.entries.isEmpty()) {
            graphics.centeredText(this.font, Component.translatable("gui.astral_craft.creator.file_browser.empty"),
                    layout.listX() + layout.listW() / 2, layout.listY() + 8, 0xFF8F9BAD);
            return;
        }
        for (int index = start; index < end; index++) {
            Path entry = this.entries.get(index);
            int rowY = layout.listY() + 2 + (index - start) * ROW_HEIGHT;
            boolean directory = Files.isDirectory(entry);
            boolean selected = entry.equals(this.selectedFile);
            boolean hovered = this.isInside(mouseX, mouseY, layout.listX() + 2, rowY, layout.listW() - 4, ROW_HEIGHT - 1);
            if (selected || hovered) graphics.fill(layout.listX() + 2, rowY, layout.listRight() - 2, rowY + ROW_HEIGHT - 1, selected ? 0x885664B7 : 0x553B4052);
            Component prefix = Component.translatable(directory
                    ? "gui.astral_craft.creator.file_browser.directory_prefix"
                    : "gui.astral_craft.creator.file_browser.file_prefix");
            String name = prefix.getString() + entry.getFileName();
            graphics.text(this.font, this.font.plainSubstrByWidth(name, layout.listW() - 10),
                    layout.listX() + 5, rowY + 5, directory ? 0xFFE5D38A : 0xFFD7E4F2);
        }
    }

    private void renderButton(GuiGraphicsExtractor graphics, int x, int y, int width, int height, String key, int mouseX, int mouseY, int color) {
        boolean hovered = this.isInside(mouseX, mouseY, x, y, width, height);
        AstralFancyButton.renderButton(graphics, this.font, Component.translatable(key),
                x, y, width, height, false, hovered,
                AstralFancyButton.ButtonStyle.button(color));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        BrowserLayout layout = this.layout();
        double mouseX = event.x();
        double mouseY = event.y();
        if (this.isInside(mouseX, mouseY, layout.pathX(), layout.pathY() - 12, layout.pathW(), 11)) {
            this.pathMenuOpen = !this.pathMenuOpen;
            return true;
        }
        if (this.pathMenuOpen && this.handlePathMenuClick(layout, mouseX, mouseY)) return true;
        if (this.isInside(mouseX, mouseY, layout.upX(), layout.pathY(), layout.smallButtonW(), 20)) {
            Path parent = this.directory.getParent();
            if (parent != null) this.navigate(parent);
            return true;
        }

        if (this.isInside(mouseX, mouseY, layout.goX(), layout.pathY(), layout.smallButtonW(), 20)) {
            this.navigateFromText();
            return true;
        }

        if (this.isInside(mouseX, mouseY, layout.refreshX(), layout.pathY(), layout.smallButtonW(), 20)) {
            this.refreshEntries();
            return true;
        }

        if (this.mode == BrowserMode.FOLDER && this.isInside(mouseX, mouseY, layout.createFolderX(), layout.newFolderY(), layout.createFolderW(), 20)) {
            this.createFolder();
            return true;
        }

        if (this.isInside(mouseX, mouseY, layout.selectX(), layout.actionY(), layout.actionButtonW(), 22)) {
            if (this.mode == BrowserMode.FOLDER) this.finish(this.directory);
            else if (this.selectedFile != null) this.finish(this.selectedFile);
            return true;
        }

        if (this.isInside(mouseX, mouseY, layout.cancelX(), layout.actionY(), layout.actionButtonW(), 22)) {
            this.onClose();
            return true;
        }

        int visibleRows = Math.max(1, layout.listH() / ROW_HEIGHT);
        int start = Math.clamp(this.entries.size() - visibleRows, 0, this.scrollRows);
        if (this.isInside(mouseX, mouseY, layout.listX(), layout.listY(), layout.listW(), layout.listH())) {
            int row = (int) ((mouseY - layout.listY() - 2) / ROW_HEIGHT);
            int index = start + row;
            if (row >= 0 && index >= 0 && index < this.entries.size()) {
                Path entry = this.entries.get(index);
                if (Files.isDirectory(entry)) {
                    if (doubleClick) this.navigate(entry);
                    else this.selectedFile = null;
                } else if (this.mode == BrowserMode.FILE) {
                    this.selectedFile = entry;
                    if (doubleClick) this.finish(entry);
                }
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        BrowserLayout layout = this.layout();
        if (this.isInside(mouseX, mouseY, layout.listX(), layout.listY(), layout.listW(), layout.listH())) {
            int visibleRows = Math.max(1, layout.listH() / ROW_HEIGHT);
            int max = Math.max(0, this.entries.size() - visibleRows);
            this.scrollRows = Math.clamp(this.scrollRows - (int) Math.signum(deltaY) * 3L, 0, max);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ENTER && this.pathBox != null && this.pathBox.isFocused()) {
            this.navigateFromText();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    private void finish(Path path) {
        if (path == null) return;
        this.selected.accept(path.toAbsolutePath().normalize());
        this.onClose();
    }

    private void navigateFromText() {
        if (this.pathBox == null || this.pathBox.getValue().isBlank()) return;
        try {
            Path path = Path.of(this.pathBox.getValue()).toAbsolutePath().normalize();
            if (Files.isRegularFile(path) && this.mode == BrowserMode.FILE && this.accepts(path)) {
                this.finish(path);
                return;
            }
            this.navigate(path);
        } catch (Exception ignored) {
            this.status = Component.translatable("gui.astral_craft.creator.file_browser.invalid_path");
        }
    }

    private void navigate(Path path) {
        if (path == null || !Files.isDirectory(path)) {
            this.status = Component.translatable("gui.astral_craft.creator.file_browser.invalid_path");
            return;
        }
        this.directory = path.toAbsolutePath().normalize();
        this.pathMenuOpen = false;
        this.selectedFile = null;
        this.scrollRows = 0;
        this.status = Component.empty();
        if (this.pathBox != null) this.pathBox.setValue(this.directory.toString());
        this.refreshEntries();
    }

    private void refreshEntries() {
        List<Path> loaded = new ArrayList<>();
        try (Stream<Path> stream = Files.list(this.directory)) {
            stream.filter(path -> Files.isDirectory(path) || this.mode == BrowserMode.FILE && this.accepts(path)).forEach(loaded::add);
            loaded.sort(Comparator.comparing((Path path) -> !Files.isDirectory(path)).thenComparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)));
            this.status = Component.empty();
        } catch (IOException | SecurityException ignored) {
            this.status = Component.translatable("gui.astral_craft.creator.file_browser.read_error");
        }
        this.entries = List.copyOf(loaded);
        int visibleRows = Math.max(1, this.layout().listH() / ROW_HEIGHT);
        this.scrollRows = Math.clamp(this.scrollRows, 0, Math.max(0, this.entries.size() - visibleRows));
    }

    private void createFolder() {
        if (this.folderNameBox == null || this.folderNameBox.getValue().isBlank()) return;
        String raw = this.folderNameBox.getValue().strip();
        if (raw.equals(".") || raw.equals("..") || raw.contains("/") || raw.contains("\\")) {
            this.status = Component.translatable("gui.astral_craft.creator.file_browser.invalid_folder_name");
            return;
        }
        try {
            Path created = Files.createDirectory(this.directory.resolve(raw));
            this.folderNameBox.setValue("");
            this.refreshEntries();
            this.navigate(created);
        } catch (IOException | SecurityException ignored) {
            this.status = Component.translatable("gui.astral_craft.creator.file_browser.create_error");
        }
    }

    private boolean accepts(Path path) {
        if (!Files.isRegularFile(path)) return false;
        if (this.extensions.isEmpty()) return true;
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        return dot >= 0 && this.extensions.contains(name.substring(dot + 1));
    }

    private Path initialDirectory(Path start) {
        Path fallback = this.minecraft.gameDirectory.toPath();
        Path value = start == null ? fallback : start.toAbsolutePath().normalize();
        if (Files.isRegularFile(value)) value = value.getParent();
        if (value == null || !Files.isDirectory(value)) value = fallback;
        return value.toAbsolutePath().normalize();
    }

    private boolean hoveredControl(BrowserLayout layout, double mouseX, double mouseY) {
        if (this.isInside(mouseX, mouseY, layout.upX(), layout.pathY(), layout.smallButtonW(), 20)
                || this.isInside(mouseX, mouseY, layout.goX(), layout.pathY(), layout.smallButtonW(), 20)
                || this.isInside(mouseX, mouseY, layout.refreshX(), layout.pathY(), layout.smallButtonW(), 20)
                || this.isInside(mouseX, mouseY, layout.selectX(), layout.actionY(), layout.actionButtonW(), 22)
                || this.isInside(mouseX, mouseY, layout.cancelX(), layout.actionY(), layout.actionButtonW(), 22)) return true;
        if (this.mode == BrowserMode.FOLDER && this.isInside(mouseX, mouseY, layout.createFolderX(), layout.newFolderY(), layout.createFolderW(), 20)) return true;
        return this.isInside(mouseX, mouseY, layout.listX(), layout.listY(), layout.listW(), layout.listH());
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private BrowserLayout layout() {
        int panelW = Math.clamp(this.width - 24, 280, 620);
        int panelH = Math.clamp(this.height - 24, 230, 410);
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;
        int innerX = panelX + 9;
        int innerW = panelW - 18;
        int smallButtonW = Math.clamp(innerW / 8, 52, 78);
        int pathY = panelY + 44;
        int pathW = Math.max(90, innerW - (smallButtonW + GAP) * 3);
        int upX = innerX + pathW + GAP;
        int goX = upX + smallButtonW + GAP;
        int refreshX = goX + smallButtonW + GAP;
        int actionY = panelY + panelH - 31;
        int statusY = actionY - 15;
        int newFolderY = this.mode == BrowserMode.FOLDER ? statusY - 25 : statusY;
        int listY = pathY + 28;
        int listBottom = this.mode == BrowserMode.FOLDER ? newFolderY - 7 : statusY - 7;
        int listH = Math.max(60, listBottom - listY);
        int actionButtonW = Math.max(72, (innerW - GAP) / 2);
        int newFolderBoxW = Math.max(90, innerW * 2 / 3);
        int createFolderW = innerW - newFolderBoxW - GAP;
        return new BrowserLayout(panelX, panelY, panelW, panelH, innerX, innerW, pathY, pathW, smallButtonW, upX, goX, refreshX,
                listY, listH, newFolderY, newFolderBoxW, createFolderW, statusY, actionY, actionButtonW);
    }

    public enum BrowserMode {
        FOLDER,
        FILE
    }

    private record BrowserLayout(int panelX, int panelY, int panelW, int panelH, int innerX, int innerW, int pathY, int pathW,
                                 int smallButtonW, int upX, int goX, int refreshX, int listY, int listH, int newFolderY,
                                 int newFolderBoxW, int createFolderW, int statusY, int actionY, int actionButtonW) {
        private int panelRight() { return this.panelX + this.panelW; }
        private int pathX() { return this.innerX; }
        private int listX() { return this.innerX; }
        private int listW() { return this.innerW; }
        private int listRight() { return this.listX() + this.listW(); }
        private int createFolderX() { return this.innerX + this.newFolderBoxW + GAP; }
        private int selectX() { return this.innerX; }
        private int cancelX() { return this.innerX + this.actionButtonW + GAP; }
    }
}
