package com.revampes.Fault.modules.impl.wynncraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import meteordevelopment.orbit.EventHandler;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import com.revampes.Fault.events.impl.PreUpdateEvent;
import com.revampes.Fault.gui.block.BlockNode;
import com.revampes.Fault.gui.block.BlockType;
import com.revampes.Fault.gui.screen.SpellComboScreen;
import com.revampes.Fault.mixin.accessor.KeyBindingAccessor;
import com.revampes.Fault.mixin.accessor.KeyboardAccessor;
import com.revampes.Fault.mixin.accessor.MouseAccessor;
import com.revampes.Fault.modules.Module;
import com.revampes.Fault.settings.impl.ButtonSetting;
import com.revampes.Fault.settings.impl.InputSetting;
import com.revampes.Fault.utility.BindUtils;
import com.revampes.Fault.utility.Utils;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.revampes.Fault.Revampes.mc;

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

    public SpellCombo() {
        super("SpellCombo", "Queue Wynntils quick-cast spell chains onto one key.", category.Wynncraft);
        comboStorage.visible = false;
        this.registerSetting(configureButton);
        this.registerSetting(comboStorage);
    }
    private void setKey()
    {
        for (KeyBinding keyBinding : mc.options.allKeys) {
            String id = keyBinding.getId();
            switch (id) {
                case "wynntils.keybind.castFirstSpell" ->
                        Spell1Key = parseKeyCode(keyBinding.getBoundKeyLocalizedText().getString());
                case "wynntils.keybind.castSecondSpell" ->
                        Spell2Key = parseKeyCode(keyBinding.getBoundKeyLocalizedText().getString());
                case "wynntils.keybind.castThirdSpell" ->
                        Spell3Key = parseKeyCode(keyBinding.getBoundKeyLocalizedText().getString());
                case "wynntils.keybind.castFourthSpell" ->
                        Spell4Key = parseKeyCode(keyBinding.getBoundKeyLocalizedText().getString());
            }

        }
        System.out.println("Spell1: "  + Spell1Key);
        System.out.println("Spell2: "  + Spell2Key);
        System.out.println("Spell3: "  + Spell3Key);
        System.out.println("Spell4: "  + Spell4Key);
    }

    @Override
    public String getDesc() {
        return "Use Configure to build combo scripts with block-style actions, then bind each combo trigger with the key box.";
    }

    @Override
    public void onEnable() {
        loadCombosFromStorage();
        clearQueue();
        forceReleaseAllPending();
        resetTriggerHoldStates();
        setKey();

    }

    @Override
    public void onDisable() {
        clearQueue();
        forceReleaseAllPending();
        resetTriggerHoldStates();
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
        loadCombosFromStorage();
        mc.setScreen(new SpellComboScreen(this));
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
            case LEFT_CLICK -> executeMouseAction(GLFW.GLFW_MOUSE_BUTTON_LEFT);
            case RIGHT_CLICK -> executeMouseAction(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
            default -> true;
        };
    }

    private boolean executeMouseAction(int mouseButton) {
        InputUtil.Key key = InputUtil.Type.MOUSE.createFromCode(mouseButton);
        return pressAndQueueRelease(key, GLFW.GLFW_KEY_UNKNOWN);
    }

    private boolean executeSpellAction(int spellIndex) {
        int keyCode = switch (spellIndex) {
            case 1 -> Spell1Key;
            case 2 -> Spell2Key;
            case 3 -> Spell3Key;
            case 4 -> Spell4Key;
            default -> GLFW.GLFW_KEY_UNKNOWN;
        };


        if (keyCode == GLFW.GLFW_KEY_UNKNOWN) {
            Utils.addChatMessage("§cCast " + spellIndex + " Spell key is invalid. Set it to your Wynntils quick-cast key first.");
            return false;
        }
        return pressAndQueueRelease(InputUtil.Type.KEYSYM.createFromCode(keyCode), keyCode);
    }

    private boolean pressAndQueueRelease(InputUtil.Key key, int keyboardKeyCode) {
        if (key == null) {
            return false;
        }
        
        boolean sentAny = false;
        boolean staticPressed = false;

        if (SET_KEY_PRESSED_METHOD != null && invokeStaticKeyBindingMethod(SET_KEY_PRESSED_METHOD, key, true)) {
            sentAny = true;
            staticPressed = true;
        }

        if (ON_KEY_PRESSED_METHOD != null && invokeStaticKeyBindingMethod(ON_KEY_PRESSED_METHOD, key)) {
            sentAny = true;
        }

        List<KeyBinding> touchedBindings = collectMatchingBindings(key);
        if (!touchedBindings.isEmpty()) {
            sentAny = true;
        }

        boolean keyboardPressed = false;
        if (keyboardKeyCode != GLFW.GLFW_KEY_UNKNOWN) {
            keyboardPressed = sendKeyboardEvent(keyboardKeyCode, GLFW.GLFW_PRESS);
            if (keyboardPressed) {
                sentAny = true;
            }
        }

        if (!sentAny) {
            if (staticPressed && SET_KEY_PRESSED_METHOD != null) {
                invokeStaticKeyBindingMethod(SET_KEY_PRESSED_METHOD, key, false);
            }
            for (KeyBinding binding : touchedBindings) {
                binding.setPressed(false);
            }
            return false;
        }

        queuePendingRelease(key, touchedBindings, keyboardKeyCode, staticPressed, keyboardPressed);
        return true;
    }

    private List<KeyBinding> collectMatchingBindings(InputUtil.Key key) {
        List<KeyBinding> touched = new ArrayList<>();
        if (mc.options == null || mc.options.allKeys == null) {
            return touched;
        }

        for (KeyBinding keyBinding : mc.options.allKeys) {
            InputUtil.Key bound = ((KeyBindingAccessor) keyBinding).getKey();
            if (bound == null || !bound.equals(key)) {
                continue;
            }
            keyBinding.setPressed(true);
            touched.add(keyBinding);
        }

        return touched;
    }

    private void queuePendingRelease(InputUtil.Key key, List<KeyBinding> bindings, int keyboardKeyCode,
                                     boolean releaseStatic, boolean releaseKeyboard) {
        pendingReleases.addLast(new PendingKeyRelease(
                key,
                bindings,
                keyboardKeyCode,
                releaseStatic,
                releaseKeyboard,
                System.currentTimeMillis() + SPELL_KEY_HOLD_MS
        ));
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
        if (pending.releaseKeyboardEvent && pending.keyboardKeyCode != GLFW.GLFW_KEY_UNKNOWN) {
            sendKeyboardEvent(pending.keyboardKeyCode, GLFW.GLFW_RELEASE);
        }

        if (pending.releaseStaticState && SET_KEY_PRESSED_METHOD != null) {
            invokeStaticKeyBindingMethod(SET_KEY_PRESSED_METHOD, pending.key, false);
        }

        for (KeyBinding keyBinding : pending.bindings) {
            keyBinding.setPressed(false);
        }
    }

    private boolean sendMouseEvent(int mouseButton, int action) {
        if (mc.mouse == null || mc.getWindow() == null) {
            return false;
        }

        long window = mc.getWindow().getHandle();
        if (window == 0L) {
            return false;
        }

        try {
            MouseInput input = new MouseInput(mouseButton, 0);
            MouseAccessor mouse = (MouseAccessor) mc.mouse;
            mouse.revampes$invokeOnMouseButton(window, input, action);
            return true;
        } catch (Exception ignored) {
            return false;
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

    private int parseKeyCode(String value) {
        System.out.println("Parsing key code from: " + value);
        if (value.contains("Button"))
        {
            return Integer.parseInt(value.replace("Button ",""));
        }
        String normalized = BindUtils.normalize(value);
        if (normalized.isEmpty() || normalized.equals("NONE") || normalized.equals("UNBOUND")) {
            return GLFW.GLFW_KEY_UNKNOWN;
        }

        if (normalized.length() == 1) {
            char c = normalized.charAt(0);
            if (c >= 'A' && c <= 'Z') {
                return GLFW.GLFW_KEY_A + (c - 'A');
            }
            if (c >= '0' && c <= '9') {
                return GLFW.GLFW_KEY_0 + (c - '0');
            }
        }

        if (normalized.matches("-?\\d+")) {
            try {
                return Integer.parseInt(normalized);
            } catch (NumberFormatException ignored) {
                return GLFW.GLFW_KEY_UNKNOWN;
            }
        }

        if (normalized.startsWith("F") && normalized.length() <= 3 && normalized.length() > 1) {
            try {
                int fKey = Integer.parseInt(normalized.substring(1));
                if (fKey >= 1 && fKey <= 25) {
                    return GLFW.GLFW_KEY_F1 + (fKey - 1);
                }
            } catch (NumberFormatException ignored) {
                return GLFW.GLFW_KEY_UNKNOWN;
            }
        }

        return switch (normalized) {
            case "SPACE", "SPACEBAR" -> GLFW.GLFW_KEY_SPACE;
            case "TAB" -> GLFW.GLFW_KEY_TAB;
            case "ENTER", "RETURN" -> GLFW.GLFW_KEY_ENTER;
            case "ESC", "ESCAPE" -> GLFW.GLFW_KEY_ESCAPE;
            case "BACKSPACE" -> GLFW.GLFW_KEY_BACKSPACE;
            case "DELETE", "DEL" -> GLFW.GLFW_KEY_DELETE;
            case "INSERT", "INS" -> GLFW.GLFW_KEY_INSERT;
            case "HOME" -> GLFW.GLFW_KEY_HOME;
            case "END" -> GLFW.GLFW_KEY_END;
            case "PAGEUP", "PGUP" -> GLFW.GLFW_KEY_PAGE_UP;
            case "PAGEDOWN", "PGDN" -> GLFW.GLFW_KEY_PAGE_DOWN;
            case "UP", "UPARROW" -> GLFW.GLFW_KEY_UP;
            case "DOWN", "DOWNARROW" -> GLFW.GLFW_KEY_DOWN;
            case "LEFT", "LEFTARROW" -> GLFW.GLFW_KEY_LEFT;
            case "RIGHT", "RIGHTARROW" -> GLFW.GLFW_KEY_RIGHT;
            case "SHIFT", "LEFTSHIFT", "LSHIFT" -> GLFW.GLFW_KEY_LEFT_SHIFT;
            case "RIGHTSHIFT", "RSHIFT" -> GLFW.GLFW_KEY_RIGHT_SHIFT;
            case "CTRL", "CONTROL", "LEFTCONTROL", "LEFTCTRL", "LCTRL" -> GLFW.GLFW_KEY_LEFT_CONTROL;
            case "RIGHTCONTROL", "RIGHTCTRL", "RCTRL" -> GLFW.GLFW_KEY_RIGHT_CONTROL;
            case "ALT", "LEFTALT", "LALT" -> GLFW.GLFW_KEY_LEFT_ALT;
            case "RIGHTALT", "RALT" -> GLFW.GLFW_KEY_RIGHT_ALT;
            case "CAPS", "CAPSLOCK" -> GLFW.GLFW_KEY_CAPS_LOCK;
            case "COMMA" -> GLFW.GLFW_KEY_COMMA;
            case "PERIOD", "DOT" -> GLFW.GLFW_KEY_PERIOD;
            case "MINUS", "DASH" -> GLFW.GLFW_KEY_MINUS;
            case "EQUAL", "EQUALS" -> GLFW.GLFW_KEY_EQUAL;
            case "SEMICOLON" -> GLFW.GLFW_KEY_SEMICOLON;
            case "APOSTROPHE", "QUOTE" -> GLFW.GLFW_KEY_APOSTROPHE;
            case "SLASH", "FORWARDSLASH" -> GLFW.GLFW_KEY_SLASH;
            case "BACKSLASH" -> GLFW.GLFW_KEY_BACKSLASH;
            case "LEFTBRACKET", "LBRACKET" -> GLFW.GLFW_KEY_LEFT_BRACKET;
            case "RIGHTBRACKET", "RBRACKET" -> GLFW.GLFW_KEY_RIGHT_BRACKET;
            case "GRAVE", "TILDE" -> GLFW.GLFW_KEY_GRAVE_ACCENT;
            default -> GLFW.GLFW_KEY_UNKNOWN;
        };
    }

    private boolean isBusy() {
        return !queuedActions.isEmpty() || nextActionAtMs > System.currentTimeMillis();
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
        private final InputUtil.Key key;
        private final List<KeyBinding> bindings;
        private final int keyboardKeyCode;
        private final boolean releaseStaticState;
        private final boolean releaseKeyboardEvent;
        private final long releaseAtMs;

        private PendingKeyRelease(InputUtil.Key key, List<KeyBinding> bindings, int keyboardKeyCode,
                                  boolean releaseStaticState, boolean releaseKeyboardEvent, long releaseAtMs) {
            this.key = key;
            this.bindings = bindings;
            this.keyboardKeyCode = keyboardKeyCode;
            this.releaseStaticState = releaseStaticState;
            this.releaseKeyboardEvent = releaseKeyboardEvent;
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
