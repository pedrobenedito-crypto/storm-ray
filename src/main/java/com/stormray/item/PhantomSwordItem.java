package com.stormray.item;

import com.stormray.config.StormRayConfig;
import com.stormray.network.DashPacket;
import com.stormray.network.ModNetwork;
import com.stormray.registry.ModParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The Phantom Sword: a blade of bound scales that burns what it cuts, and carries the ray's
 * own vanishing trick. Right-click to dash forward: armour, blade and all vanish for as long
 * as the dash lasts.
 */
public class PhantomSwordItem extends SwordItem {
    public PhantomSwordItem(Tier tier, int attackDamage, float attackSpeed, Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
    }

    /** Innate burn: the blade sets its target alight without an enchantment to strip off. */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        target.setSecondsOnFire(StormRayConfig.SWORD_FIRE_SECONDS.get());
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        double power = StormRayConfig.SWORD_DASH_POWER.get();
        int invisible = StormRayConfig.SWORD_DASH_TICKS.get();

        // The player is authoritative over their own movement, so the push is applied on both
        // sides: on the client it moves them, on the server it keeps everyone else in step.
        Vec3 look = player.getLookAngle();
        Vec3 dash = new Vec3(look.x, Math.max(look.y, -0.2D) * 0.6D + 0.15D, look.z).normalize().scale(power);
        player.setDeltaMovement(dash);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;

        if (level instanceof ServerLevel serverLevel) {
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, invisible, 0, false, false, true));
            // tells every client watching to drop this player's armour and blade from the render
            ModNetwork.sendTracking(player, new DashPacket(player.getId(), invisible));
            player.getCooldowns().addCooldown(this, StormRayConfig.SWORD_DASH_COOLDOWN.get());
            stack.hurtAndBreak(1, player, user -> user.broadcastBreakEvent(hand));
            serverLevel.sendParticles(ModParticles.SOOT.get(), player.getX(), player.getY() + 1.0D, player.getZ(),
                    40, 0.3D, 0.6D, 0.3D, 0.05D);
            serverLevel.sendParticles(ModParticles.STORM_EMBER.get(), player.getX(), player.getY() + 1.0D, player.getZ(),
                    16, 0.3D, 0.6D, 0.3D, 0.03D);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_DRAGON_FLAP,
                    SoundSource.PLAYERS, 0.7F, 1.6F);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.success(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.stormray.phantom_sword.tooltip.fire").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.stormray.phantom_sword.tooltip.dash").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
