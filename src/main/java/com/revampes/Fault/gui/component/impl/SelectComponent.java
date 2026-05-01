package com.revampes.Fault.gui.component.impl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import com.revampes.Fault.gui.component.Component;
import com.revampes.Fault.modules.ModuleManager;
import com.revampes.Fault.settings.impl.SelectSetting;

import java.awt.*;

import static com.revampes.Fault.Revampes.mc;

public class SelectComponent extends Component {
    private static final int COLOR_DROPDOWN_BACKGROUND = 0xFF000000;
    private static final int COLOR_OPTION_HOVERED = 0xFF4444AA;
    private static final int COLOR_OPTION_NORMAL = 0xFF333333;
    private static final int COLOR_OPTION_TEXT = 0xFFFFFFFF;

    private final SelectSetting setting;
    private boolean expanded;
    private boolean clickConsumed;

    public SelectComponent(SelectSetting setting, float x, float y, float width, float height) {
        super(x, y, width, height);
        this.setting = setting;
        this.expanded = false;
        this.clickConsumed = false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!isVisible()) return;
        boolean isLight = ModuleManager.ui.clickGuiColor.getValue() == 0;
        isHovered = mouseX >= x + width - 100 && mouseX <= x + width - 10 && mouseY >= y && mouseY <= y + height;

        context.drawText(mc.textRenderer, setting.getName(), (int) (x + 2), (int) (y + height / 2 - 4), isLight ? COLOR_LIGHT_TEXT : COLOR_DARK_TEXT, false);

        int boxColor = isHovered ? COLOR_BOX_HOVERED : COLOR_BOX_NORMAL;
        context.fill((int) (x + width - 100), (int) y, (int) (x + width - 10), (int) (y + height), boxColor);

        String currentOption = setting.getOptions()[(int) setting.getValue()];
        context.drawText(mc.textRenderer, Text.literal(currentOption), (int) (x + width - 95), (int) (y + height / 2 - 4), isLight ? COLOR_LIGHT_TEXT : COLOR_DARK_TEXT, false);

        context.drawText(mc.textRenderer, Text.literal(expanded ? "▲" : "▼"), (int) (x + width - 20), (int) (y + height / 2 - 4), isLight ? COLOR_LIGHT_TEXT : COLOR_DARK_TEXT, false);

        if (expanded) {
            context.fill((int) (x + width - 100), (int) (y + height),
                    (int) (x + width - 10), (int) (y + height + setting.getOptions().length * height),
                    COLOR_DROPDOWN_BACKGROUND);

            for (int i = 0; i < setting.getOptions().length; i++) {
                int optionY = (int) (y + height * (i + 1));
                boolean optionHovered = mouseX >= x + width - 100 && mouseX <= x + width - 10 &&
                        mouseY >= optionY && mouseY < optionY + height;

                context.fill((int) (x + width - 100), optionY,
                        (int) (x + width - 10), optionY + (int) height, // -1
                        optionHovered ? COLOR_OPTION_HOVERED : COLOR_OPTION_NORMAL);

                context.drawText(mc.textRenderer, Text.literal(setting.getOptions()[i]),
                        (int) (x + width - 95), optionY + (int) (height / 2 - 4),
                        COLOR_OPTION_TEXT, false);
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (!isVisible() || button != 0) return;

        clickConsumed = false;

        if (mouseX >= x + width - 100 && mouseX <= x + width - 10 && mouseY >= y && mouseY <= y + height) {
            expanded = !expanded;
            clickConsumed = true;
            return;
        }

        if (expanded) {
            for (int i = 0; i < setting.getOptions().length; i++) {
                int optionY = (int) (y + height * (i + 1));
                if (mouseX >= x + width - 100 && mouseX <= x + width - 10 &&
                        mouseY >= optionY && mouseY < optionY + height) {
                    setting.setValue(i);
                    expanded = false;
                    clickConsumed = true;
                    return;
                }
            }

            float selectBottom = y + height + setting.getOptions().length * height;
            if (mouseX >= x + width - 100 && mouseX <= x + width - 10 &&
                    mouseY >= y + height && mouseY <= selectBottom) {
                expanded = false;
                clickConsumed = true;
                return;
            }
        }
    }

    public boolean isClickConsumed() {
        return clickConsumed;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public int getOptionsLength() {
        return setting != null ? setting.getOptions().length : 0;
    }

    @Override
    public boolean isVisible() {
        return setting.isVisible();
    }
}
