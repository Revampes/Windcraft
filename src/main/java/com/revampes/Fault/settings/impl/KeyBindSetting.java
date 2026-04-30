package com.revampes.Fault.settings.impl;

import net.minecraft.client.util.InputUtil;
import com.revampes.Fault.modules.Module;
import com.revampes.Fault.utility.BindUtils;

public class KeyBindSetting extends ButtonSetting {
    private final Module module;

    public KeyBindSetting(Module module) {
        super("KeyBind", false);
        this.module = module;
    }

    public String getKeyText() {
        return BindUtils.formatBind(module.getKeycode());
    }

    public Module getModule() {
        return module;
    }
}
