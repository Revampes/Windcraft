package com.revampes.Fault.mixin;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.revampes.Fault.Revampes;
import com.revampes.Fault.events.impl.BreakBlockEvent;
import com.revampes.Fault.events.impl.StartBreakingBlockEvent;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {

    @Inject(method = "attackBlock", at = @At("HEAD"), cancellable = true)
    private void onAttackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (Revampes.EVENT_BUS.post(StartBreakingBlockEvent.get(pos, direction)).isCancelled()) {
            cir.cancel();
        }
    }

    @Inject(method = "breakBlock", at = @At("HEAD"), cancellable = true)
    private void onBreakBlock(BlockPos blockPos, CallbackInfoReturnable<Boolean> cir) {
        if (Revampes.EVENT_BUS.post(BreakBlockEvent.get(blockPos)).isCancelled()) {
            cir.setReturnValue(false);
        }
    }
}
