package com.revampes.Fault.gui.block;

public enum BlockType {
    WHEN_TRIGGER("When ? button is being clicked then", false, false, 0, 0, 0),
    REPEAT("Repeat for n times", true, true, 1, 1, 9999),
    CAST_SPELL("Cast n Spell then", true, false, 1, 1, 4),
    WAIT("Wait for n ms", true, false, 100, 0, 100000),
    LEFT_CLICK("Left click", false, false, 0, 0, 0),
    RIGHT_CLICK("Right click", false, false, 0, 0, 0);

    private final String label;
    private final boolean editableValue;
    private final boolean container;
    private final int defaultValue;
    private final int minValue;
    private final int maxValue;

    BlockType(String label, boolean editableValue, boolean container, int defaultValue, int minValue, int maxValue) {
        this.label = label;
        this.editableValue = editableValue;
        this.container = container;
        this.defaultValue = defaultValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    public String getLabel() {
        return label;
    }

    public boolean hasEditableValue() {
        return editableValue;
    }

    public boolean isContainer() {
        return container;
    }

    public int getDefaultValue() {
        return defaultValue;
    }

    public int getMinValue() {
        return minValue;
    }

    public int getMaxValue() {
        return maxValue;
    }

    public static BlockType[] paletteValues() {
        return new BlockType[]{REPEAT, CAST_SPELL, WAIT, LEFT_CLICK, RIGHT_CLICK};
    }
}