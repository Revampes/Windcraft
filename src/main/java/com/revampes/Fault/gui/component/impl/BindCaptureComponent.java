package com.revampes.Fault.gui.component.impl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import com.revampes.Fault.gui.component.Component;
import com.revampes.Fault.modules.ModuleManager;
import com.revampes.Fault.utility.BindUtils;

import java.awt.*;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import static com.revampes.Fault.Revampes.mc;

public class BindCaptureComponent extends Component {
    private static final int COLOR_BOX_LISTENING = new Color(140, 180, 255).getRGB();
    private static final int COLOR_BOX_HOVERED = new Color(190, 190, 190).getRGB();
    private static final int COLOR_BOX_NORMAL = new Color(170, 170, 170).getRGB();

    private final String label;
    private final IntSupplier getter;
    private final IntConsumer setter;
    private boolean listening;

    public BindCaptureComponent(String label, IntSupplier getter, IntConsumer setter, float x, float y, float width, float height) {
        super(x, y, width, height);
        this.label = label;
        this.getter = getter;
        this.setter = setter;
        this.listening = false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean isLight = ModuleManager.ui.isLightTheme();
        isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        context.drawText(mc.textRenderer, label, (int) (x + 2), (int) (y + height / 2 - 4), isLight ? COLOR_LIGHT_TEXT : COLOR_DARK_TEXT, false);

        int boxW = Math.min(110, Math.max(78, (int) width - 42));
        int boxX = (int) (x + width - boxW);
        int boxColor = listening ? COLOR_BOX_LISTENING : (isHovered ? COLOR_BOX_HOVERED : COLOR_BOX_NORMAL);
        context.fill(boxX, (int) y, boxX + boxW, (int) (y + height), boxColor);

        String text = listening ? "Listening..." : BindUtils.formatBind(getter.getAsInt());
        context.drawText(mc.textRenderer, Text.literal(text), boxX + 5, (int) (y + height / 2 - 4), isLight ? COLOR_LIGHT_TEXT : COLOR_DARK_TEXT, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!listening) {
            return false;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            setter.accept(0);
            listening = false;
            return true;
        }

        setter.accept(keyCode);
        listening = false;
        return true;
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (listening) {
            setter.accept(BindUtils.toMouseBind(button));
            listening = false;
            return;
        }

        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height && button == 0) {
            listening = true;
        }
    }

    public boolean isListening() {
        return listening;
    }
}