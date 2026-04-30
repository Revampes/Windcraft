package com.revampes.Fault.modules;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.revampes.Fault.modules.impl.client.Cape;
import com.revampes.Fault.modules.impl.client.Commands;
import com.revampes.Fault.modules.impl.client.Title;
import com.revampes.Fault.modules.impl.client.UI;
import com.revampes.Fault.modules.impl.movement.Sprint;
import com.revampes.Fault.modules.impl.render.AntiDebuff;
import com.revampes.Fault.modules.impl.render.ArmorHider;
import com.revampes.Fault.modules.impl.render.ChestESP;
import com.revampes.Fault.modules.impl.render.FreeLook;
import com.revampes.Fault.modules.impl.render.Fullbright;
import com.revampes.Fault.modules.impl.render.HUD;
import com.revampes.Fault.modules.impl.render.HidePlayer;
import com.revampes.Fault.modules.impl.render.NickHider;
import com.revampes.Fault.modules.impl.render.NoBlur;
import com.revampes.Fault.modules.impl.render.NoHudElement;
import com.revampes.Fault.modules.impl.render.NoHurtCam;
import com.revampes.Fault.modules.impl.render.NoOverlay;
import com.revampes.Fault.modules.impl.render.PlayerESP;
import com.revampes.Fault.modules.impl.render.TPS;
import com.revampes.Fault.modules.impl.render.blockanimation.BlockAnimation;
import com.revampes.Fault.modules.impl.wynncraft.SpellCombo;
import com.revampes.Fault.settings.Setting;

public class ModuleManager {
    public static List<Module> modules = new ArrayList<>();
    public static List<Module> organizedModules = new ArrayList<>();

    public static TPS tps;
    public static UI ui;
    public static HUD hud;
    public static Sprint sprint;
    public static AntiDebuff antiDebuff;
    public static Fullbright fullbright;
    public static NoHurtCam noHurtCam;
    public static PlayerESP playerESP;
    public static ArmorHider armorHider;
    public static NickHider nickHider;
    public static FreeLook freeLook;
    public static NoHudElement noHudElement;
    public static NoOverlay noOverlay;
    public static NoBlur noBlur;
    public static Title title;
    public static Commands commands;
    public static ChestESP chestESP;
    public static Cape cape;
    public static BlockAnimation blockAnimation;
    public static HidePlayer hidePlayer;
    public static SpellCombo spellCombo;

    public void register() {
        this.addModule(tps = new TPS());
        this.addModule(ui = new UI());
        this.addModule(hud = new HUD());
        this.addModule(sprint = new Sprint());
        this.addModule(antiDebuff = new AntiDebuff());
        this.addModule(fullbright = new Fullbright());
        this.addModule(noHurtCam = new NoHurtCam());
        this.addModule(playerESP = new PlayerESP());
        this.addModule(armorHider = new ArmorHider());
        this.addModule(nickHider = new NickHider());
        this.addModule(freeLook = new FreeLook());
        this.addModule(noHudElement = new NoHudElement());
        this.addModule(noOverlay = new NoOverlay());
        this.addModule(noBlur = new NoBlur());
        this.addModule(title = new Title());
        this.addModule(commands = new Commands());
        this.addModule(chestESP = new ChestESP());
        this.addModule(cape = new Cape());
        this.addModule(blockAnimation = new BlockAnimation());
        this.addModule(hidePlayer = new HidePlayer());
        this.addModule(spellCombo = new SpellCombo());
        modules.sort(Comparator.comparing(Module::getName));
    }

    public void addModule(Module m) {
        modules.add(m);
    }

    public static List<Module> getModules() {
        return modules;
    }

    public List<Module> inCategory(Module.category categ) {
        ArrayList<Module> categML = new ArrayList<>();

        for (Module mod : getModules()) {
            if (mod.moduleCategory().equals(categ)) {
                categML.add(mod);
            }
        }

        return categML;
    }

    public static Module getModule(String moduleName) {
        for (Module module : modules) {
            if (module.getName().equals(moduleName)) {
                return module;
            }
        }
        return null;
    }

    public Module getModule(Class clazz) {
        for (Module module : modules) {
            if (module.getClass().equals(clazz)) {
                return module;
            }
        }
        return null;
    }

//    public static void sort() {
//        if (HUD.alphabeticalSort.isToggled()) {
//            Collections.sort(organizedModules, Comparator.comparing(Module::getNameInHud));
//        }
//        else {
//            organizedModules.sort((o1, o2) -> mc.fontRendererObj.getStringWidth(o2.getNameInHud() + ((HUD.showInfo.isToggled() && !o2.getInfo().isEmpty()) ? " " + o2.getInfo() : "")) - mc.fontRendererObj.getStringWidth(o1.getNameInHud() + (HUD.showInfo.isToggled() && !o1.getInfo().isEmpty() ? " " + o1.getInfo() : "")));
//        }
//    }

//    public static boolean canExecuteChatCommand() {
//        return ModuleManager.chatCommands != null && ModuleManager.chatCommands.isEnabled();
//    }

    public static List<Module> getModulesByName(String name) {
        List<Module> result = new ArrayList<>();
        for (Module module : modules) {
            if (module.getName().toLowerCase().contains(name.toLowerCase())) {
                result.add(module);
            }
        }
        return result;
    }

    public static List<Module> getModulesByCategory(Module.category selectedCategory) {
        List<Module> result = new ArrayList<>();
        for (Module module : modules) {
            if (module.moduleCategory() == selectedCategory) {
                result.add(module);
            }
        }
        return result;
    }

    public static Module getModuleByName(String name) {
        for (Module module : modules) {
            if (module.getName().equalsIgnoreCase(name)) {
                return module;
            }
        }
        return null;
    }

    public static Module getModuleBySetting(Setting setting) {
        for (Module module : getModules()) {
            if (module.getSettings().contains(setting)) {
                return module;
            }
        }
        return null;
    }
}

