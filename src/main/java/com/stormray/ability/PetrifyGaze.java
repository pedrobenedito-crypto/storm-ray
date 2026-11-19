package com.stormray.ability;

import com.stormray.config.StormRayConfig;
import com.stormray.item.PhantomArmorItem;
import com.stormray.petrify.PetrificationManager;
import com.stormray.registry.ModItems;
import com.stormray.registry.ModParticles;
import com.stormray.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * The gaze granted by a full set of phantom armour: look at a creature, press the key, and it
 * begins turning to stone exactly as it would under the ray's own pulse. One target at a time,
 * on a cooldown shown on the helmet.
 */
public final class PetrifyGaze {
    private PetrifyGaze() {
    }

    /** Called when a player presses the ability key. Everything is checked here, on the server. */
    public static void cast(ServerPlayer player) {
        if (!StormRayConfig.GAZE_ENABLED.get()) {
            return;
        }
        if (!PhantomArmorItem.hasFullSet(player)) {
            player.displayClientMessage(Component.translatable("message.stormray.gaze.no_set")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (player.getCooldowns().isOnCooldown(ModItems.PHANTOM_HELMET.get())) {
            return; // the cooldown overlay on the helmet is feedback enough
        }
        LivingEntity target = lookedAt(player, StormRayConfig.GAZE_RANGE.get());
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.stormray.gaze.no_target")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (PetrificationManager.isPetrified(target) || !PetrificationManager.canBePetrifiedBy(target, player)) {
            player.displayClientMessage(Component.translatable("message.stormray.gaze.immune", target.getDisplayName())
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }

        PetrificationManager.petrify(target, player, StormRayConfig.GAZE_PETRIFY_TICKS.get());
        player.getCooldowns().addCooldown(ModItems.PHANTOM_HELMET.get(), StormRayConfig.GAZE_COOLDOWN.get());

        ServerLevel level = (ServerLevel) player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 to = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).subtract(eye);
        double distance = to.length();
        if (distance > 0.1D) {
            Vec3 step = to.scale(1.0D / distance);
            for (double d = 0.5D; d < distance; d += 0.5D) {
                level.sendParticles(ModParticles.PETRIFY_DUST.get(), eye.x + step.x * d, eye.y + step.y * d,
                        eye.z + step.z * d, 1, 0.02D, 0.02D, 0.02D, 0.0D);
            }
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.PETRIFY.get(),
                SoundSource.PLAYERS, 1.2F, 1.4F);
    }

    /** The living creature the player is looking at, within range and in plain sight. */
    @Nullable
    private static LivingEntity lookedAt(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        AABB box = player.getBoundingBox().expandTowards(player.getLookAngle().scale(range)).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, box,
                PetrifyGaze::targetable, range * range);
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    private static boolean targetable(Entity entity) {
        return entity instanceof LivingEntity living && living.isAlive() && living.isPickable() && !living.isSpectator();
    }
}
