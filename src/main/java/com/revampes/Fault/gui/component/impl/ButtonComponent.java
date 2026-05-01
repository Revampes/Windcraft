package com.revampes.Fault.gui.component.impl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import com.revampes.Fault.gui.component.Component;
import com.revampes.Fault.modules.ModuleManager;
import com.revampes.Fault.settings.impl.ButtonSetting;

import java.awt.*;

import static com.revampes.Fault.Revampes.mc;

public class ButtonComponent extends Component {
    private static final int COLOR_TOGGLE_ON = 0xFF00AA00;
    private static final int COLOR_TOGGLE_OFF = 0xFFAA0000;
    private static final int COLOR_LIGHT_BORDER = 0xFF000000;
    private static final int COLOR_DARK_BORDER = 0xFFFFFFFF;
    private static final int COLOR_BUTTON_TEXT = 0xFFFFFFFF;

    private final ButtonSetting setting;

    public ButtonComponent(ButtonSetting setting, float x, float y, float width, float height) {
        super(x, y, width, height);
        this.setting = setting;
    }

    @Override
    public boolean isHovered(double mouseX, double mouseY) {
        return mouseX >= x + width - 60 && mouseX <= x + width - 10 && mouseY >= y && mouseY <= y + height;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!isVisible()) {
            return;
        }
        boolean isLight = ModuleManager.ui.clickGuiColor.getValue() == 0;
        int toggleX = (int) (x + width - 60);
        int toggleWidth = 50;
        int toggleColor = setting.isToggled() ? COLOR_TOGGLE_ON : COLOR_TOGGLE_OFF;
        isHovered = mouseX >= x + width - 60 && mouseX <= x + width - 10 && mouseY >= y && mouseY <= y + height;

        context.drawText(mc.textRenderer, setting.getName(), (int) (x + 2), (int) (y + height / 2 - 4), isLight ? COLOR_LIGHT_TEXT : COLOR_DARK_TEXT, false);

        int borderColor = isLight ? COLOR_LIGHT_BORDER : COLOR_DARK_BORDER;
        context.fill(toggleX, (int) y, toggleX + toggleWidth, (int) (y + height), borderColor);
        context.fill(toggleX + 1, (int) y + 1, toggleX + toggleWidth - 1, (int) (y + height - 1), toggleColor);

        String toggleText = setting.isToggled() ? "ON" : "OFF";
        int textWidth = mc.textRenderer.getWidth(toggleText);
        context.drawText(mc.textRenderer, toggleText,
                toggleX + (toggleWidth - textWidth) / 2, (int) (y + height / 2 - 4), COLOR_BUTTON_TEXT, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (!isVisible()) {
            return;
        }

        if (isHovered(mouseX, mouseY)) {
            if (setting.isMethodButton) {
                if (button == 0 || button == 1) {
                    setting.runMethod();
                }
            } else {
                setting.toggle();
            }
        }
    }

    @Override
    public boolean isVisible() {
        return setting.isVisible();
    }
}
