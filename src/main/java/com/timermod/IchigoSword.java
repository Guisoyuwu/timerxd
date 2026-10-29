package com.timermod;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;

public class IchigoSword extends SwordItem {

    /** Daño total del golpe = 1 (mano) + 29 = 30 */
    public IchigoSword(Settings settings) {
        super(IchigoMaterial.INSTANCE, 29, -2.4f, settings);
    }

    /** Click derecho: Getsuga Tenshō (ola de energía de 30 bloques, 30 de daño). */
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world instanceof ServerWorld sw) {
            Vec3d eye = user.getEyePos();
            Vec3d dir = user.getRotationVec(1.0f).normalize();
            DustParticleEffect red = new DustParticleEffect(new Vector3f(0.9f, 0.05f, 0.05f), 2.0f);
            Set<Integer> hit = new HashSet<>();

            for (double d = 1.5; d <= 30; d += 0.75) {
                Vec3d p = eye.add(dir.multiply(d));
                BlockPos bp = BlockPos.ofFloored(p);
                if (!sw.getBlockState(bp).getCollisionShape(sw, bp).isEmpty()) break;

                for (double h = -1.5; h <= 1.5; h += 0.75) {
                    sw.spawnParticles(ParticleTypes.SQUID_INK, p.x, p.y + h, p.z, 1, 0.05, 0.05, 0.05, 0.0);
                    sw.spawnParticles(red, p.x, p.y + h, p.z, 1, 0.1, 0.1, 0.1, 0.0);
                }

                Box box = new Box(p.x - 1.2, p.y - 1.8, p.z - 1.2, p.x + 1.2, p.y + 1.8, p.z + 1.2);
                for (LivingEntity e : sw.getEntitiesByClass(LivingEntity.class, box, en -> en != user && en.isAlive())) {
                    if (hit.add(e.getId())) {
                        e.damage(sw.getDamageSources().playerAttack(user), 30.0f);
                    }
                }
            }
            sw.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 1.0f, 0.8f);
            user.getItemCooldownManager().set(this, 60); // 3 segundos
        }
        user.swingHand(hand, true);
        return TypedActionResult.success(stack, world.isClient());
    }

    public enum IchigoMaterial implements ToolMaterial {
        INSTANCE;
        @Override public int getDurability() { return 0; } // 0 = no se rompe
        @Override public float getMiningSpeedMultiplier() { return 8.0f; }
        @Override public float getAttackDamage() { return 0.0f; }
        @Override public int getMiningLevel() { return 4; }
        @Override public int getEnchantability() { return 20; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.EMPTY; }
    }
}
