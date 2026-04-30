package com.revampes.Fault.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.revampes.Fault.modules.ModuleManager;

import static com.revampes.Fault.Revampes.mc;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @SuppressWarnings({"UnreachableCode", "ConstantValue"})
    @Redirect(method = "knockbackTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec3d;multiply(DDD)Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d hookSlowVelocity(Vec3d instance, double x, double y, double z) {
        if ((Object) this == mc.player && ModuleManager.sprint.isEnabled() && ModuleManager.sprint.keep.isToggled()) {
            x = z = (100 - ModuleManager.sprint.slow.getInput()) / 100;
        }

        return instance.multiply(x, y, z);
    }
}
