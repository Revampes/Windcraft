package com.revampes.Fault.gui.component.impl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import com.revampes.Fault.gui.block.BlockNode;
import com.revampes.Fault.gui.block.BlockType;
import com.revampes.Fault.gui.component.Component;
import com.revampes.Fault.modules.ModuleManager;
import com.revampes.Fault.utility.RenderUtils;

import java.awt.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Supplier;

import static com.revampes.Fault.Revampes.mc;

public class BlockEditorPanel extends Component {
    private static final float PALETTE_WIDTH = 175f;
    private static final float HEADER_HEIGHT = 26f;
    private static final float ROW_HEIGHT = 32f;
    private static final float BODY_PADDING = 10f;

    private final List<BlockNode> rootBlocks;
    private final List<BlockLayout> layouts = new ArrayList<>();
    private final List<PaletteLayout> paletteLayouts = new ArrayList<>();
    private final Deque<BlockNode> scopeStack = new ArrayDeque<>();
    private Supplier<String> headerSupplier;
    private Runnable onChange;

    private BlockNode draggingBlock;
    private List<BlockNode> draggingList;
    private float draggingMouseY;
    private BlockNode editingValueBlock;
    private String editingBuffer = "";

    public BlockEditorPanel(List<BlockNode> rootBlocks, Supplier<String> headerSupplier, Runnable onChange, float x, float y, float width, float height) {
        super(x, y, width, height);
        this.rootBlocks = rootBlocks;
        this.headerSupplier = headerSupplier;
        this.onChange = onChange;
    }

    public void setHeaderSupplier(Supplier<String> headerSupplier) {
        this.headerSupplier = headerSupplier;
    }

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    public void resetScope() {
        scopeStack.clear();
        editingValueBlock = null;
        editingBuffer = "";
    }

    public List<BlockNode> getCurrentBlocks() {
        return scopeStack.isEmpty() ? rootBlocks : scopeStack.peek().children;
    }

    private BlockNode currentContainer() {
        return scopeStack.peek();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        isHovered = isPointInside(mouseX, mouseY);
        boolean isLight = ModuleManager.ui.isLightTheme();
        int borderColor = isLight ? 0xFF000000 : 0xFFFFFFFF;
        int backgroundColor = ModuleManager.ui.useCustomColors() ? ModuleManager.ui.panelColor.getRGB() : (isLight ? new Color(235, 235, 235).getRGB() : new Color(55, 55, 55).getRGB());
        int paletteColor = ModuleManager.ui.useCustomColors() ? ModuleManager.ui.panelColor.getRGB() : (isLight ? new Color(245, 245, 245).getRGB() : new Color(68, 68, 68).getRGB());
        int workspaceColor = ModuleManager.ui.useCustomColors() ? ModuleManager.ui.panelColor.getRGB() : (isLight ? new Color(225, 225, 225).getRGB() : new Color(62, 62, 62).getRGB());

        context.fill((int) x, (int) y, (int) (x + width), (int) (y + height), backgroundColor);
        RenderUtils.drawBorder(context, (int) x, (int) y, (int) width, (int) height, borderColor);

        float paletteX = x + 8;
        float paletteY = y + 8;
        float workspaceX = x + PALETTE_WIDTH + 14;
        float workspaceW = width - PALETTE_WIDTH - 22;

        context.fill((int) paletteX, (int) paletteY, (int) (paletteX + PALETTE_WIDTH - 10), (int) (y + height - 8), paletteColor);
        context.fill((int) workspaceX, (int) paletteY, (int) (workspaceX + workspaceW), (int) (y + height - 8), workspaceColor);

        context.drawText(mc.textRenderer, Text.literal("Blocks"), (int) (paletteX + 8), (int) (paletteY + 5), isLight ? Color.BLACK.getRGB() : Color.WHITE.getRGB(), false);

        paletteLayouts.clear();
        float paletteItemY = paletteY + 22;
        for (BlockType type : BlockType.paletteValues()) {
            paletteLayouts.add(new PaletteLayout(type, paletteX + 8, paletteItemY, PALETTE_WIDTH - 26, ROW_HEIGHT - 2));
            paletteItemY += ROW_HEIGHT + 4;
        }

        for (PaletteLayout layout : paletteLayouts) {
            boolean hovered = layout.contains(mouseX, mouseY);
            int fill = hovered ? new Color(150, 170, 240).getRGB() : new Color(120, 120, 120).getRGB();
            context.fill((int) layout.x, (int) layout.y, (int) (layout.x + layout.width), (int) (layout.y + layout.height), fill);
            context.drawText(mc.textRenderer, Text.literal(layout.type.getLabel()), (int) (layout.x + 6), (int) (layout.y + 9), Color.WHITE.getRGB(), false);
        }

        context.drawText(mc.textRenderer, Text.literal(headerSupplier == null ? "When ? button is being clicked then" : headerSupplier.get()), (int) (workspaceX + 8), (int) (paletteY + 5), isLight ? Color.BLACK.getRGB() : Color.WHITE.getRGB(), false);

        if (!scopeStack.isEmpty()) {
            int backX = (int) (workspaceX + workspaceW - 54);
            int backY = (int) (paletteY + 2);
            context.fill(backX, backY, backX + 46, backY + 18, new Color(120, 120, 120).getRGB());
            context.drawText(mc.textRenderer, Text.literal("Back"), backX + 10, backY + 5, Color.WHITE.getRGB(), false);
        }

        layouts.clear();
        float currentY = paletteY + HEADER_HEIGHT + 4;
        List<BlockNode> blocks = getCurrentBlocks();
        if (blocks.isEmpty()) {
            context.drawText(mc.textRenderer, Text.literal("Drop or click blocks here."), (int) (workspaceX + 10), (int) currentY, isLight ? Color.DARK_GRAY.getRGB() : Color.LIGHT_GRAY.getRGB(), false);
        }

        for (int i = 0; i < blocks.size(); i++) {
            BlockNode block = blocks.get(i);
            float blockHeight = getBlockHeight(block);
            float blockX = workspaceX + 8;
            float blockW = workspaceW - 16;
            boolean draggingGhost = draggingBlock == block;

            if (!draggingGhost) {
                drawBlock(context, block, blockX, currentY, blockW, blockHeight, mouseX, mouseY, false);
            }

            layouts.add(new BlockLayout(block, blocks, i, blockX, currentY, blockW, blockHeight));
            currentY += blockHeight + 6;
        }

        if (draggingBlock != null) {
            float ghostY = draggingMouseY - 16;
            drawBlock(context, draggingBlock, workspaceX + 14, ghostY, workspaceW - 28, getBlockHeight(draggingBlock), mouseX, mouseY, true);
        }

        if (editingValueBlock != null) {
            context.drawText(mc.textRenderer, Text.literal("Editing value: " + editingBuffer), (int) (workspaceX + 8), (int) (y + height - 18), Color.WHITE.getRGB(), false);
        }
    }

    private void drawBlock(DrawContext context, BlockNode block, float blockX, float blockY, float blockW, float blockH, int mouseX, int mouseY, boolean ghost) {
        boolean isLight = ModuleManager.ui.isLightTheme();
        int color = getBlockColor(block.type, ghost);
        context.fill((int) blockX, (int) blockY, (int) (blockX + blockW), (int) (blockY + blockH), color);
        RenderUtils.drawBorder(context, (int) blockX, (int) blockY, (int) blockW, (int) blockH, isLight ? 0xFF000000 : 0xFFFFFFFF);

        context.drawText(mc.textRenderer, Text.literal(blockLabel(block)), (int) (blockX + 6), (int) (blockY + 8), Color.WHITE.getRGB(), false);

        if (block.type.hasEditableValue()) {
            int chipW = 48;
            int chipX = (int) (blockX + blockW - chipW - 48);
            int chipY = (int) (blockY + 7);
            context.fill(chipX, chipY, chipX + chipW, chipY + 18, editingValueBlock == block ? new Color(255, 215, 120).getRGB() : new Color(90, 90, 90).getRGB());
            context.drawText(mc.textRenderer, Text.literal(editingValueBlock == block ? editingBuffer : String.valueOf(block.value)), chipX + 6, chipY + 5, Color.WHITE.getRGB(), false);
        }

        if (block.type.isContainer()) {
            int chipX = (int) (blockX + blockW - 40);
            int chipY = (int) (blockY + 7);
            context.fill(chipX, chipY, chipX + 32, chipY + 18, new Color(75, 105, 165).getRGB());
            context.drawText(mc.textRenderer, Text.literal(scopeStack.peek() == block ? "<" : ">"), chipX + 12, chipY + 5, Color.WHITE.getRGB(), false);
        }

        if (block.type == BlockType.REPEAT) {
            int childCountX = (int) (blockX + blockW - 74);
            int childCountY = (int) (blockY + 7);
            context.fill(childCountX, childCountY, childCountX + 28, childCountY + 18, new Color(100, 100, 100).getRGB());
            context.drawText(mc.textRenderer, Text.literal(String.valueOf(block.children.size())), childCountX + 10, childCountY + 5, Color.WHITE.getRGB(), false);
        }

        if (block.type.isContainer() && block.type == BlockType.REPEAT) {
            context.drawText(mc.textRenderer, Text.literal("open"), (int) (blockX + 6), (int) (blockY + blockH - 10), new Color(220, 220, 220).getRGB(), false);
        }
    }

    private int getBlockColor(BlockType type, boolean ghost) {
        Color color = switch (type) {
            case REPEAT -> new Color(120, 70, 160);
            case CAST_SPELL -> new Color(65, 140, 200);
            case WAIT -> new Color(160, 110, 55);
            case LEFT_CLICK -> new Color(55, 150, 85);
            case RIGHT_CLICK -> new Color(160, 70, 70);
            case WHEN_TRIGGER -> new Color(90, 120, 190);
        };

        if (ghost) {
            return new Color(color.getRed(), color.getGreen(), color.getBlue(), 150).getRGB();
        }

        return color.getRGB();
    }

    private String blockLabel(BlockNode block) {
        return switch (block.type) {
            case REPEAT -> "Repeat for " + block.value + " times";
            case CAST_SPELL -> "Cast " + block.value + " Spell then";
            case WAIT -> "Wait for " + block.value + " ms";
            case LEFT_CLICK -> "Left click";
            case RIGHT_CLICK -> "Right click";
            case WHEN_TRIGGER -> "When trigger";
        };
    }

    private float getBlockHeight(BlockNode block) {
        return block.type == BlockType.REPEAT ? ROW_HEIGHT + 6 : ROW_HEIGHT;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editingValueBlock != null) {
            if (keyCode == 256) {
                cancelEditing();
                return true;
            }
            if (keyCode == 257 || keyCode == 335) {
                applyEditedValue();
                return true;
            }
            if (keyCode == 259) {
                if (!editingBuffer.isEmpty()) {
                    editingBuffer = editingBuffer.substring(0, editingBuffer.length() - 1);
                }
                return true;
            }
            return false;
        }

        if (keyCode == 259 && !scopeStack.isEmpty()) {
            exitScope();
            return true;
        }

        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (editingValueBlock == null) {
            return false;
        }

        if (Character.isDigit(chr)) {
            editingBuffer += chr;
            return true;
        }

        return false;
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (!scopeStack.isEmpty()) {
                int backX = (int) (x + PALETTE_WIDTH + 14 + width - PALETTE_WIDTH - 22 - 54);
                int backY = (int) (y + 10);
                if (mouseX >= backX && mouseX <= backX + 46 && mouseY >= backY && mouseY <= backY + 18) {
                    exitScope();
                    return;
                }
            }

            for (PaletteLayout layout : paletteLayouts) {
                if (layout.contains(mouseX, mouseY)) {
                    BlockNode created = new BlockNode(layout.type);
                    getCurrentBlocks().add(created);
                    changed();
                    return;
                }
            }
        }

        for (int i = layouts.size() - 1; i >= 0; i--) {
            BlockLayout layout = layouts.get(i);
            if (!layout.contains(mouseX, mouseY)) {
                continue;
            }

            if (layout.valueChipContains(mouseX, mouseY) && layout.block.type.hasEditableValue()) {
                editingValueBlock = layout.block;
                editingBuffer = String.valueOf(layout.block.value);
                return;
            }

            if (layout.containerChipContains(mouseX, mouseY) && layout.block.type.isContainer() && button == 0) {
                enterScope(layout.block);
                return;
            }

            if (button == 1) {
                layout.parent.remove(layout.block);
                changed();
                return;
            }

            if (button == 0) {
                draggingBlock = layout.block;
                draggingList = layout.parent;
                draggingMouseY = (float) mouseY;
                return;
            }
        }
    }

    public void mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingBlock != null) {
            draggingMouseY = (float) mouseY;
        }
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingBlock == null || draggingList == null) {
            return;
        }

        List<BlockNode> list = draggingList;
        list.remove(draggingBlock);

        int insertIndex = list.size();
        for (int i = 0; i < layouts.size(); i++) {
            BlockLayout layout = layouts.get(i);
            if (layout.parent != list) {
                continue;
            }
            if (mouseY < layout.y + layout.height / 2f) {
                insertIndex = layout.index;
                break;
            }
        }

        if (insertIndex < 0 || insertIndex > list.size()) {
            insertIndex = list.size();
        }

        list.add(insertIndex, draggingBlock);
        draggingBlock = null;
        draggingList = null;
        changed();
    }

    private void enterScope(BlockNode node) {
        scopeStack.push(node);
        editingValueBlock = null;
        editingBuffer = "";
    }

    private void exitScope() {
        if (!scopeStack.isEmpty()) {
            scopeStack.pop();
        }
    }

    private void cancelEditing() {
        editingValueBlock = null;
        editingBuffer = "";
    }

    private void applyEditedValue() {
        if (editingValueBlock == null) {
            return;
        }

        try {
            int value = Integer.parseInt(editingBuffer.isBlank() ? "0" : editingBuffer);
            BlockType type = editingValueBlock.type;
            value = Math.max(type.getMinValue(), Math.min(type.getMaxValue(), value));
            editingValueBlock.value = value;
            changed();
        } catch (NumberFormatException ignored) {
        }

        cancelEditing();
    }

    private void changed() {
        if (onChange != null) {
            onChange.run();
        }
    }

    private static final class PaletteLayout {
        private final BlockType type;
        private final float x;
        private final float y;
        private final float width;
        private final float height;

        private PaletteLayout(BlockType type, float x, float y, float width, float height) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }

    private static final class BlockLayout {
        private final BlockNode block;
        private final List<BlockNode> parent;
        private final int index;
        private final float x;
        private final float y;
        private final float width;
        private final float height;

        private BlockLayout(BlockNode block, List<BlockNode> parent, int index, float x, float y, float width, float height) {
            this.block = block;
            this.parent = parent;
            this.index = index;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }

        private boolean valueChipContains(double mouseX, double mouseY) {
            int chipW = 48;
            int chipX = (int) (x + width - chipW - 48);
            int chipY = (int) (y + 7);
            return mouseX >= chipX && mouseX <= chipX + chipW && mouseY >= chipY && mouseY <= chipY + 18;
        }

        private boolean containerChipContains(double mouseX, double mouseY) {
            if (!block.type.isContainer()) {
                return false;
            }
            int chipX = (int) (x + width - 40);
            int chipY = (int) (y + 7);
            return mouseX >= chipX && mouseX <= chipX + 32 && mouseY >= chipY && mouseY <= chipY + 18;
        }
    }
}