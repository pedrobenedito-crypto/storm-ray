package com.stormray.item;

import com.stormray.dimension.ModDimensions;
import com.stormray.registry.ModParticles;
import com.stormray.registry.ModStructures;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Found in Sky Island chests. Using it sends a streak of embers towards the Ruined Colosseum
 * and tells the holder how far away it is. It is not used up. The colosseum stands at the
 * same coordinates in the overworld and in the Ashen Reflection, so it works in both.
 */
public class ColosseumLocatorItem extends Item {
    private static final String TAG_X = "ColosseumX";
    private static final String TAG_Z = "ColosseumZ";
    private static final String[] DIRECTIONS = {"n", "ne", "e", "se", "s", "sw", "w", "nw"};
    private static final double ARRIVED_DISTANCE = 48.0D;

    public ColosseumLocatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.success(stack);
        }
        player.getCooldowns().addCooldown(this, 40);
        BlockPos target = locate(serverLevel, player.blockPosition(), stack);
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.stormray.locator.nothing").withStyle(ChatFormatting.GRAY), true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5F, 1.4F);
            return InteractionResultHolder.consume(stack);
        }
        double dx = target.getX() + 0.5D - player.getX();
        double dz = target.getZ() + 0.5D - player.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < ARRIVED_DISTANCE) {
            player.displayClientMessage(Component.translatable("message.stormray.locator.here").withStyle(ChatFormatting.DARK_RED), true);
        } else {
            Component direction = Component.translatable("direction.stormray." + DIRECTIONS[compassIndex(dx, dz)]);
            player.displayClientMessage(Component.translatable("message.stormray.locator.found", (int) Math.round(distance), direction)
                    .withStyle(ChatFormatting.RED), true);
            // a streak of embers pointing the way
            Vec3 eye = player.getEyePosition();
            for (int step = 2; step <= 16; step++) {
                double reach = step * 0.9D;
                serverLevel.sendParticles(ModParticles.STORM_EMBER.get(), eye.x + dx / distance * reach, eye.y - 0.2D + step * 0.03D,
                        eye.z + dz / distance * reach, 2, 0.04D, 0.04D, 0.04D, 0.0D);
            }
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_EYE_LAUNCH, SoundSource.PLAYERS, 0.8F, 0.6F);
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.consume(stack);
    }

    /** 0 = north, 1 = north-east, ... clockwise. North is -Z, east is +X. */
    public static int compassIndex(double dx, double dz) {
        double angle = Math.atan2(dx, -dz);
        return Math.floorMod((int) Math.round(angle / (Math.PI / 4.0D)), 8);
    }

    /** Centre of the colosseum (y = 0), remembered on the item after the first lookup. */
    @Nullable
    private static BlockPos locate(ServerLevel level, BlockPos origin, ItemStack stack) {
        if (level.dimension() != Level.OVERWORLD && !ModDimensions.isMirror(level)) {
            return null;
        }
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_X) && tag.contains(TAG_Z)) {
            return new BlockPos(tag.getInt(TAG_X), 0, tag.getInt(TAG_Z));
        }
        BlockPos found = level.findNearestMapStructure(ModStructures.COLOSSEUM_TAG, origin, 100, false);
        if (found == null) {
            return null;
        }
        // locate results are the corner of the structure's chunk; the arena centre is its middle
        BlockPos centre = new BlockPos(found.getX() + 8, 0, found.getZ() + 8);
        CompoundTag data = stack.getOrCreateTag();
        data.putInt(TAG_X, centre.getX());
        data.putInt(TAG_Z, centre.getZ());
        return centre;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.stormray.colosseum_locator.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
