package com.revampes.Fault.gui.component.impl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import com.revampes.Fault.gui.component.Component;
import com.revampes.Fault.modules.Module;
import com.revampes.Fault.modules.ModuleManager;

import java.awt.*;

import static com.revampes.Fault.Revampes.mc;

public class CategoryComponent extends Component {
    private static final int COLOR_SELECTED_LIGHT = new Color(100, 100, 255).getRGB();
    private static final int COLOR_SELECTED_DARK = new Color(60, 60, 180).getRGB();
    private static final int COLOR_HOVERED_LIGHT = new Color(200, 200, 200).getRGB();
    private static final int COLOR_HOVERED_DARK = new Color(120, 120, 120).getRGB();
    private static final int COLOR_NORMAL_LIGHT = new Color(180, 180, 180).getRGB();
    private static final int COLOR_NORMAL_DARK = new Color(80, 80, 80).getRGB();

    private final Module.category category;
    private boolean selected;
    private float hoverAnimation = 0.0f;

    public CategoryComponent(Module.category category, float x, float y, float width, float height, boolean selected) {
        super(x, y, width, height);
        this.category = category;
        this.selected = selected;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        isHovered = isHovered(mouseX, mouseY);
        hoverAnimation = MathHelper.lerp(0.22f, hoverAnimation, isHovered ? 1.0f : 0.0f);

        boolean isLight = ModuleManager.ui.isLightTheme();

        int color;
        if (selected) {
            color = ModuleManager.ui.useCustomColors() ? ModuleManager.ui.accentColor.getRGB()
                    : (isLight ? COLOR_SELECTED_LIGHT : COLOR_SELECTED_DARK);
        } else if (isHovered) {
            color = isLight ? COLOR_HOVERED_LIGHT : COLOR_HOVERED_DARK;
        } else {
            color = isLight ? COLOR_NORMAL_LIGHT : COLOR_NORMAL_DARK;
        }

        context.fill((int) x, (int) y, (int) (x + width), (int) (y + height), color);
        int hoverOverlay = ((int) (MathHelper.clamp(hoverAnimation, 0.0f, 1.0f) * 35.0f) << 24) | 0x00FFFFFF;
        context.fill((int) x, (int) y, (int) (x + width), (int) (y + height), hoverOverlay);
        context.drawText(mc.textRenderer, Text.literal(category.name()), (int) (x + width / 2 - mc.textRenderer.getWidth(category.name()) / 2),
                (int) (y + height / 2 - 4), isLight ? COLOR_LIGHT_TEXT : COLOR_DARK_TEXT, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered(mouseX, mouseY) && button == 0) {
            selected = true;
        }
    }

    public Module.category getCategory() {
        return category;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
