package com.stormray.ritual;

import com.stormray.StormRayMod;
import com.stormray.config.StormRayConfig;
import com.stormray.entity.StormRayEntity;
import com.stormray.portal.SteveHeads;
import com.stormray.registry.ModEntities;
import com.stormray.registry.ModParticles;
import com.stormray.registry.ModSounds;
import com.stormray.storm.StormEnvironment;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Detects the redstone T crowned with a Steve head, like the vanilla iron golem.
 * The ritual itself runs one tick after the head is placed, so Forge's block placement
 * bookkeeping finishes before the structure is consumed.
 */
@Mod.EventBusSubscriber(modid = StormRayMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SummoningRitualHandler {
    private record PendingRitual(ResourceKey<Level> dimension, BlockPos headPos, @Nullable UUID summoner) {
    }

    private static final List<PendingRitual> PENDING = new ArrayList<>();

    private SummoningRitualHandler() {
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!event.getPlacedBlock().is(Blocks.PLAYER_HEAD) || !StormRayConfig.ENABLE_RITUAL.get()) {
            return;
        }
        BlockPos headPos = event.getPos().immutable();
        if (findOrientation(level, headPos) == null) {
            return;
        }
        for (PendingRitual pending : PENDING) {
            if (pending.headPos().equals(headPos) && pending.dimension() == level.dimension()) {
                return; // already queued: never summon twice from the same structure
            }
        }
        UUID summoner = event.getEntity() != null ? event.getEntity().getUUID() : null;
        PENDING.add(new PendingRitual(level.dimension(), headPos, summoner));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING.isEmpty()) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        List<PendingRitual> batch = new ArrayList<>(PENDING);
        PENDING.clear();
        if (server == null) {
            return;
        }
        for (PendingRitual pending : batch) {
            ServerLevel level = server.getLevel(pending.dimension());
            if (level == null) {
                continue;
            }
            Player summoner = pending.summoner() == null ? null : level.getPlayerByUUID(pending.summoner());
            performRitual(level, pending.headPos(), summoner);
        }
    }

    private static void performRitual(ServerLevel level, BlockPos headPos, @Nullable Player summoner) {
        // Re-validate: the structure may have changed during the tick.
        if (!level.getBlockState(headPos).is(Blocks.PLAYER_HEAD)) {
            return;
        }
        Boolean armsAlongX = findOrientation(level, headPos);
        if (armsAlongX == null) {
            return;
        }
        if (StormRayConfig.REQUIRE_STEVE_HEAD.get() && !isSteveHead(level, headPos)) {
            tell(summoner, Component.translatable("message.stormray.ritual.wrong_head").withStyle(ChatFormatting.GRAY));
            return;
        }
        int[] base = RitualShape.baseOffset();
        BlockPos basePos = headPos.offset(base[0], base[1], base[2]);
        Vec3 center = Vec3.atBottomCenterOf(basePos);
        if (!StormRayConfig.ALLOW_MULTIPLE.get()
                && StormRayEntity.anyAliveNear(level, center, StormRayConfig.MIN_BOSS_DISTANCE.get())) {
            tell(summoner, Component.translatable("message.stormray.ritual.already_active").withStyle(ChatFormatting.DARK_RED));
            return;
        }

        // Consume the structure (head first so it can never be re-triggered).
        consumeBlock(level, headPos);
        for (int[] o : RitualShape.redstoneOffsets(armsAlongX)) {
            consumeBlock(level, headPos.offset(o[0], o[1], o[2]));
        }

        StormRayEntity ray = ModEntities.STORM_RAY.get().create(level);
        if (ray == null) {
            return;
        }
        float yaw = summoner != null ? summoner.getYRot() + 180.0F : level.random.nextFloat() * 360.0F;
        ray.moveTo(center.x, center.y, center.z, yaw, 0.0F);
        ray.finalizeSpawn(level, level.getCurrentDifficultyAt(basePos), MobSpawnType.EVENT, null, null);
        level.addFreshEntity(ray);

        // Ritual effects
        level.playSound(null, center.x, center.y, center.z, ModSounds.RITUAL_IGNITE.get(), SoundSource.HOSTILE, 6.0F, 0.9F);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y + 1.0D, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ModParticles.RITUAL_RUNE.get(), center.x, center.y + 1.0D, center.z, 160, 1.5D, 2.0D, 1.5D, 0.35D);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y + 0.5D, center.z, 60, 2.0D, 0.5D, 2.0D, 0.05D);
        StormEnvironment.spawnVisualLightning(level, center);
        tell(summoner, Component.translatable("message.stormray.ritual.success").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        StormRayMod.LOGGER.info("Storm Ray summoned at {} in {}", basePos, level.dimension().location());
    }

    private static void consumeBlock(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        level.levelEvent(2001, pos, Block.getId(state)); // block-break particles + sound
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    /** @return TRUE for a bar along X, FALSE for a bar along Z, null if there is no valid T under the head. */
    @Nullable
    private static Boolean findOrientation(Level level, BlockPos headPos) {
        if (matches(level, headPos, true)) {
            return Boolean.TRUE;
        }
        if (matches(level, headPos, false)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private static boolean matches(Level level, BlockPos headPos, boolean armsAlongX) {
        for (int[] o : RitualShape.redstoneOffsets(armsAlongX)) {
            if (!level.getBlockState(headPos.offset(o[0], o[1], o[2])).is(Blocks.REDSTONE_BLOCK)) {
                return false;
            }
        }
        return true;
    }

    /** A head without an owner renders as Steve; heads explicitly owned by "Steve"/"MHF_Steve" also count. */
    private static boolean isSteveHead(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof SkullBlockEntity skull && SteveHeads.isSteve(skull.getOwnerProfile());
    }

    private static void tell(@Nullable Player player, Component message) {
        if (player != null) {
            player.displayClientMessage(message, true);
        }
    }
}
