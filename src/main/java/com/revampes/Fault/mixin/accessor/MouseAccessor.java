package com.revampes.Fault.mixin.accessor;

import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Mouse.class)
public interface MouseAccessor {
    @Invoker("onMouseButton")
    void revampes$invokeOnMouseButton(long window, MouseInput input, int action);
}

