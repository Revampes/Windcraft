package com.revampes.Fault.utility;

import net.minecraft.client.util.InputUtil;

import java.util.Locale;

public final class BindUtils {
    public static final int MOUSE_BIND_OFFSET = 1000;

    private BindUtils() {
    }

    public static boolean isMouseBind(int code) {
        return code >= MOUSE_BIND_OFFSET;
    }

    public static int toMouseBind(int button) {
        return MOUSE_BIND_OFFSET + button;
    }

    public static int toMouseButton(int code) {
        return code - MOUSE_BIND_OFFSET;
    }

    public static String formatBind(int code) {
        if (code == 0) {
            return "None";
        }

        if (isMouseBind(code)) {
            return switch (toMouseButton(code)) {
                case 0 -> "Left Click";
                case 1 -> "Right Click";
                case 2 -> "Middle Click";
                case 3 -> "Button 4";
                case 4 -> "Button 5";
                default -> "Mouse " + toMouseButton(code);
            };
        }

        if (code < 0) {
            return "Invalid";
        }

        try {
            InputUtil.Key key = InputUtil.Type.KEYSYM.createFromCode(code);
            String text = key.getLocalizedText().getString();
            return text == null || text.isBlank() ? "Invalid" : text;
        } catch (Exception ignored) {
            return "Invalid";
        }
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.toUpperCase(Locale.ROOT)
                .replace(" ", "")
                .replace("_", "")
                .replace("-", "")
                .trim();
    }
}