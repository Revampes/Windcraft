package com.revampes.Fault.modules.impl.wynncraft;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.lwjgl.glfw.GLFW;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.revampes.Fault.events.impl.PreUpdateEvent;
import com.revampes.Fault.gui.block.BlockNode;
import com.revampes.Fault.gui.block.BlockType;
import com.revampes.Fault.gui.screen.SpellComboScreen;
import com.revampes.Fault.mixin.accessor.KeyBindingAccessor;
import com.revampes.Fault.mixin.accessor.KeyboardAccessor;
import com.revampes.Fault.modules.Module;
import com.revampes.Fault.settings.impl.ButtonSetting;
import com.revampes.Fault.settings.impl.InputSetting;
import com.revampes.Fault.utility.BindUtils;
import com.revampes.Fault.utility.Input;
import com.revampes.Fault.utility.Utils;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public class SpellCombo extends Module {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type COMBO_LIST_TYPE = new TypeToken<List<ComboProfile>>() {
    }.getType();
    private static final long SPELL_KEY_HOLD_MS = 45L;
    private static final Method ON_KEY_PRESSED_METHOD = findKeyBindingMethod("onKeyPressed", InputUtil.Key.class);
    private static final Method SET_KEY_PRESSED_METHOD = findKeyBindingMethod("setKeyPressed", InputUtil.Key.class, boolean.class);

//    private final InputSetting cast1SpellKey = new InputSetting("Cast 1st Spell", 32, "Z", "Wynntils quick-cast key");
//    private final InputSetting cast2SpellKey = new InputSetting("Cast 2nd Spell", 32, "X", "Wynntils quick-cast key");
//    private final InputSetting cast3SpellKey = new InputSetting("Cast 3rd Spell", 32, "C", "Wynntils quick-cast key");
//    private final InputSetting cast4SpellKey = new InputSetting("Cast 4th Spell", 32, "V", "Wynntils quick-cast key");
    private final ButtonSetting configureButton = new ButtonSetting("Configure", this::openEditor);
    private final InputSetting comboStorage = new InputSetting("Combo Storage", 200000, "[]");

    private static int Spell1Key;
    private static int Spell2Key;
    private static int Spell3Key;
    private static int Spell4Key;

    private final List<ComboProfile> combos = new ArrayList<>();
    private final ArrayDeque<RuntimeAction> queuedActions = new ArrayDeque<>();
    private final ArrayDeque<PendingKeyRelease> pendingReleases = new ArrayDeque<>();

    private long nextActionAtMs;
    private boolean combosInitialized = false;

    public SpellCombo() {
        super("SpellCombo", "Queue Wynntils quick-cast spell chains onto one key.", category.Wynncraft);
        comboStorage.visible = false;
        this.registerSetting(configureButton);
        this.registerSetting(comboStorage);
    }
    private void refreshSpellKeys() {
        Spell1Key = GLFW.GLFW_KEY_UNKNOWN;
        Spell2Key = GLFW.GLFW_KEY_UNKNOWN;
        Spell3Key = GLFW.GLFW_KEY_UNKNOWN;
        Spell4Key = GLFW.GLFW_KEY_UNKNOWN;

        if (mc.options == null || mc.options.allKeys == null) return;

        for (KeyBinding keyBinding : mc.options.allKeys) {
            if (keyBinding == null) {
                continue;
            }

            String id = keyBinding.getId();
            if (id != null && id.startsWith("wynntils.keybind.cast") && id.endsWith("Spell")) {
                InputUtil.Key bound = ((KeyBindingAccessor) keyBinding).getKey();
                int code = bound.getCode();
                
                if (bound.getCategory() == InputUtil.Type.MOUSE) {
                    code = BindUtils.toMouseBind(code);
                }

                switch (spellIndexFromId(id)) {
                    case 1 -> Spell1Key = code;
                    case 2 -> Spell2Key = code;
                    case 3 -> Spell3Key = code;
                    case 4 -> Spell4Key = code;
                }
            }
        }
    }

    private int spellIndexFromId(String id) {
        return switch (id) {
            case "wynntils.keybind.castFirstSpell" -> 1;
            case "wynntils.keybind.castSecondSpell" -> 2;
            case "wynntils.keybind.castThirdSpell" -> 3;
            case "wynntils.keybind.castFourthSpell" -> 4;
            default -> 0;
        };
    }

    @Override
    public String getDesc() {
        return "Use Configure to build combo scripts with block-style actions, then bind each combo trigger with the key box.";
    }

    @Override
    public void onEnable() {
        clearQueue();
        forceReleaseAllPending();
        resetTriggerHoldStates();
        refreshSpellKeys();
        combosInitialized = false;
    }

    @Override
    public void onDisable() {
        clearQueue();
        forceReleaseAllPending();
        resetTriggerHoldStates();
        combosInitialized = false;
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        long now = System.currentTimeMillis();
        processPendingReleases(now);

        if (!Utils.nullCheck() || mc.currentScreen != null) {
            clearQueue();
            forceReleaseAllPending();
            resetTriggerHoldStates();
            return;
        }

        pollTriggerCombos();

        if (queuedActions.isEmpty() || now < nextActionAtMs) {
            return;
        }

        RuntimeAction action = queuedActions.pollFirst();
        if (action == null) {
            return;
        }

        if (!executeAction(action)) {
            clearQueue();
            return;
        }

        nextActionAtMs = queuedActions.isEmpty()
                ? 0L
                : now + (action.type == BlockType.WAIT ? action.value : SPELL_KEY_HOLD_MS);
    }

    public void openEditor() {
        if (!combosInitialized) {
            loadCombosFromStorage();
            combosInitialized = true;
        }
        refreshSpellKeys();
        mc.setScreen(new SpellComboScreen(this));
    }

    @Override
    public void onUpdate() {
        if (!combosInitialized) {
            loadCombosFromStorage();
            combosInitialized = true;
        }
        refreshSpellKeys();
    }

    public void debugPrintKeybindings() {
        if (mc.options == null || mc.options.allKeys == null) {
            Utils.addChatMessage("§cNo keybindings available.");
            return;
        }

        Utils.addChatMessage("§eSearching for Wynntils keybindings...");
        for (KeyBinding keyBinding : mc.options.allKeys) {
            try {
                String category = keyBinding.getId();
                String displayName = keyBinding.getBoundKeyLocalizedText().getString();

                // Try to extract the keybinding ID from the object using reflection or other methods
                System.out.println("[KEYBIND] Category: " + category + " | Bound: " + displayName + " | Object: " + keyBinding.getClass().getName());

                // Print any keybindings that contain "cast" or "spell" or "wynntils"
                String catLower = category.toLowerCase();
                if (catLower.contains("cast") || catLower.contains("spell") || catLower.contains("wynntils") || catLower.contains("quick")) {
                    Utils.addChatMessage("§a[SPELL] Category: " + category + " -> " + displayName);
                }
            } catch (Exception e) {
                // Silently skip
            }
        }
    }

    public List<ComboProfile> getCombos() {
        return combos;
    }

    public void addCombo() {
        combos.add(createCombo("Combo " + (combos.size() + 1)));
        renumberCombos();
        saveCombosToStorage();
    }

    public void removeCombo(int index) {
        if (index < 0 || index >= combos.size()) {
            return;
        }

        combos.remove(index);
        if (combos.isEmpty()) {
            combos.add(createCombo("Combo 1"));
        }
        renumberCombos();
        saveCombosToStorage();
    }

    public void loadCombosFromStorage() {
        try {
            List<ComboProfile> loaded = GSON.fromJson(comboStorage.getValue(), COMBO_LIST_TYPE);
            combos.clear();
            if (loaded != null) {
                for (ComboProfile combo : loaded) {
                    if (combo == null) {
                        continue;
                    }
                    if (combo.name == null || combo.name.isBlank()) {
                        combo.name = "Combo " + (combos.size() + 1);
                    }
                    if (combo.blocks == null) {
                        combo.blocks = new ArrayList<>();
                    }
                    combo.triggerHeld = false;
                    combos.add(combo);
                }
            }
        } catch (Exception e) {
            combos.clear();
        }

        if (combos.isEmpty()) {
            combos.add(createCombo("Combo 1"));
        }

        renumberCombos();
        saveCombosToStorage();
    }

    public void saveCombosToStorage() {
        comboStorage.setValue(GSON.toJson(combos));
    }

    private ComboProfile createCombo(String name) {
        ComboProfile combo = new ComboProfile();
        combo.name = name;
        combo.triggerCode = 0;
        combo.blocks = new ArrayList<>();
        combo.triggerHeld = false;
        return combo;
    }

    private void renumberCombos() {
        for (int i = 0; i < combos.size(); i++) {
            ComboProfile combo = combos.get(i);
            combo.name = "Combo " + (i + 1);
        }
    }

    private void pollTriggerCombos() {
        for (ComboProfile combo : combos) {
            boolean isDown = isTriggerInputDown(combo.triggerCode);
            boolean wasDown = combo.triggerHeld;
            combo.triggerHeld = isDown;

            if (isDown && !wasDown) {
                queueCombo(combo);
            }
        }
    }

    private boolean isTriggerInputDown(int bindCode) {
        if (bindCode == 0) {
            return false;
        }

        if (BindUtils.isMouseBind(bindCode)) {
            return isMouseButtonDown(BindUtils.toMouseButton(bindCode));
        }

        return isPhysicalKeyDown(bindCode);
    }

    private boolean isPhysicalKeyDown(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_UNKNOWN || mc.getWindow() == null) {
            return false;
        }

        long window = mc.getWindow().getHandle();
        if (window == 0L) {
            return false;
        }

        return GLFW.glfwGetKey(window, keyCode) == GLFW.GLFW_PRESS;
    }

    private boolean isMouseButtonDown(int mouseButton) {
        if (mc.getWindow() == null) {
            return false;
        }

        long window = mc.getWindow().getHandle();
        if (window == 0L) {
            return false;
        }

        return GLFW.glfwGetMouseButton(window, mouseButton) == GLFW.GLFW_PRESS;
    }

    private void queueCombo(ComboProfile combo) {
        if (isBusy()) {
            Utils.addChatMessage("§eSpellCombo is already running. Wait for the current combo to finish.");
            return;
        }

        List<RuntimeAction> actions = new ArrayList<>();
        appendBlocks(combo.blocks, actions);
        if (actions.isEmpty()) {
            Utils.addChatMessage("§c" + combo.name + " has no blocks yet.");
            return;
        }

        queuedActions.addAll(actions);
        nextActionAtMs = 0L;
        Utils.addChatMessage("§a" + combo.name + " queued with §f" + actions.size() + "§a block action(s).");
    }

    private void appendBlocks(List<BlockNode> blocks, List<RuntimeAction> actions) {
        for (BlockNode block : blocks) {
            if (block == null || block.type == null) {
                continue;
            }

            switch (block.type) {
                case REPEAT -> {
                    int repeatCount = Math.max(1, block.value);
                    for (int i = 0; i < repeatCount; i++) {
                        appendBlocks(block.children, actions);
                    }
                }
                case CAST_SPELL, WAIT, LEFT_CLICK, RIGHT_CLICK -> actions.add(new RuntimeAction(block.type, block.value));
                case WHEN_TRIGGER -> {
                }
            }
        }
    }

    private boolean executeAction(RuntimeAction action) {
        return switch (action.type) {
            case CAST_SPELL -> executeSpellAction(action.value);
            case WAIT -> action.value >= 0;
            case LEFT_CLICK -> pressAndQueueRelease(BindUtils.toMouseBind(GLFW.GLFW_MOUSE_BUTTON_LEFT));
            case RIGHT_CLICK -> pressAndQueueRelease(BindUtils.toMouseBind(GLFW.GLFW_MOUSE_BUTTON_RIGHT));
            default -> true;
        };
    }

    private boolean executeSpellAction(int spellIndex) {
        int bindCode = switch (spellIndex) {
            case 1 -> Spell1Key;
            case 2 -> Spell2Key;
            case 3 -> Spell3Key;
            case 4 -> Spell4Key;
            default -> GLFW.GLFW_KEY_UNKNOWN;
        };

        if (bindCode == GLFW.GLFW_KEY_UNKNOWN) {
            Utils.addChatMessage("§cCast " + spellIndex + " Spell key is invalid. Set it to your Wynntils quick-cast key first.");
            return false;
        }

        return pressAndQueueRelease(bindCode);
    }

    private boolean pressAndQueueRelease(int bindCode) {
        if (bindCode == 0 || bindCode == GLFW.GLFW_KEY_UNKNOWN) return false;

        boolean isMouse = BindUtils.isMouseBind(bindCode);
        int realCode = isMouse ? BindUtils.toMouseButton(bindCode) : bindCode;

        // 1. Raw Accessor Event Injection (Simulates hardware input)
        if (isMouse) {
            sendMouseEvent(realCode, GLFW.GLFW_PRESS);
        } else {
            sendKeyboardEvent(realCode, GLFW.GLFW_PRESS);
        }

        // 2. Vanilla KeyBinding manipulation (Sync logic)
        InputUtil.Key inputKey = isMouse 
            ? InputUtil.Type.MOUSE.createFromCode(realCode) 
            : InputUtil.Type.KEYSYM.createFromCode(realCode);
        
        List<KeyBinding> touchedBindings = new ArrayList<>();
        if (mc.options != null && mc.options.allKeys != null) {
            for (KeyBinding kb : mc.options.allKeys) {
                 if (((KeyBindingAccessor) kb).getKey().equals(inputKey)) {
                     kb.setPressed(true);
                     touchedBindings.add(kb);
                 }
            }
        }
        
        // Ensure static mappings are ticked
        if (ON_KEY_PRESSED_METHOD != null) {
            invokeStaticKeyBindingMethod(ON_KEY_PRESSED_METHOD, inputKey);
        }
        if (SET_KEY_PRESSED_METHOD != null) {
            invokeStaticKeyBindingMethod(SET_KEY_PRESSED_METHOD, inputKey, true);
        }

        pendingReleases.addLast(new PendingKeyRelease(
            bindCode,
            inputKey,
            touchedBindings,
            System.currentTimeMillis() + SPELL_KEY_HOLD_MS
        ));

        return true;
    }

    private void processPendingReleases(long now) {
        while (!pendingReleases.isEmpty()) {
            PendingKeyRelease pending = pendingReleases.peekFirst();
            if (pending == null || now < pending.releaseAtMs) {
                return;
            }
            releasePending(pending);
            pendingReleases.pollFirst();
        }
    }

    private void forceReleaseAllPending() {
        while (!pendingReleases.isEmpty()) {
            PendingKeyRelease pending = pendingReleases.pollFirst();
            if (pending != null) {
                releasePending(pending);
            }
        }
    }

    private void releasePending(PendingKeyRelease pending) {
        boolean isMouse = BindUtils.isMouseBind(pending.bindCode);
        int realCode = isMouse ? BindUtils.toMouseButton(pending.bindCode) : pending.bindCode;

        // 1. Hardware Release Injection
        if (isMouse) {
            sendMouseEvent(realCode, GLFW.GLFW_RELEASE); 
        } else {
            sendKeyboardEvent(realCode, GLFW.GLFW_RELEASE);
        }

        // 2. Vanilla Synchronization Release
        if (SET_KEY_PRESSED_METHOD != null) {
            invokeStaticKeyBindingMethod(SET_KEY_PRESSED_METHOD, pending.inputKey, false);
        }

        for (KeyBinding keyBinding : pending.bindings) {
            keyBinding.setPressed(false);
        }
    }

    private boolean sendKeyboardEvent(int keyCode, int action) {
        if (keyCode == GLFW.GLFW_KEY_UNKNOWN || mc.keyboard == null || mc.getWindow() == null) {
            return false;
        }

        long window = mc.getWindow().getHandle();
        if (window == 0L) {
            return false;
        }

        int scancode = GLFW.glfwGetKeyScancode(keyCode);
        KeyInput input = new KeyInput(keyCode, scancode, 0);

        try {
            KeyboardAccessor keyboard = (KeyboardAccessor) mc.keyboard;
            keyboard.revampes$invokeOnKey(window, action, input);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean invokeStaticKeyBindingMethod(Method method, Object... args) {
        try {
            method.invoke(null, args);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static Method findKeyBindingMethod(String name, Class<?>... parameterTypes) {
        try {
            Method method = KeyBinding.class.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (Exception ignored) {
            return null;
        }
    }



    private boolean isBusy() {
        return !queuedActions.isEmpty() || nextActionAtMs > System.currentTimeMillis();
    }

    private boolean sendMouseEvent(int buttonCode, int action) {
        if (mc.mouse == null || mc.getWindow() == null) return false;
        
        long window = mc.getWindow().getHandle();

        try {
            // Use the Accessor to call the private method
            // Most Fabric/Wynncraft environments use (window, button, action, mods)
            ((com.revampes.Fault.mixin.accessor.MouseAccessor) mc.mouse)
                .revampes$invokeOnMouseButton(window, buttonCode, action, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void clearQueue() {
        queuedActions.clear();
        nextActionAtMs = 0L;
    }

    private void resetTriggerHoldStates() {
        for (ComboProfile combo : combos) {
            combo.triggerHeld = false;
        }
    }

    public static class ComboProfile {
        public String name;
        public int triggerCode;
        public List<BlockNode> blocks = new ArrayList<>();
        public transient boolean triggerHeld;
    }

    private static class PendingKeyRelease {
        private final int bindCode;
        private final InputUtil.Key inputKey;
        private final List<KeyBinding> bindings;
        private final long releaseAtMs;

        private PendingKeyRelease(int bindCode, InputUtil.Key inputKey, List<KeyBinding> bindings, long releaseAtMs) {
            this.bindCode = bindCode;
            this.inputKey = inputKey;
            this.bindings = bindings;
            this.releaseAtMs = releaseAtMs;
        }
    }

    private static class RuntimeAction {
        private final BlockType type;
        private final int value;

        private RuntimeAction(BlockType type, int value) {
            this.type = type;
            this.value = value;
        }
    }


}
