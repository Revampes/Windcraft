package com.revampes.Fault.gui.component;

import net.minecraft.client.gui.DrawContext;
import com.revampes.Fault.settings.Setting;

import java.awt.Color;

public abstract class Component {
    // Shared Color Constants
    protected static final int COLOR_LIGHT_TEXT = Color.BLACK.getRGB();
    protected static final int COLOR_DARK_TEXT = Color.WHITE.getRGB();
    protected static final int COLOR_BOX_HOVERED = new Color(200, 200, 200).getRGB();
    protected static final int COLOR_BOX_NORMAL = new Color(180, 180, 180).getRGB();
    protected static final int COLOR_BOX_FOCUSED = new Color(200, 200, 255).getRGB();
    protected static final int COLOR_TEXT_WHITE = Color.WHITE.getRGB();

    protected float x, y, width, height;
    protected boolean isHovered;
    protected Setting setting;

    public Component(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public Component(Setting setting, float x, float y, float width, float height) {
        this(x, y, width, height);
        this.setting = setting;
    }


    public abstract void render(DrawContext context, int mouseX, int mouseY, float delta);

    public abstract boolean keyPressed(int keyCode, int scanCode, int modifiers);

    public abstract void mouseClicked(double mouseX, double mouseY, int button);

    public boolean isHovered(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public void updatePosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void updateBounds(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean isPointInside(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public Setting getSetting() {
        return setting;
    }

    public boolean isVisible() {
        return setting == null || setting.isVisible();
    }
}
