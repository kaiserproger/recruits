package com.talhanation.recruits.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
    public void teleportTo(CallbackInfoReturnable<Void> cir) {

    }
}
