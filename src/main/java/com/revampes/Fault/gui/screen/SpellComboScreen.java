package com.revampes.Fault.gui.screen;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import com.revampes.Fault.gui.block.BlockNode;
import com.revampes.Fault.gui.block.BlockType;
import com.revampes.Fault.gui.ClickGui;
import com.revampes.Fault.gui.component.impl.BindCaptureComponent;
import com.revampes.Fault.gui.component.impl.BlockEditorPanel;
import com.revampes.Fault.modules.impl.wynncraft.SpellCombo;
import com.revampes.Fault.utility.BindUtils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static com.revampes.Fault.Revampes.mc;

public class SpellComboScreen extends Screen {
    private static final float LIST_WIDTH = 330f;
    private static final float ROW_HEIGHT = 38f;
    private static final int COLOR_TEXT_WHITE = Color.WHITE.getRGB();

    private final SpellCombo module;
    private final List<ComboRow> rows = new ArrayList<>();
    private BlockEditorPanel blockPanel;
    private int selectedIndex = -1;

    public SpellComboScreen(SpellCombo module) {
        super(Text.literal("Spell Combo"));
        this.module = module;
    }

    @Override
    protected void init() {
        module.loadCombosFromStorage();
        rebuildRows();
        if (module.getCombos().isEmpty()) {
            module.addCombo();
            rebuildRows();
        }

        if (selectedIndex < 0 && !rows.isEmpty()) {
            selectedIndex = 0;
        }
        rebuildBlockPanel();
    }

    private void rebuildRows() {
        rows.clear();
        List<SpellCombo.ComboProfile> combos = module.getCombos();
        for (int i = 0; i < combos.size(); i++) {
            rows.add(new ComboRow(combos.get(i), i));
        }
        if (selectedIndex >= rows.size()) {
            selectedIndex = rows.isEmpty() ? -1 : rows.size() - 1;
        }
    }

    private SpellCombo.ComboProfile getSelectedCombo() {
        if (selectedIndex < 0 || selectedIndex >= module.getCombos().size()) {
            return null;
        }
        return module.getCombos().get(selectedIndex);
    }

    private void rebuildBlockPanel() {
        SpellCombo.ComboProfile selected = getSelectedCombo();
        if (selected == null) {
            blockPanel = null;
            return;
        }

        float panelX = 360f;
        float panelY = 44f;
        float panelWidth = Math.max(260f, this.width - panelX - 18f);
        float panelHeight = Math.max(160f, this.height - 62f);
        blockPanel = new BlockEditorPanel(
                selected.blocks,
                () -> "When " + BindUtils.formatBind(selected.triggerCode) + " button is being clicked then",
                module::saveCombosToStorage,
                panelX,
                panelY,
                panelWidth,
                panelHeight
        );
    }

    private void ensurePanelBounds() {
        if (blockPanel == null) {
            return;
        }

        float panelX = 360f;
        float panelY = 44f;
        float panelWidth = Math.max(260f, this.width - panelX - 18f);
        float panelHeight = Math.max(160f, this.height - 62f);
        blockPanel.updateBounds(panelX, panelY, panelWidth, panelHeight);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        ensurePanelBounds();

        context.fill(0, 0, this.width, this.height, new Color(18, 18, 22, 245).getRGB());
        context.drawCenteredTextWithShadow(mc.textRenderer, "Spell Combo Editor", this.width / 2, 10, COLOR_TEXT_WHITE);

        int addX = 16;
        int addY = 28;
        context.fill(addX, addY, addX + 110, addY + 20, new Color(80, 120, 80).getRGB());
        context.drawText(mc.textRenderer, Text.literal("[+] Add Combo"), addX + 14, addY + 6, COLOR_TEXT_WHITE, false);

        context.fill(12, 52, (int) LIST_WIDTH, this.height - 12, new Color(36, 36, 44, 240).getRGB());
        context.fill((int) LIST_WIDTH + 18, 52, this.width - 12, this.height - 12, new Color(28, 28, 34, 240).getRGB());

        for (int i = 0; i < rows.size(); i++) {
            ComboRow row = rows.get(i);
            row.updateBounds(20, 64 + (i * (ROW_HEIGHT + 8)), LIST_WIDTH - 28, ROW_HEIGHT);
            row.render(context, mouseX, mouseY, delta, i == selectedIndex);
        }

        if (blockPanel != null) {
            blockPanel.render(context, mouseX, mouseY, delta);
        } else {
            context.drawText(mc.textRenderer, Text.literal("Select a combo and click Configure to edit blocks."), 380, 70, Color.WHITE.getRGB(), false);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        for (ComboRow row : rows) {
            if (row.trigger.isListening()) {
                row.trigger.mouseClicked(mouseX, mouseY, button);
                module.saveCombosToStorage();
                return true;
            }
        }

        if (blockPanel != null && blockPanel.isPointInside(mouseX, mouseY)) {
            blockPanel.mouseClicked(mouseX, mouseY, button);
            return true;
        }

        if (mouseX >= 16 && mouseX <= 126 && mouseY >= 28 && mouseY <= 48 && button == 0) {
            module.addCombo();
            rebuildRows();
            selectedIndex = rows.size() - 1;
            rebuildBlockPanel();
            return true;
        }

        for (ComboRow row : rows) {
            if (row.mouseClicked(mouseX, mouseY, button)) {
                if (row.requestedSelection >= 0) {
                    selectedIndex = row.requestedSelection;
                    rebuildBlockPanel();
                }
                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        if (blockPanel != null) {
            blockPanel.mouseReleased(mouseX, mouseY, button);
            return true;
        }

        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        if (blockPanel != null) {
            blockPanel.mouseDragged(mouseX, mouseY, button, offsetX, offsetY);
            return true;
        }

        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (blockPanel != null) {
            return blockPanel.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        int scanCode = input.scancode();
        int modifiers = input.modifiers();

        for (ComboRow row : rows) {
            if (row.trigger.isListening() && row.trigger.keyPressed(keyCode, scanCode, modifiers)) {
                module.saveCombosToStorage();
                return true;
            }
        }

        if (blockPanel != null && blockPanel.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.client.setScreen(ClickGui.getInstance());
            return true;
        }

        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        char chr = Character.toChars(input.codepoint())[0];
        int modifiers = input.modifiers();

        if (blockPanel != null && blockPanel.charTyped(chr, modifiers)) {
            return true;
        }

        return super.charTyped(input);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void openCombo(int index) {
        if (index < 0 || index >= rows.size()) {
            return;
        }

        selectedIndex = index;
        rebuildBlockPanel();
    }

    @Override
    public void close() {
        this.client.setScreen(ClickGui.getInstance());
    }

    private final class ComboRow {
        private final SpellCombo.ComboProfile combo;
        private final int index;
        private final BindCaptureComponent trigger;
        private float x;
        private float y;
        private float width;
        private float height;
        private int requestedSelection = -1;

        private ComboRow(SpellCombo.ComboProfile combo, int index) {
            this.combo = combo;
            this.index = index;
            this.trigger = new BindCaptureComponent(
                    "Trigger",
                    () -> combo.triggerCode,
                    value -> {
                        combo.triggerCode = value;
                        module.saveCombosToStorage();
                    },
                    0,
                    0,
                    140,
                    22
            );
        }

        private void updateBounds(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            trigger.updateBounds(x + 8, y + 8, 140, 22);
        }

        private void render(DrawContext context, int mouseX, int mouseY, float delta, boolean selected) {
            Color fill = selected ? new Color(74, 90, 150) : new Color(55, 55, 64);
            context.fill((int) x, (int) y, (int) (x + width), (int) (y + height), fill.getRGB());
            context.drawText(mc.textRenderer, Text.literal("Combo " + (index + 1)), (int) (x + 8), (int) (y + 6), Color.WHITE.getRGB(), false);

            trigger.render(context, mouseX, mouseY, delta);

            float buttonY = y + 8;
            float configureX = x + width - 132;
            float deleteX = x + width - 32;
            context.fill((int) configureX, (int) buttonY, (int) (configureX + 86), (int) (buttonY + 22), new Color(96, 96, 130).getRGB());
            context.drawText(mc.textRenderer, Text.literal("Configure"), (int) (configureX + 10), (int) (buttonY + 7), Color.WHITE.getRGB(), false);
            context.fill((int) deleteX, (int) buttonY, (int) (deleteX + 20), (int) (buttonY + 22), new Color(140, 70, 70).getRGB());
            context.drawText(mc.textRenderer, Text.literal("x"), (int) (deleteX + 7), (int) (buttonY + 7), Color.WHITE.getRGB(), false);
        }

        private boolean mouseClicked(double mouseX, double mouseY, int button) {
            requestedSelection = -1;

            if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
                requestedSelection = index;
            }

            if (mouseX >= x + width - 32 && mouseX <= x + width - 12 && mouseY >= y + 8 && mouseY <= y + 30 && button == 0) {
                module.removeCombo(index);
                rebuildRows();
                if (selectedIndex >= rows.size()) {
                    selectedIndex = rows.isEmpty() ? -1 : rows.size() - 1;
                }
                rebuildBlockPanel();
                return true;
            }

            if (mouseX >= x + width - 132 && mouseX <= x + width - 46 && mouseY >= y + 8 && mouseY <= y + 30) {
                requestedSelection = index;
                if (button == 1 || button == 0) {
                    openCombo(index);
                    return true;
                }
            }

            if (mouseX >= x + 8 && mouseX <= x + 8 + 140 && mouseY >= y + 8 && mouseY <= y + 30) {
                trigger.mouseClicked(mouseX, mouseY, button);
                return true;
            }

            if (requestedSelection >= 0) {
                openCombo(index);
                return true;
            }

            return false;
        }
    }
}