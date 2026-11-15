package com.stormray.portal;

import com.stormray.registry.ModParticles;
import com.stormray.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The open floor portal. Players who step into it travel to the same coordinates in the
 * other world. It collapses as soon as its redstone ring is broken.
 */
public class AshenPortalBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D);

    public AshenPortalBlock(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level instanceof ServerLevel serverLevel && entity instanceof ServerPlayer player
                && !player.isPassenger() && !player.isVehicle() && !player.isOnPortalCooldown() && player.canChangeDimensions()) {
            PortalTravel.travel(player, serverLevel, pos);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {
        if (direction.getAxis().isHorizontal() && !PortalLogic.isCellSupported(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            level.addParticle(ModParticles.STORM_EMBER.get(), pos.getX() + random.nextDouble(), pos.getY() + 0.8D,
                    pos.getZ() + random.nextDouble(), 0.0D, 0.04D + random.nextDouble() * 0.06D, 0.0D);
        }
        if (random.nextInt(4) == 0) {
            level.addParticle(ModParticles.SOOT.get(), pos.getX() + random.nextDouble(), pos.getY() + 0.8D,
                    pos.getZ() + random.nextDouble(), 0.0D, 0.03D, 0.0D);
        }
        if (random.nextInt(160) == 0) {
            level.playLocalSound(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, ModSounds.PORTAL_AMBIENT.get(),
                    SoundSource.BLOCKS, 0.6F, 0.8F + random.nextFloat() * 0.3F, false);
        }
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean canBeReplaced(BlockState state, Fluid fluid) {
        return false;
    }
}
