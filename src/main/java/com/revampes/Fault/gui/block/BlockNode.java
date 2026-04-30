package com.revampes.Fault.gui.block;

import java.util.ArrayList;
import java.util.List;

public class BlockNode {
    public BlockType type;
    public int value;
    public List<BlockNode> children = new ArrayList<>();

    public BlockNode() {
    }

    public BlockNode(BlockType type) {
        this(type, type.getDefaultValue());
    }

    public BlockNode(BlockType type, int value) {
        this.type = type;
        this.value = value;
    }

    public BlockNode copy() {
        BlockNode copy = new BlockNode(type, value);
        for (BlockNode child : children) {
            copy.children.add(child.copy());
        }
        return copy;
    }
}