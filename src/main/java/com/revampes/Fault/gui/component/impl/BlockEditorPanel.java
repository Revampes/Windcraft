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

    // Color constants
    private static final int COLOR_BORDER_LIGHT = 0xFF000000;
    private static final int COLOR_BORDER_DARK = 0xFFFFFFFF;
    private static final int COLOR_BACKGROUND_LIGHT = new Color(235, 235, 235).getRGB();
    private static final int COLOR_BACKGROUND_DARK = new Color(55, 55, 55).getRGB();
    private static final int COLOR_PALETTE_LIGHT = new Color(245, 245, 245).getRGB();
    private static final int COLOR_PALETTE_DARK = new Color(68, 68, 68).getRGB();
    private static final int COLOR_WORKSPACE_LIGHT = new Color(225, 225, 225).getRGB();
    private static final int COLOR_WORKSPACE_DARK = new Color(62, 62, 62).getRGB();
    private static final int COLOR_PALETTE_ITEM_HOVERED = new Color(150, 170, 240).getRGB();
    private static final int COLOR_PALETTE_ITEM_NORMAL = new Color(120, 120, 120).getRGB();
    private static final int COLOR_TEXT_DARK_GRAY = Color.DARK_GRAY.getRGB();
    private static final int COLOR_TEXT_LIGHT_GRAY = Color.LIGHT_GRAY.getRGB();
    private static final int COLOR_INSERT_INDICATOR = 0xFFFFFF00;
    private static final int COLOR_VALUE_CHIP_EDITING = new Color(255, 215, 120).getRGB();
    private static final int COLOR_VALUE_CHIP_NORMAL = new Color(90, 90, 90).getRGB();
    private static final int COLOR_CONTAINER_CHIP = new Color(75, 105, 165).getRGB();
    private static final int COLOR_CHILD_COUNT_CHIP = new Color(100, 100, 100).getRGB();
    private static final int COLOR_OPEN_TEXT = new Color(220, 220, 220).getRGB();
    private static final int COLOR_BACK_BUTTON = new Color(120, 120, 120).getRGB();
    
    // Block type colors
    private static final int COLOR_BLOCK_REPEAT = new Color(120, 70, 160).getRGB();
    private static final int COLOR_BLOCK_CAST_SPELL = new Color(65, 140, 200).getRGB();
    private static final int COLOR_BLOCK_WAIT = new Color(160, 110, 55).getRGB();
    private static final int COLOR_BLOCK_LEFT_CLICK = new Color(55, 150, 85).getRGB();
    private static final int COLOR_BLOCK_RIGHT_CLICK = new Color(160, 70, 70).getRGB();
    private static final int COLOR_BLOCK_WHEN_TRIGGER = new Color(90, 120, 190).getRGB();

    private float workspaceScrollOffset = 0f;  // Scroll position for workspace blocks
    private float paletteScrollOffset = 0f;    // Scroll position for palette items
    private float maxWorkspaceScroll = 0f;     // Maximum workspace scroll
    private float maxPaletteScroll = 0f;       // Maximum palette scroll
    private static final float SCROLL_SPEED = 15f;

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
        workspaceScrollOffset = 0f;
        editingValueBlock = null;
        editingBuffer = "";
    }

    public List<BlockNode> getCurrentBlocks() {
        return scopeStack.isEmpty() ? rootBlocks : scopeStack.peek().children;
    }

    private BlockNode currentContainer() {
        return scopeStack.peek();
    }

    private void updatePaletteScrollBounds() {
        int numPaletteItems = BlockType.paletteValues().length;
        float totalPaletteHeight = numPaletteItems * (ROW_HEIGHT + 4);
        float paletteViewHeight = height - HEADER_HEIGHT - 16;
        maxPaletteScroll = Math.max(0, totalPaletteHeight - paletteViewHeight);
        paletteScrollOffset = Math.max(0, Math.min(paletteScrollOffset, maxPaletteScroll));
    }

    private void updateWorkspaceScrollBounds() {
        float totalWorkspaceHeight = 0f;
        List<BlockNode> blocks = getCurrentBlocks();
        for (BlockNode block : blocks) {
            totalWorkspaceHeight += getBlockHeight(block) + 6;
        }
        float workspaceViewHeight = height - HEADER_HEIGHT - 16;
        maxWorkspaceScroll = Math.max(0, totalWorkspaceHeight - workspaceViewHeight);
        workspaceScrollOffset = Math.max(0, Math.min(workspaceScrollOffset, maxWorkspaceScroll));
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!isHovered) return false;

        float paletteX = x + 8;
        float workspaceX = x + PALETTE_WIDTH + 14;
        float workspaceW = width - PALETTE_WIDTH - 22;
        float paletteY = y + 8;
        float workspaceBottom = y + height - 8;

        // Check if scrolling in palette area
        if (mouseX >= paletteX && mouseX <= paletteX + PALETTE_WIDTH - 10 &&
                mouseY >= paletteY + HEADER_HEIGHT && mouseY <= workspaceBottom) {
            paletteScrollOffset -= verticalAmount * SCROLL_SPEED;
            updatePaletteScrollBounds();
            return true;
        }

        // Check if scrolling in workspace area
        if (mouseX >= workspaceX && mouseX <= workspaceX + workspaceW &&
                mouseY >= paletteY + HEADER_HEIGHT && mouseY <= workspaceBottom) {
            workspaceScrollOffset -= verticalAmount * SCROLL_SPEED;
            updateWorkspaceScrollBounds();
            return true;
        }

        return false;
    }
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        isHovered = isPointInside(mouseX, mouseY);
        updatePaletteScrollBounds();
        updateWorkspaceScrollBounds();
        
        boolean isLight = ModuleManager.ui.isLightTheme();
        int borderColor = isLight ? COLOR_BORDER_LIGHT : COLOR_BORDER_DARK;
        int backgroundColor = ModuleManager.ui.useCustomColors() ? ModuleManager.ui.panelColor.getRGB() : (isLight ? COLOR_BACKGROUND_LIGHT : COLOR_BACKGROUND_DARK);
        int paletteColor = ModuleManager.ui.useCustomColors() ? ModuleManager.ui.panelColor.getRGB() : (isLight ? COLOR_PALETTE_LIGHT : COLOR_PALETTE_DARK);
        int workspaceColor = ModuleManager.ui.useCustomColors() ? ModuleManager.ui.panelColor.getRGB() : (isLight ? COLOR_WORKSPACE_LIGHT : COLOR_WORKSPACE_DARK);

        context.fill((int) x, (int) y, (int) (x + width), (int) (y + height), backgroundColor);
        RenderUtils.drawBorder(context, (int) x, (int) y, (int) width, (int) height, borderColor);

        float paletteX = x + 8;
        float paletteY = y + 8;
        float workspaceX = x + PALETTE_WIDTH + 14;
        float workspaceW = width - PALETTE_WIDTH - 22;

        context.fill((int) paletteX, (int) paletteY, (int) (paletteX + PALETTE_WIDTH - 10), (int) (y + height - 8), paletteColor);
        context.fill((int) workspaceX, (int) paletteY, (int) (workspaceX + workspaceW), (int) (y + height - 8), workspaceColor);

        context.drawText(mc.textRenderer, Text.literal("Blocks"), (int) (paletteX + 8), (int) (paletteY + 5), isLight ? COLOR_LIGHT_TEXT : COLOR_DARK_TEXT, false);

        paletteLayouts.clear();
        float paletteItemY = paletteY + 22 - paletteScrollOffset;
        for (BlockType type : BlockType.paletteValues()) {
            paletteLayouts.add(new PaletteLayout(type, paletteX + 8, paletteItemY, PALETTE_WIDTH - 26, ROW_HEIGHT - 2));
            paletteItemY += ROW_HEIGHT + 4;
        }

        // Enable scissor for palette area
        int paletteScissorY1 = (int) (paletteY + HEADER_HEIGHT);
        int paletteScissorY2 = (int) (y + height - 8);
        context.enableScissor((int) paletteX, paletteScissorY1, (int) (paletteX + PALETTE_WIDTH - 10), paletteScissorY2);

        for (PaletteLayout layout : paletteLayouts) {
            boolean hovered = layout.contains(mouseX, mouseY);
            int fill = hovered ? COLOR_PALETTE_ITEM_HOVERED : COLOR_PALETTE_ITEM_NORMAL;
            context.fill((int) layout.x, (int) layout.y, (int) (layout.x + layout.width), (int) (layout.y + layout.height), fill);
            context.drawText(mc.textRenderer, Text.literal(layout.type.getLabel()), (int) (layout.x + 6), (int) (layout.y + 9), Color.WHITE.getRGB(), false);
        }

        context.disableScissor();

        context.drawText(mc.textRenderer, Text.literal(headerSupplier == null ? "When ? button is being clicked then" : headerSupplier.get()), (int) (workspaceX + 8), (int) (paletteY + 5), isLight ? COLOR_LIGHT_TEXT : COLOR_DARK_TEXT, false);

        if (!scopeStack.isEmpty()) {
            int backX = (int) (workspaceX + workspaceW - 54);
            int backY = (int) (paletteY + 2);
            context.fill(backX, backY, backX + 46, backY + 18, COLOR_BACK_BUTTON);
            context.drawText(mc.textRenderer, Text.literal("Back"), backX + 10, backY + 5, Color.WHITE.getRGB(), false);
        }

        layouts.clear();
        float currentY = paletteY + HEADER_HEIGHT + 4 - workspaceScrollOffset;
        List<BlockNode> blocks = getCurrentBlocks();
        if (blocks.isEmpty()) {
            context.drawText(mc.textRenderer, Text.literal("Drop or click blocks here."), (int) (workspaceX + 10), (int) currentY, isLight ? COLOR_TEXT_DARK_GRAY : COLOR_TEXT_LIGHT_GRAY, false);
        }

        // Enable scissor for workspace area
        int workspaceScissorY1 = (int) (paletteY + HEADER_HEIGHT);
        int workspaceScissorY2 = (int) (y + height - 8);
        context.enableScissor((int) workspaceX, workspaceScissorY1, (int) (workspaceX + workspaceW), workspaceScissorY2);

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
            for (BlockLayout layout : layouts) {
                if (mouseY < layout.y + (layout.height / 2f)) {
                    context.fill((int) layout.x, (int) layout.y - 2, (int) (layout.x + layout.width), (int) layout.y, COLOR_INSERT_INDICATOR);
                    break;
                }
            }

            float ghostY = draggingMouseY - 16;
            drawBlock(context, draggingBlock, workspaceX + 14, ghostY, workspaceW - 28, getBlockHeight(draggingBlock), mouseX, mouseY, true);
        }

        context.disableScissor();

        if (editingValueBlock != null) {
            context.drawText(mc.textRenderer, Text.literal("Editing value: " + editingBuffer), (int) (workspaceX + 8), (int) (y + height - 18), Color.WHITE.getRGB(), false);
        }
    }

    private void drawBlock(DrawContext context, BlockNode block, float blockX, float blockY, float blockW, float blockH, int mouseX, int mouseY, boolean ghost) {
        boolean isLight = ModuleManager.ui.isLightTheme();
        int color = getBlockColor(block.type, ghost);
        context.fill((int) blockX, (int) blockY, (int) (blockX + blockW), (int) (blockY + blockH), color);
        RenderUtils.drawBorder(context, (int) blockX, (int) blockY, (int) blockW, (int) blockH, isLight ? COLOR_BORDER_LIGHT : COLOR_BORDER_DARK);

        context.drawText(mc.textRenderer, Text.literal(blockLabel(block)), (int) (blockX + 6), (int) (blockY + 8), Color.WHITE.getRGB(), false);

        if (block.type.hasEditableValue()) {
            int chipW = 48;
            int chipX = (int) (blockX + blockW - chipW - 48);
            int chipY = (int) (blockY + 7);
            context.fill(chipX, chipY, chipX + chipW, chipY + 18, editingValueBlock == block ? COLOR_VALUE_CHIP_EDITING : COLOR_VALUE_CHIP_NORMAL);
            context.drawText(mc.textRenderer, Text.literal(editingValueBlock == block ? editingBuffer : String.valueOf(block.value)), chipX + 6, chipY + 5, Color.WHITE.getRGB(), false);
        }

        if (block.type.isContainer()) {
            int chipX = (int) (blockX + blockW - 40);
            int chipY = (int) (blockY + 7);
            context.fill(chipX, chipY, chipX + 32, chipY + 18, COLOR_CONTAINER_CHIP);
            context.drawText(mc.textRenderer, Text.literal(scopeStack.peek() == block ? "<" : ">"), chipX + 12, chipY + 5, Color.WHITE.getRGB(), false);
        }

        if (block.type == BlockType.REPEAT) {
            int childCountX = (int) (blockX + blockW - 74);
            int childCountY = (int) (blockY + 7);
            context.fill(childCountX, childCountY, childCountX + 28, childCountY + 18, COLOR_CHILD_COUNT_CHIP);
            context.drawText(mc.textRenderer, Text.literal(String.valueOf(block.children.size())), childCountX + 10, childCountY + 5, Color.WHITE.getRGB(), false);
        }

        if (block.type.isContainer() && block.type == BlockType.REPEAT) {
            context.drawText(mc.textRenderer, Text.literal("open"), (int) (blockX + 6), (int) (blockY + blockH - 10), COLOR_OPEN_TEXT, false);
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
                    List<BlockNode> blocks = getCurrentBlocks();
                    blocks.add(created);
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
                insertIndex = i;
                break;
            }
        }

        if (insertIndex < 0) insertIndex = 0;
        if (insertIndex > list.size()) insertIndex =list.size();

        list.add(insertIndex, draggingBlock);
        draggingBlock = null;
        draggingList = null;
        changed();
    }

    private void enterScope(BlockNode node) {
        scopeStack.push(node);
        workspaceScrollOffset = 0f;
        editingValueBlock = null;
        editingBuffer = "";
    }

    private void exitScope() {
        if (!scopeStack.isEmpty()) {
            scopeStack.pop();
            workspaceScrollOffset = 0f;
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