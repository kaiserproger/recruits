package com.talhanation.recruits;

import com.talhanation.recruits.config.RecruitsServerConfig;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;

public class DamageEvent {
    public void registerEvents() {
        EntityEvent.LIVING_HURT.register((LivingEntity entity, DamageSource source, float amount) -> {
            this.onEntityHurt(entity, source);
            return EventResult.interruptDefault();
        });

        PlayerEvent.ATTACK_ENTITY.register((Player player, Level level, Entity entity, InteractionHand hand, @Nullable EntityHitResult hitResult) -> {
            EventResult result = onPlayerAttack(player, level, entity);
            if (result.interruptsFurtherEvaluation()) {
                return result;
            }

            return onEntityHurtByPlayer(player, level, entity);
        });
    }

    public static EventResult onPlayerAttack(Player player, Level level, Entity target) {
        if (level.isClientSide()) {
            return EventResult.pass();
        }

        float str = player.getAttackStrengthScale(0);
        if (str <= 0.1) {
            return EventResult.interruptTrue();
        }

        if (str <= 0.75) {
            if (target instanceof LivingEntity) {
                ((LivingEntity) target).swinging = true;
            }
        }

        return EventResult.pass();
    }

    public static EventResult onKnockback(LivingEntity entity) {
        if (entity.swinging) {
            entity.swinging = false;
            return EventResult.interruptTrue();
        }

        return EventResult.pass();
    }

    public void onEntityHurt(LivingEntity entity, DamageSource source) {
        if (entity.getLevel().isClientSide()) {
            return;
        }

        if (!RecruitsServerConfig.NoDamageImmunity.get()) return;

        if (entity.getLevel().isClientSide()) {
            return;
        }

        //Velocity Damage
        if (source != null) {

        }

        if (source != null && RecruitsServerConfig.AcceptedDamagesourceImmunity.get().contains(source.getMsgId())) {
            return;
        }

        entity.invulnerableTime = 0;
    }

    public EventResult onEntityHurtByPlayer(Player player, Level level, Entity target) {
        if (target.getFirstPassenger() instanceof LivingEntity passenger) {
            if (!RecruitEvents.canHarmTeam(player, passenger)) {
                return EventResult.interruptTrue();
            }
        }

        return EventResult.pass();
    }
}