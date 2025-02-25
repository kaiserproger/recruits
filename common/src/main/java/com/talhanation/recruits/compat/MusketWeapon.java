package com.talhanation.recruits.compat;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MusketWeapon implements IWeapon {
    @Override
    @Nullable
    public Item getWeapon() {
        try {
            Class<?> itemClass = Class.forName("ewewukek.musketmod.Items");
            Object musketWeaponInstance = itemClass.newInstance();

            Field musketItemField = musketWeaponInstance.getClass().getField("MUSKET");
            Object item = musketItemField.get("MUSKET");
            return (Item) item;
        }
        catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException | InstantiationException e) {
            Main.LOGGER.error("Items of MusketMod was not found");
            return null;
        }
    }

    @Override
    public double getMoveSpeedAmp() {
        return 0.4D;
    }

    @Override
    public int getAttackCooldown() {
        return 30;//MusketItem.RELOAD_DURATION;
    }

    @Override
    public int getWeaponLoadTime() {
        return 35; //return MusketItem.LOADING_STAGE_1 + MusketItem.LOADING_STAGE_2 + MusketItem.LOADING_STAGE_3;
    }

    @Override
    public float getProjectileSpeed() {
        return 2.0F;
    }

    public boolean isLoaded(ItemStack stack) {
        try {
            Class<?> musketItemClass = Class.forName("ewewukek.musketmod.MusketItem");

            Method musketItemIsLoaded = musketItemClass.getMethod("isLoaded", ItemStack.class);
            return (boolean) musketItemIsLoaded.invoke(musketItemClass, stack);
        }
        catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            Main.LOGGER.info("MusketItem was not found");
            return false;
        }
    }

    @Override
    public void setLoaded(ItemStack stack, boolean loaded) {
        try {
            Class<?> musketItemClass = Class.forName("ewewukek.musketmod.MusketItem");

            Method musketItemSetLoaded = musketItemClass.getMethod("setLoaded", ItemStack.class, boolean.class);

            musketItemSetLoaded.invoke(musketItemClass, stack, loaded);
        }
        catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            Main.LOGGER.info("MusketItem was not found");
        }
    }

    @Override
    public AbstractHurtingProjectile getProjectile(LivingEntity shooter) {
        try {
            Class<?> bulletClass = Class.forName("ewewukek.musketmod.BulletEntity");
            Class<?>[] constructorParamTypes = {Level.class};
            Constructor<?> bulletConstructor = bulletClass.getConstructor(constructorParamTypes);
            Level level = shooter.level;
            Object bulletInstance = bulletConstructor.newInstance(level);

            if(bulletInstance instanceof AbstractHurtingProjectile bullet){
                bullet.setOwner(shooter);
                bullet.setPos(shooter.getX(), shooter.getY() + shooter.getEyeHeight() - 0.1D, shooter.getZ());

                return bullet;
            }
            else
                return null;
        }
        catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException | InstantiationException e) {
            Main.LOGGER.info("MusketItem was not found");
            return null;
        }
    }

    @Override
    public AbstractArrow getProjectileArrow(LivingEntity shooter) {
        return null;
    }

    @Override
    public AbstractHurtingProjectile shoot(LivingEntity shooter, AbstractHurtingProjectile projectile, double x, double y, double z) {
        double d3 = Mth.sqrt((float) (x * x + z * z));
        Vec3 vec3 = (new Vec3(x, y + d3 * (double) 0.065, z)).normalize().scale(10F);
        try {
            Class<?> bulletClass = Class.forName("ewewukek.musketmod.BulletEntity");
            if (bulletClass.isInstance(projectile)) {
                Object bullet = bulletClass.cast(projectile);

                Field bulletDamageField = bullet.getClass().getField("damageMultiplier");
                bulletDamageField.setAccessible(true);

                Method bulletClassSetInitialSpeedMethod = bullet.getClass().getMethod("setInitialSpeed", float.class);

                bulletClassSetInitialSpeedMethod.invoke(bullet, 5F);
                bulletDamageField.setFloat(bullet, 1.5F);



                projectile.setDeltaMovement(vec3);
                projectile.shoot(x, y + d3 * (double) 0.065, z, 4.5F, (float) (0));
            }

        } catch (NoSuchFieldException e) {
            Main.LOGGER.error("bulletDamageField was not found (NoSuchFieldException)");
        } catch (ClassNotFoundException e) {
            Main.LOGGER.error("BulletEntity.class was not found (ClassNotFoundException)");
        } catch (InvocationTargetException e) {
            Main.LOGGER.error("bulletClassSetInitialSpeedMethod was not found (InvocationTargetException)");
        } catch (NoSuchMethodException e) {
            Main.LOGGER.error("bulletClassSetDeltaMovementMethod was not found (NoSuchMethodException)");
        } catch (IllegalAccessException e) {
            Main.LOGGER.error("BulletEntity.class was not found (IllegalAccessException)");
        }

        Vec3 forward = new Vec3(x, y, z).normalize();
        Vec3 origin = new Vec3(shooter.getX(), shooter.getEyeY(), shooter.getZ());

        try{
            Class<?> musketModClass = Class.forName("ewewukek.musketmod.MusketMod");
            Method sendSmokeEffectMethod = musketModClass.getMethod("sendSmokeEffect", LivingEntity.class, Vec3.class, Vec3.class);
            sendSmokeEffectMethod.invoke(musketModClass, shooter, origin, forward);

        } catch (ClassNotFoundException e) {
            Main.LOGGER.error("MusketMod.class was not found (ClassNotFoundException)");

        } catch (InvocationTargetException e) {
            Main.LOGGER.error("sendSmokeEffectMethod was not found (InvocationTargetException)");

        } catch (NoSuchMethodException e) {
            Main.LOGGER.error("sendSmokeEffectMethod was not found (NoSuchMethodException)");

        } catch (IllegalAccessException e) {
            Main.LOGGER.error("MusketMod.class was not found (IllegalAccessException)");

        }
        return projectile;
    }

    @Override
    public AbstractArrow shootArrow(LivingEntity shooter, AbstractArrow projectile, double x, double y, double z) {
        return null;
    }

    @Override
    public SoundEvent getShootSound() {
        try {
            Class<?> itemClass = Class.forName("ewewukek.musketmod.Sounds");
            Object musketWeaponInstance = itemClass.newInstance();

            Field musketItemField = musketWeaponInstance.getClass().getField("MUSKET_FIRE");
            Object soundEvent = musketItemField.get("MUSKET_FIRE");
            return (SoundEvent) soundEvent;
        }
        catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException | InstantiationException e) {
            Main.LOGGER.error("Sounds of MusketMod was not found");
            return null;
        }
    }

    @Override
    public SoundEvent getLoadSound() {
        try {
            Class<?> itemClass = Class.forName("ewewukek.musketmod.Sounds");
            Object musketWeaponInstance = itemClass.newInstance();

            Field musketItemField = musketWeaponInstance.getClass().getField("MUSKET_READY");
            Object soundEvent = musketItemField.get("MUSKET_READY");
            return (SoundEvent) soundEvent;
        }
        catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException | InstantiationException e) {
            Main.LOGGER.error("Sounds of MusketMod was not found");
            return null;
        }
    }

    @Override
    public boolean isGun() {
        return true;
    }

    @Override
    public boolean canMelee() {
        return false;
    }

    @Override
    public boolean isBow(){
        return false;
    }

    @Override
    public boolean isCrossBow() {
        return false;
    }

    @Override
    public void performRangedAttackIWeapon(AbstractRecruitEntity shooter, double x, double y, double z, float projectileSpeed) {
        AbstractHurtingProjectile projectileEntity = this.getProjectile(shooter);
        double d0 = x - shooter.getX();
        double d1 = y + 0.5D - projectileEntity.getY();
        double d2 = z - shooter.getZ();


        this.shoot(shooter, projectileEntity, d0, d1, d2);

        shooter.playSound(this.getShootSound(), 1.0F, 1.0F / (shooter.getRandom().nextFloat() * 0.4F + 0.8F));
        shooter.level.addFreshEntity(projectileEntity);

        shooter.damageMainHandItem();
    }

}
