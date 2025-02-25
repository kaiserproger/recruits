package com.talhanation.recruits.mixin;

import com.talhanation.recruits.DamageEvent;
import dev.architectury.event.EventResult;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @SuppressWarnings("DataFlowIssue")
    @Inject(at = @At("Lnet/minecraft/world/entity/LivingEntity;knockback(D;D;D)V"))
    public void knockback(CallbackInfoReturnable<Void> cir) {
        EventResult result = DamageEvent.onKnockback((LivingEntity)(Object)this);

        if(result.interruptsFurtherEvaluation()) {
            cir.cancel();
        }
    }
}
